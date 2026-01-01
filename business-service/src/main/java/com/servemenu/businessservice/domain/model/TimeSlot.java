package com.servemenu.businessservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlot implements Serializable {
    private LocalTime openTime;
    private LocalTime closeTime;
}
