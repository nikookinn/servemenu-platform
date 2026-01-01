package com.servemenu.businessservice.application.mapper;

import com.servemenu.businessservice.application.dto.response.CustomDomainResponse;
import com.servemenu.businessservice.application.dto.response.DnsRecordResponse;
import com.servemenu.businessservice.domain.model.CustomDomain;
import com.servemenu.businessservice.domain.model.DnsRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CustomDomainMapper {

    @Mapping(target = "dnsRecords", source = "dnsRecords")
    CustomDomainResponse toResponse(CustomDomain customDomain);

    List<DnsRecordResponse> toDnsRecordResponseList(List<DnsRecord> dnsRecords);

    DnsRecordResponse toDnsRecordResponse(com.servemenu.businessservice.domain.model.DnsRecord dnsRecord);
}
