package com.servemenu.businessservice.application.dto.command;

public record UpdateSocialAccountsCommand(
        String facebook,
        String twitter,
        String instagram,
        String snapchat,
        String pinterest,
        String foursquare,
        String tripadvisor,
        String zomato,
        String tiktok
) {}
