package com.servemenu.businessservice.application.service;

import com.servemenu.businessservice.domain.event.TableQRRegenerationEvent;
import com.servemenu.businessservice.domain.model.QRCustomization;
import com.servemenu.businessservice.domain.model.Table;
import com.servemenu.businessservice.infrastructure.kafka.producer.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class QRRegenerationService {

    private final DomainEventPublisher eventPublisher;

    /**
     * Regenerate QR codes for multiple tables with new customization
     */
    @Transactional
    public void regenerateQRCodesForTables(
            List<Table> tables, 
            QRCustomization customization, 
            UUID performedBy
    ) {
        log.info("Starting QR regeneration for {} tables", tables.size());

        for (Table table : tables) {
            regenerateQRCodeForTable(table, customization, performedBy);
        }

        log.info("Completed QR regeneration for {} tables", tables.size());
    }

    /**
     * Regenerate QR code for a single table
     */
    @Transactional
    public void regenerateQRCodeForTable(
            Table table, 
            QRCustomization customization, 
            UUID performedBy
    ) {
        log.debug("Regenerating QR code for table: {} with customization", table.getId());

        // Publish event for QR Service to regenerate QR code
        eventPublisher.publish(new TableQRRegenerationEvent(
                table.getId(),
                table.getStore().getId(),
                table.getStore().getBusiness().getId(),
                table.getTableName(),
                table.getTableNumber(),
                customization.getId(),
                performedBy
        ));

        log.debug("Published QR regeneration event for table: {}", table.getId());
    }
}
