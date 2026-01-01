package com.servemenu.businessservice.application.mapper;

import com.servemenu.businessservice.application.dto.response.*;
import com.servemenu.businessservice.domain.model.*;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface BusinessMapper {

    BusinessResponse toResponse(Business business);

    @Mapping(target = "totalStores", expression = "java(countActiveStores(business))")
    @Mapping(target = "maxStoresAllowed", expression = "java(business.getSubscriptionPlan().getMaxStores())")
    BusinessDetailResponse toDetailResponse(Business business);

    default int countActiveStores(Business business) {
        return (int) business.getStores().stream()
                .filter(s -> s.getStatus() != com.servemenu.businessservice.domain.enums.StoreStatus.DELETED)
                .count();
    }
}
