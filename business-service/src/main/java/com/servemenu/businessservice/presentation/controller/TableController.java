package com.servemenu.businessservice.presentation.controller;

import com.servemenu.businessservice.application.dto.command.CreateTableCommand;
import com.servemenu.businessservice.application.dto.command.UpdateTableCommand;
import com.servemenu.businessservice.application.dto.request.BulkCreateTablesRequest;
import com.servemenu.businessservice.application.dto.request.CreateTableRequest;
import com.servemenu.businessservice.application.dto.request.UpdateTableRequest;
import com.servemenu.businessservice.application.dto.response.ApiResponse;
import com.servemenu.businessservice.application.dto.response.PageResponse;
import com.servemenu.businessservice.application.dto.response.TableResponse;
import com.servemenu.businessservice.application.service.BusinessSecurityService;
import com.servemenu.businessservice.application.service.QRUrlService;
import com.servemenu.businessservice.application.service.TableService;
import com.servemenu.businessservice.common.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/stores/{storeId}/tables")
@RequiredArgsConstructor
public class TableController {

    private final TableService tableService;
    private final BusinessSecurityService securityService;
    private final JwtUtil jwtUtil;

    /**
     * Create single table (ASYNC flow with Kafka events)
     * POST /api/v1/businesses/{businessId}/stores/{storeId}/tables
     * 
     * Flow:
     * 1. Create table in DB
     * 2. Publish TableCreatedEvent to Kafka
     * 3. QR Service generates QR image (async)
     * 4. Media Service uploads to S3 (async)
     * 5. Business Service updates table.qrCodeMediaId (async)
     * 
     * Response: Table object (qrCodeMediaId will be null initially)
     * Frontend should poll or use WebSocket to get updated table with QR media
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('business_owner', 'store_admin')")
    public ResponseEntity<ApiResponse<TableResponse>> createTable(
            @PathVariable UUID businessId,
            @PathVariable UUID storeId,
            @RequestBody @Valid CreateTableRequest request,
            Authentication authentication
    ) {
        UUID userId = jwtUtil.extractUserId(authentication);
        securityService.validateStoreBusinessRelationship(businessId, storeId);
        securityService.validateStoreAccess(userId, storeId, authentication);

        CreateTableCommand command = new CreateTableCommand(request.tableName());
        TableResponse response = tableService.createTable(storeId, command, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Table created. QR code generation in progress..."));
    }


    /**
     * Bulk create tables (ASYNC flow with Kafka events)
     * POST /api/v1/businesses/{businessId}/stores/{storeId}/tables/bulk
     * 
     * Flow:
     * 1. Create N tables in DB
     * 2. Publish N TableCreatedEvents to Kafka
     * 3. QR Service generates N QR images (parallel, async)
     * 4. Media Service uploads N images to S3 (parallel, async)
     * 5. Business Service updates N tables with qrCodeMediaId (async)
     * 
     * Response: Array of table objects (qrCodeMediaId will be null initially)
     * Frontend should poll or use WebSocket to get updated tables with QR media
     */
    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('business_owner', 'store_admin')")
    public ResponseEntity<ApiResponse<List<TableResponse>>> bulkCreateTables(
            @PathVariable UUID businessId,
            @PathVariable UUID storeId,
            @RequestBody @Valid BulkCreateTablesRequest request,
            Authentication authentication
    ) {
        UUID userId = jwtUtil.extractUserId(authentication);
        securityService.validateStoreBusinessRelationship(businessId, storeId);
        securityService.validateStoreAccess(userId, storeId, authentication);

        List<TableResponse> response = tableService.bulkCreateTables(storeId, request.tableNames(), userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Tables created. QR code generation in progress..."));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('business_owner', 'store_admin', 'store_user')")
    public ResponseEntity<ApiResponse<PageResponse<TableResponse>>> getTables(
            @PathVariable UUID businessId,
            @PathVariable UUID storeId,
            @PageableDefault(size = 50) Pageable pageable,
            Authentication authentication
    ) {
        UUID userId = jwtUtil.extractUserId(authentication);
        securityService.validateStoreBusinessRelationship(businessId, storeId);
        securityService.validateStoreAccess(userId, storeId, authentication);

        PageResponse<TableResponse> response = tableService.getAllTables(storeId, pageable.getPageNumber(), pageable.getPageSize());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{tableId}")
    @PreAuthorize("hasAnyRole('business_owner', 'store_admin')")
    public ResponseEntity<ApiResponse<TableResponse>> updateTable(
            @PathVariable UUID businessId,
            @PathVariable UUID storeId,
            @PathVariable UUID tableId,
            @RequestBody @Valid UpdateTableRequest request,
            Authentication authentication
    ) {
        UUID userId = jwtUtil.extractUserId(authentication);
        securityService.validateStoreBusinessRelationship(businessId, storeId);
        securityService.validateStoreAccess(userId, storeId, authentication);

        UpdateTableCommand command = new UpdateTableCommand(
                request.tableName(),
                request.isActive()
        );

        TableResponse response = tableService.updateTable(tableId, command, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Table updated successfully"));
    }

    @DeleteMapping("/{tableId}")
    @PreAuthorize("hasAnyRole('business_owner', 'store_admin')")
    public ResponseEntity<ApiResponse<Void>> deleteTable(
            @PathVariable UUID businessId,
            @PathVariable UUID storeId,
            @PathVariable UUID tableId,
            Authentication authentication
    ) {
        UUID userId = jwtUtil.extractUserId(authentication);
        securityService.validateStoreBusinessRelationship(businessId, storeId);
        securityService.validateStoreAccess(userId, storeId, authentication);

        tableService.deleteTable(tableId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Table deleted successfully"));
    }

    /**
     * Bulk delete tables
     * DELETE /api/v1/businesses/{businessId}/stores/{storeId}/tables
     * Body: { "tableIds": ["uuid1", "uuid2", ...] }
     */
    @DeleteMapping
    @PreAuthorize("hasAnyRole('business_owner', 'store_admin')")
    public ResponseEntity<ApiResponse<Void>> deleteTables(
            @PathVariable UUID businessId,
            @PathVariable UUID storeId,
            @RequestBody Map<String, List<UUID>> request,
            Authentication authentication
    ) {
        UUID userId = jwtUtil.extractUserId(authentication);
        securityService.validateStoreBusinessRelationship(businessId, storeId);
        securityService.validateStoreAccess(userId, storeId, authentication);

        List<UUID> tableIds = request.get("tableIds");
        if (tableIds == null || tableIds.isEmpty()) {
            throw new IllegalArgumentException("tableIds cannot be empty");
        }

        tableService.deleteTables(tableIds, userId);
        return ResponseEntity.ok(ApiResponse.success(null, 
            String.format("%d table(s) deleted successfully", tableIds.size())));
    }

    /**
     * Get single table with QR URLs (for lazy loading QR preview)
     * GET /api/v1/businesses/{businessId}/stores/{storeId}/tables/{tableId}
     * 
     * This endpoint is used for lazy loading QR codes in the frontend.
     * When user clicks QR icon, frontend fetches fresh QR URLs.
     */
    @GetMapping("/{tableId}")
    @PreAuthorize("hasAnyRole('business_owner', 'store_admin', 'store_user')")
    public ResponseEntity<ApiResponse<TableResponse>> getTable(
            @PathVariable UUID businessId,
            @PathVariable UUID storeId,
            @PathVariable UUID tableId,
            Authentication authentication
    ) {
        UUID userId = jwtUtil.extractUserId(authentication);
        securityService.validateStoreBusinessRelationship(businessId, storeId);
        securityService.validateStoreAccess(userId, storeId, authentication);

        TableResponse response = tableService.getTableById(tableId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
