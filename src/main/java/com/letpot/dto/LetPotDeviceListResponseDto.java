package com.letpot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LetPotDeviceListResponseDto {
    private boolean ok;
    private List<LetPotDeviceDto> data;
} 