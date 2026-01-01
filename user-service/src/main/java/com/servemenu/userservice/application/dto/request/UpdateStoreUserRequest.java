package com.servemenu.userservice.application.dto.request;

import com.servemenu.userservice.domain.enums.StoreAccessLevel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating store user details
 * Note: All fields are optional - only provided fields will be updated
 */
public record UpdateStoreUserRequest(
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        @Email(message = "Invalid email format")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        StoreAccessLevel accessLevel,

        Boolean isActive
) {
}
