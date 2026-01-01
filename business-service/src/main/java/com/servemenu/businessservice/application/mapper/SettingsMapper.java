package com.servemenu.businessservice.application.mapper;

import com.servemenu.businessservice.application.dto.response.*;
import com.servemenu.businessservice.domain.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SettingsMapper {

    StoreSettingsResponse toStoreSettingsResponse(StoreSettings settings);

    SocialAccountsResponse toSocialAccountsResponse(SocialAccounts socialAccounts);

    LocationDetailsResponse toLocationDetailsResponse(LocationDetails locationDetails);

    BusinessSettingsResponse toBusinessSettingsResponse(BusinessSettings settings);

    NotificationSettingsResponse toNotificationSettingsResponse(NotificationSettings settings);

    OrderSettingsResponse toOrderSettingsResponse(OrderSettings settings);
}
