package com.servemenu.businessservice.application.mapper;

import com.servemenu.businessservice.application.dto.command.SaveQRCustomizationCommand;
import com.servemenu.businessservice.application.dto.request.QRCustomizationRequest;
import com.servemenu.businessservice.application.dto.response.QRCustomizationResponse;
import com.servemenu.businessservice.domain.model.QRCustomization;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface QRCustomizationMapper {

    @Mapping(target = "storeId", source = "store.id")
    @Mapping(target = "logoUrl", source = "logoUrl") // Map from transient field (set by MediaEnrichmentService)
    QRCustomizationResponse toResponse(QRCustomization qrCustomization);

    SaveQRCustomizationCommand toCommand(QRCustomizationRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "store", ignore = true)
    @Mapping(target = "qrType", ignore = true) // Set manually in service
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    QRCustomization toEntity(SaveQRCustomizationCommand command);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "store", ignore = true)
    @Mapping(target = "qrType", ignore = true) // QR type cannot be changed
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget QRCustomization qrCustomization, SaveQRCustomizationCommand command);
}
