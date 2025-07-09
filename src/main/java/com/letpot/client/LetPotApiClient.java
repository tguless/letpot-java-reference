package com.letpot.client;

import com.letpot.dto.LetPotAuthenticationResponseDto;
import com.letpot.dto.LetPotDeviceListResponseDto;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.Map;

public interface LetPotApiClient {
    
    @POST("v1/auth/login")
    Call<LetPotAuthenticationResponseDto> login(@Body Map<String, String> loginForm);
    
    @POST("v1/auth/refresh")
    Call<LetPotAuthenticationResponseDto> refreshToken(@Query("refresh_token") String refreshToken);
    
    @GET("v1/user/{userId}/devices")
    Call<LetPotDeviceListResponseDto> getDevices(@Header("Authorization") String token, @Path("userId") String userId);
} 