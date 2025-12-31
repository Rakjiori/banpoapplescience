package com.example.sbb.controller;

import com.example.sbb.dto.ScheduleSlotDto;
import com.example.sbb.service.ScheduleApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleApiController {

    private final ScheduleApiService scheduleApiService;

    @GetMapping
    public List<ScheduleSlotDto> list() {
        return scheduleApiService.findAllSlots();
    }
}
