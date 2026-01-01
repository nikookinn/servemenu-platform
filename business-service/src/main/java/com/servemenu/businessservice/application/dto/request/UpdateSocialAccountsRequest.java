package com.servemenu.businessservice.application.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateSocialAccountsRequest(
        @Size(max = 500, message = "URL must not exceed 500 characters")
        String facebook,

        @Size(max = 500, message = "URL must not exceed 500 characters")
        String twitter,

        @Size(max = 500, message = "URL must not exceed 500 characters")
        String instagram,

        @Size(max = 500, message = "URL must not exceed 500 characters")
        String snapchat,

        @Size(max = 500, message = "URL must not exceed 500 characters")
        String pinterest,

        @Size(max = 500, message = "URL must not exceed 500 characters")
        String foursquare,

        @Size(max = 500, message = "URL must not exceed 500 characters")
        String tripadvisor,

        @Size(max = 500, message = "URL must not exceed 500 characters")
        String zomato,

        @Size(max = 500, message = "URL must not exceed 500 characters")
        String tiktok
) {}
