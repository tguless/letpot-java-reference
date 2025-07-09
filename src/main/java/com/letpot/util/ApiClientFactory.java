package com.letpot.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.letpot.client.LetPotApiClient;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class ApiClientFactory {
    private static final String BASE_URL = "https://api.letpot.net/";
    
    public static LetPotApiClient createLetPotApiClient() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                .build();
        
        return retrofit.create(LetPotApiClient.class);
    }
} 