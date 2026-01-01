package com.example.sbb.dto;

public record GroupNoticeDto(Long id, String title, String content, String createdAt, String author, boolean canManage) {}
