package com.letpot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LetPotDeviceDto {
    private String deviceId;
    private String deviceName;
    private String deviceType;
    private String deviceStatus;
    private String deviceModel;
    private String firmwareVersion;
} 