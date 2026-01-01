package com.servemenu.businessservice.application.mapper;

import com.servemenu.businessservice.application.dto.response.OpeningHoursResponse;
import com.servemenu.businessservice.domain.model.OpeningHours;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OpeningHoursMapper {

    OpeningHoursResponse toResponse(OpeningHours openingHours);

    List<OpeningHoursResponse> toResponseList(List<OpeningHours> openingHours);
}
