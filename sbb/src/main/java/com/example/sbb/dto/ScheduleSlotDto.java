package com.example.sbb.dto;

public record ScheduleSlotDto(
        Long id,
        String dayOfWeek,
        String startTime,
        String endTime,
        String subject,
        String courseType,
        String school,
        String note
) {}
