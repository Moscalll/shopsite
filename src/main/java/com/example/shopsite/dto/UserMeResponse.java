package com.example.shopsite.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserMeResponse(
        @JsonProperty("id") Long id,
        @JsonProperty("username") String username,
        @JsonProperty("role") String role
) {}
