package com.servemenu.businessservice.application.mapper;

import com.servemenu.businessservice.application.dto.response.TaxResponse;
import com.servemenu.businessservice.domain.model.Tax;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TaxMapper {

    TaxResponse toResponse(Tax tax);

    List<TaxResponse> toResponseList(List<Tax> taxes);
}
