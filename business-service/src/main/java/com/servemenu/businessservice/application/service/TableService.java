package com.servemenu.businessservice.application.service;

import com.servemenu.businessservice.application.dto.command.*;
import com.servemenu.businessservice.application.dto.response.*;
import com.servemenu.businessservice.application.mapper.*;
import com.servemenu.businessservice.common.exception.*;
import com.servemenu.businessservice.domain.enums.SubscriptionPlan;
import com.servemenu.businessservice.domain.event.TableCreatedEvent;
import com.servemenu.businessservice.domain.event.TableUpdatedEvent;
import com.servemenu.businessservice.domain.event.TableDeletedEvent;
import com.servemenu.businessservice.domain.model.*;
import com.servemenu.businessservice.domain.repository.*;
import com.servemenu.businessservice.infrastructure.kafka.producer.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
@CacheConfig(cacheNames = "table")
public class TableService {

    private final TableRepository tableRepository;
    private final StoreRepository storeRepository;
    private final BusinessAuditLogRepository auditLogRepository;
    private final DomainEventPublisher eventPublisher;
    private final TableMapper tableMapper;
    private final BusinessRepository businessRepository;
    private final MediaEnrichmentService mediaEnrichmentService;
    private final QRCustomizationService qrCustomizationService;
    private final QRUrlService qrUrlService;
    private final QRRequestService qrRequestService;


    /**
     * Create table (ASYNC flow with Kafka events)
     * QR generation happens asynchronously via Kafka → QR Service → Media Service
     */
    @Transactional
    public TableResponse createTable(
            UUID storeId,
            CreateTableCommand command,
            UUID performedBy
    ) {
        log.info("Creating table (async flow): storeId={}, tableName={}", storeId, command.tableName());

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found"));

        Business business = businessRepository.findById(store.getBusiness().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        // Validate subscription limits
        validateTableCreationLimit(storeId, 1, business.getSubscriptionPlan());

        // Check duplicate table name
        if (tableRepository.existsByStoreIdAndTableName(storeId, command.tableName())) {
            throw new DuplicateResourceException("Table with this name already exists");
        }

        // Generate table number
        String tableNumber = generateTableNumber(storeId);

        // Create table entity
        Table table = Table.builder()
                .store(store)
                .tableName(command.tableName())
                .tableNumber(tableNumber)
                .isActive(true)
                .build();

        // Save table first to get the ID
        Table saved = tableRepository.save(table);

        // Generate QR URL (NO extra query - using saved object)
        String qrUrl = qrUrlService.generateQRUrl(saved);

        // Audit log
        auditLogRepository.save(
                BusinessAuditLog.tableCreated(
                        business.getId(),
                        storeId,
                        saved.getId(),
                        performedBy,
                        saved.getTableName()
                )
        );

        // Publish TableCreatedEvent (for event sourcing & future consumers)
        TableCreatedEvent tableCreatedEvent = new TableCreatedEvent(
                saved.getId(),
                storeId,
                business.getId(),
                saved.getTableName(),
                saved.getTableNumber(),
                qrUrl
        );
        eventPublisher.publishTableCreatedEvent(tableCreatedEvent);
        log.info("Published TableCreatedEvent for table: {}", saved.getId());
        
        // Request QR generation via QRRequestService (centralized QR logic)
        qrRequestService.publishTableQRRequest(saved, qrUrl);

        log.info("Table created (async QR generation): tableId={}, qrUrl={}", saved.getId(), qrUrl);

        return tableMapper.toResponse(saved);
    }

    /**
     * Bulk create tables (ASYNC flow with Kafka events)
     * QR generation happens asynchronously via Kafka → QR Service → Media Service
     */
    @Transactional
    public List<TableResponse> bulkCreateTables(
            UUID storeId,
            List<String> tableNames,
            UUID performedBy
    ) {
        log.info("Bulk creating tables (async flow): storeId={}, count={}", storeId, tableNames.size());

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found"));

        Business business = businessRepository.findById(store.getBusiness().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        // Validate subscription limits
        validateTableCreationLimit(storeId, tableNames.size(), business.getSubscriptionPlan());

        // Check for duplicate names
        for (String tableName : tableNames) {
            if (tableRepository.existsByStoreIdAndTableName(storeId, tableName)) {
                throw new DuplicateResourceException("Table with name '" + tableName + "' already exists");
            }
        }

        List<TableResponse> createdTables = new ArrayList<>();

        for (String tableName : tableNames) {
            String tableNumber = generateTableNumber(storeId);

            Table table = Table.builder()
                    .store(store)
                    .tableName(tableName)
                    .tableNumber(tableNumber)
                    .isActive(true)
                    .build();

            Table saved = tableRepository.save(table);

            // Generate QR URL (NO extra query - using saved object)
            String qrUrl = qrUrlService.generateQRUrl(saved);

            // Audit log
            auditLogRepository.save(
                    BusinessAuditLog.tableCreated(
                            business.getId(),
                            storeId,
                            saved.getId(),
                            performedBy,
                            saved.getTableName()
                    )
            );

            // Publish TableCreatedEvent (for event sourcing & future consumers)
            TableCreatedEvent tableCreatedEvent = new TableCreatedEvent(
                    saved.getId(),
                    storeId,
                    business.getId(),
                    saved.getTableName(),
                    saved.getTableNumber(),
                    qrUrl
            );
            eventPublisher.publishTableCreatedEvent(tableCreatedEvent);
            log.info("Published TableCreatedEvent for table: {}", saved.getId());
            
            // Request QR generation via QRRequestService (centralized QR logic)
            qrRequestService.publishTableQRRequest(saved, qrUrl);

            createdTables.add(tableMapper.toResponse(saved));

            log.info("Table created (async QR generation): tableId={}, tableName={}, qrUrl={}", 
                    saved.getId(), saved.getTableName(), qrUrl);
        }

        log.info("Bulk table creation completed: {} tables created (async QR generation)", createdTables.size());

        return createdTables;
    }

    /**
     * Get all tables for a store with batch QR URL enrichment
     */
    // @Cacheable(key = "'store:' + #storeId")
    public PageResponse<TableResponse> getAllTables(UUID storeId, int page, int size) {
        log.info("Fetching all tables for store: storeId={}", storeId);

        Pageable pageable = PageRequest.of(page, size);
        Page<Table> tablePage = tableRepository.findByStoreIdAndDeletedAtIsNull(storeId, pageable);
        
        // Use batch enrichment via TableMapper (single batch call to Media Service)
        List<TableResponse> enrichedResponses = tableMapper.toResponseList(tablePage.getContent());
        
        log.debug("Fetched {} tables with QR URLs for store: {}", enrichedResponses.size(), storeId);
        
        // Convert List back to PageResponse with all pagination metadata
        return PageResponse.<TableResponse>builder()
                .content(enrichedResponses)
                .pageNumber(tablePage.getNumber())
                .pageSize(tablePage.getSize())
                .totalElements(tablePage.getTotalElements())
                .totalPages(tablePage.getTotalPages())
                .first(tablePage.isFirst())
                .last(tablePage.isLast())
                .empty(tablePage.isEmpty())
                .build();
    }

    /**
     * Get single table by ID with QR URL enrichment (for lazy loading)
     * Used when user clicks QR icon to preview QR code
     */
    public TableResponse getTableById(UUID tableId) {
        log.debug("Fetching table with QR URLs: tableId={}", tableId);

        Table table = tableRepository.findByIdAndDeletedAtIsNull(tableId)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found or already deleted"));
        
        // Enrich with QR URLs from Media Service
        TableResponse response = tableMapper.toResponse(table);
        
        log.debug("Fetched table with QR URLs: tableId={}, qrMediaId={}", tableId, table.getQrCodeMediaId());
        
        return response;
    }

    /**
     * Update table
     */
    @Transactional
    // @CacheEvict(allEntries = true)
    public TableResponse updateTable(
            UUID tableId,
            UpdateTableCommand command,
            UUID performedBy
    ) {
        log.info("Updating table: tableId={}", tableId);

        Table table = tableRepository.findByIdAndDeletedAtIsNull(tableId)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found or already deleted"));

        if (command.tableName() != null) {
            table.updateName(command.tableName());
        }
        if (command.isActive() != null) {
            if (command.isActive()) {
                table.activate();
            } else {
                table.deactivate();
            }
        }

        Table updated = tableRepository.save(table);

        // Publish event
        eventPublisher.publish(new TableUpdatedEvent(
                updated.getId(),
                updated.getStore().getId(),
                updated.getTableName()
        ));

        // Enrich with QR code URL
        mediaEnrichmentService.enrichTable(updated);

        return tableMapper.toResponse(updated);
    }

    /**
     * Delete table (SOFT DELETE - marks as deleted, preserves data)
     * Publishes TableDeletedEvent for QR cleanup
     */
    @Transactional
    // @CacheEvict(allEntries = true)
    public void deleteTable(UUID tableId, UUID performedBy) {
        log.info("Soft deleting table: tableId={}, performedBy={}", tableId, performedBy);

        Table table = tableRepository.findByIdAndDeletedAtIsNull(tableId)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found or already deleted"));

        UUID qrCodeMediaId = table.getQrCodeMediaId();
        UUID storeId = table.getStore().getId();

        // Soft delete (sets deletedAt, deletedBy, isActive=false)
        table.softDelete(performedBy);
        tableRepository.save(table);

        // Publish event (Media Service will clean up QR code)
        eventPublisher.publish(new TableDeletedEvent(
                tableId,
                storeId,
                qrCodeMediaId
        ));

        log.info("Table soft deleted: tableId={}, qrCodeMediaId={}", tableId, qrCodeMediaId);
    }

    /**
     * Bulk soft delete tables
     * Publishes TableDeletedEvent for each table (Media Service cleanup)
     */
    @Transactional
    // @CacheEvict(allEntries = true)
    public void deleteTables(List<UUID> tableIds, UUID performedBy) {
        log.info("Bulk soft deleting {} tables, performedBy={}", tableIds.size(), performedBy);

        List<Table> tables = tableRepository.findAllById(tableIds);
        
        if (tables.isEmpty()) {
            throw new ResourceNotFoundException("No tables found with provided IDs");
        }

        int deletedCount = 0;
        for (Table table : tables) {
            if (table.getDeletedAt() != null) {
                log.warn("Table already deleted, skipping: tableId={}", table.getId());
                continue;
            }

            UUID qrCodeMediaId = table.getQrCodeMediaId();
            UUID storeId = table.getStore().getId();

            // Soft delete
            table.softDelete(performedBy);
            tableRepository.save(table);

            // Publish event for each table (Media Service cleanup)
            eventPublisher.publish(new TableDeletedEvent(
                    table.getId(),
                    storeId,
                    qrCodeMediaId
            ));

            deletedCount++;
            log.debug("Table soft deleted: tableId={}, qrCodeMediaId={}", table.getId(), qrCodeMediaId);
        }

        log.info("Bulk delete completed: {}/{} tables deleted", deletedCount, tableIds.size());
    }

    // Private helper methods
    private void validateTableCreationLimit(UUID storeId, int newTablesCount, SubscriptionPlan plan) {
        int maxTables = plan.getLimits().getMaxTablesPerStore();

        // -1 means unlimited
        if (maxTables == -1) {
            return;
        }

        long currentTableCount = tableRepository.countByStoreIdAndDeletedAtIsNull(storeId);
        long totalAfterCreation = currentTableCount + newTablesCount;

        if (totalAfterCreation > maxTables) {
            throw new SubscriptionLimitExceededException(
                    String.format(
                            "Cannot create %d table(s). Your %s plan allows maximum %d tables per store. " +
                            "Current: %d, Requested: %d. Please upgrade your plan.",
                            newTablesCount,
                            plan.getDisplayName(),
                            maxTables,
                            currentTableCount,
                            newTablesCount
                    )
            );
        }
    }

    /**
     * Update table with QR media ID after frontend QR generation
     */
    @Transactional
    public void updateTableWithQRMediaId(UUID tableId, UUID qrCodeMediaId) {
        log.info("Updating table with QR media ID: tableId={}, mediaId={}", tableId, qrCodeMediaId);

        Table table = tableRepository.findById(tableId)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found"));

        table.assignQRCode(qrCodeMediaId);
        tableRepository.save(table);

        log.info("Table updated with QR media ID: tableId={}", tableId);
    }

    private String generateTableNumber(UUID storeId) {
        Integer maxNumber = tableRepository.findMaxTableNumberByStoreId(storeId);
        int nextNumber = (maxNumber != null) ? maxNumber + 1 : 1;
        return String.format("T%03d", nextNumber);
    }
}
