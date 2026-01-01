package com.servemenu.businessservice.application.dto.response;

public record DnsRecordResponse(
        String type,
        String name,
        String value,
        String description
) {}
