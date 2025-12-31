package com.example.sbb.dto;

public record ReviewDto(Long id, String author, int rating, String content, String createdAt) {}
