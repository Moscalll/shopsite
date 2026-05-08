package com.example.shopsite.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ApiMessageResponse(@JsonProperty("message") String message) {}
