package com.example.sbb.dto;

public record PushSubscriptionRequest(
        String endpoint,
        String p256dh,
        String auth,
        String userAgent
) {}
