package com.letpot.service;

import com.letpot.client.LetPotApiClient;
import com.letpot.dto.LetPotAuthenticationResponseDto;
import com.letpot.dto.LetPotDeviceDto;
import com.letpot.dto.LetPotDeviceListResponseDto;
import com.letpot.exception.LetPotAuthenticationException;
import com.letpot.exception.LetPotConnectionException;
import com.letpot.model.LetPotCredentials;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LetPotService {

    private final LetPotApiClient letPotApiClient;
    private final LetPotMqttService letPotMqttService;
    private LetPotCredentials credentials;
    private static final Logger log = LoggerFactory.getLogger(LetPotService.class);

    public LetPotService(LetPotApiClient letPotApiClient) {
        this.letPotApiClient = letPotApiClient;
        this.letPotMqttService = new LetPotMqttService();
    }

    private String getUidFromToken(String token) {
        // The LetPot token is not signed with a verifiable key, so we parse it without a key.
        // This is acceptable as we are not verifying the token's integrity here,
        // only extracting the payload data from a token we just received from the trusted API.
        String unsignedToken = token.substring(0, token.lastIndexOf('.') + 1);
        Claims claims = Jwts.parserBuilder().build().parseClaimsJwt(unsignedToken).getBody();
        return claims.get("uid", String.class);
    }

    public LetPotCredentials login(String email, String password) {
        Map<String, String> loginForm = new HashMap<>();
        loginForm.put("loginType", "EMAIL");
        loginForm.put("email", email);
        loginForm.put("password", password);
        loginForm.put("refresh_token", "");

        try {
            Response<LetPotAuthenticationResponseDto> response = letPotApiClient.login(loginForm).execute();
            if (!response.isSuccessful() || response.body() == null || !response.body().isOk()) {
                throw new LetPotAuthenticationException("LetPot login failed: " + 
                    (response.errorBody() != null ? response.errorBody().string() : "Unknown error"));
            }
            
            LetPotAuthenticationResponseDto authResponse = response.body();
            LetPotAuthenticationResponseDto.DataBlock data = authResponse.getData();
            
            String accessToken = data.getToken().getToken();
            String letpotUserId = getUidFromToken(accessToken);

            credentials = new LetPotCredentials();
            credentials.setLetpotUserId(letpotUserId);
            credentials.setEmail(email.toLowerCase());
            credentials.setAccessToken(accessToken);
            credentials.setAccessTokenExpires(data.getToken().getExp());
            credentials.setRefreshToken(data.getRefreshToken().getToken());
            credentials.setRefreshTokenExpires(data.getRefreshToken().getExp());
            
            return credentials;
        } catch (IOException e) {
            throw new LetPotConnectionException("Failed to connect to LetPot API", e);
        }
    }

    public List<LetPotDeviceDto> getDevices() {
        validateCredentials();
        try {
            Response<LetPotDeviceListResponseDto> response = 
                letPotApiClient.getDevices("Bearer " + credentials.getAccessToken(), credentials.getLetpotUserId()).execute();
            
            if (!response.isSuccessful() || response.body() == null || !response.body().isOk()) {
                throw new LetPotConnectionException("Failed to get devices: " + 
                    (response.errorBody() != null ? response.errorBody().string() : "Unknown error"));
            }
            
            return response.body().getData();
        } catch (IOException e) {
            throw new LetPotConnectionException("Failed to connect to LetPot API", e);
        }
    }

    private byte[] buildPumpCommand(boolean turnOn, int durationSeconds) {
        // Based on the ISEConverter from the python-letpot library
        byte[] commandArray = new byte[15];
        commandArray[0] = 65; // Command prefix (0x41)
        commandArray[1] = 2;  // Command type (update status)
        commandArray[2] = (byte) (turnOn ? 1 : 0); // pump_mode (1 = on, 0 = off)
        commandArray[3] = 0;  // pump_cycle_on = False
        commandArray[4] = (byte) ((durationSeconds >> 8) & 0xFF);  // pump_duration high byte
        commandArray[5] = (byte) (durationSeconds & 0xFF);         // pump_duration low byte
        
        // The rest of the parameters are for cycle mode, which we are not using.
        // Fill the rest of the array with zeros.
        for (int i = 6; i < 15; i++) {
            commandArray[i] = 0;
        }
        
        return commandArray;
    }

    public void turnOnDevice(String deviceId, int durationSeconds) {
        validateCredentials();
        try {
            byte[] payload = buildPumpCommand(true, durationSeconds);
            letPotMqttService.publishCommand(deviceId, credentials.getEmail(), credentials.getLetpotUserId(), payload);
            log.info("Successfully sent command to turn ON device {} for {} seconds", deviceId, durationSeconds);
        } catch (Exception e) {
            log.error("Failed to turn on device {}: {}", deviceId, e.getMessage(), e);
            throw new LetPotConnectionException("Failed to turn on LetPot device", e);
        }
    }

    public void turnOffDevice(String deviceId) {
        validateCredentials();
        try {
            byte[] payload = buildPumpCommand(false, 0);
            letPotMqttService.publishCommand(deviceId, credentials.getEmail(), credentials.getLetpotUserId(), payload);
            log.info("Successfully sent command to turn OFF device {}", deviceId);
        } catch (Exception e) {
            log.error("Failed to turn off device {}: {}", deviceId, e.getMessage(), e);
            throw new LetPotConnectionException("Failed to turn off LetPot device", e);
        }
    }

    public void testDevice(String deviceId) {
        try {
            turnOnDevice(deviceId, 5); // Turn on for 5 seconds
            Thread.sleep(5000); // Wait for 5 seconds
            turnOffDevice(deviceId); // Turn off the device
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Device test was interrupted", e);
        } catch (Exception e) {
            log.error("Device test failed", e);
            throw new RuntimeException("Device test failed", e);
        }
    }

    private void validateCredentials() {
        if (credentials == null) {
            throw new LetPotAuthenticationException("Not logged in. Call login() first.");
        }

        if (Instant.now().getEpochSecond() >= credentials.getAccessTokenExpires()) {
            if (Instant.now().getEpochSecond() >= credentials.getRefreshTokenExpires()) {
                throw new LetPotAuthenticationException("LetPot refresh token has expired. Please log in again.");
            }
            
            try {
                Response<LetPotAuthenticationResponseDto> response = 
                    letPotApiClient.refreshToken(credentials.getRefreshToken()).execute();
                
                if (!response.isSuccessful() || response.body() == null || !response.body().isOk()) {
                    throw new LetPotAuthenticationException("Failed to refresh token: " + 
                        (response.errorBody() != null ? response.errorBody().string() : "Unknown error"));
                }
                
                LetPotAuthenticationResponseDto refreshResponse = response.body();
                LetPotAuthenticationResponseDto.DataBlock data = refreshResponse.getData();
                String newAccessToken = data.getToken().getToken();
                String letpotUserId = getUidFromToken(newAccessToken);
                
                credentials.setAccessToken(newAccessToken);
                credentials.setLetpotUserId(letpotUserId);
                credentials.setAccessTokenExpires(data.getToken().getExp());

                if (data.getRefreshToken() != null) {
                    credentials.setRefreshToken(data.getRefreshToken().getToken());
                    credentials.setRefreshTokenExpires(data.getRefreshToken().getExp());
                }
            } catch (IOException e) {
                throw new LetPotConnectionException("Failed to refresh token", e);
            }
        }
    }
} 