package com.servemenu.userservice.application.mapper;

import com.servemenu.userservice.application.dto.response.*;
import com.servemenu.userservice.domain.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {
    
    /**
     * Convert User to UserResponse (base user info only)
     */
    default UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getKeycloakUserId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmailVerified(),
                user.getUserType(),
                user.getAccountStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    @Mapping(source = "user", target = "user")
    BusinessOwnerResponse toBusinessOwnerResponse(BusinessOwnerProfile profile);

    @Mapping(source = "user", target = "user")
    @Mapping(source = "active", target = "isActive")
    StoreUserResponse toStoreUserResponse(StoreUserProfile profile);

    @Mapping(source = "user", target = "user")
    CustomerResponse toCustomerResponse(CustomerProfile profile);

    UserPreferencesResponse toUserPreferencesResponse(UserPreferences preferences);
}
