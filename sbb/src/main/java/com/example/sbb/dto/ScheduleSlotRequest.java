package com.example.sbb.dto;

public record ScheduleSlotRequest(
        Long scheduleId,
        String dayOfWeek,
        String startTime,
        String endTime,
        String note
) {}
