package com.servemenu.businessservice.application.mapper;

import com.servemenu.businessservice.application.dto.response.StoreDetailResponse;
import com.servemenu.businessservice.application.dto.response.StoreListResponse;
import com.servemenu.businessservice.application.dto.response.StoreResponse;
import com.servemenu.businessservice.domain.model.Store;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    uses = {WiFiSettingsMapper.class}
)
public interface StoreMapper {

    @Mapping(target = "businessId", source = "business.id")
    StoreResponse toResponse(Store store);

    @Mapping(target = "tableCount", expression = "java(store.getTables().size())")
    StoreListResponse toListResponse(Store store);

    @Mapping(target = "businessId", source = "business.id")
    @Mapping(target = "tableCount", expression = "java(store.getTables().size())")
    StoreDetailResponse toDetailResponse(Store store);
}
