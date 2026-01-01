package com.servemenu.businessservice.application.dto.response;

import com.servemenu.businessservice.domain.enums.DomainVerificationStatus;
import com.servemenu.businessservice.domain.enums.SslStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CustomDomainResponse(
        UUID id,
        String domain,
        DomainVerificationStatus verificationStatus,
        List<DnsRecordResponse> dnsRecords,
        SslStatus sslStatus,
        Instant verifiedAt,
        Instant createdAt,
        Instant updatedAt
) {}
