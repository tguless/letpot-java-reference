package com.letpot.model;

import lombok.Data;

@Data
public class LetPotCredentials {
    private String letpotUserId;
    private String email;
    private String accessToken;
    private long accessTokenExpires;
    private String refreshToken;
    private long refreshTokenExpires;
} 