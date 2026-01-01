package com.example.sbb.controller;

import com.example.sbb.dto.ScheduleSlotDto;
import com.example.sbb.dto.ScheduleSlotRequest;
import com.example.sbb.service.ScheduleApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PostMapping
    public ScheduleSlotDto create(@RequestBody ScheduleSlotRequest request) {
        return scheduleApiService.createSlot(request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        scheduleApiService.deleteSlot(id);
    }
}
