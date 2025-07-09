package com.letpot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LetPotAuthenticationResponseDto {
    private boolean ok;
    private DataBlock data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataBlock {
        private TokenInfo token;
        private TokenInfo refreshToken;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TokenInfo {
        private String token;
        private long exp;
    }
} 