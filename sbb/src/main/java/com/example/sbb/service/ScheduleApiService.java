package com.example.sbb.service;

import com.example.sbb.domain.ScheduleSlot;
import com.example.sbb.dto.ScheduleSlotDto;
import com.example.sbb.repository.ScheduleSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleApiService {

    private final ScheduleSlotRepository scheduleSlotRepository;

    @Transactional(readOnly = true)
    public List<ScheduleSlotDto> findAllSlots() {
        return scheduleSlotRepository.findAllByOrderByDayOfWeekAscStartTimeAsc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    private ScheduleSlotDto toDto(ScheduleSlot s) {
        String day = s.getDayOfWeek() != null ? s.getDayOfWeek().name() : null;
        String start = s.getStartTime() != null ? s.getStartTime().toString() : null;
        String end = s.getEndTime() != null ? s.getEndTime().toString() : null;
        var sch = s.getSchedule();
        String subject = sch != null ? sch.getSubject() : null;
        String courseType = sch != null ? sch.getCourseType() : null;
        String school = sch != null ? sch.getSchool() : null;
        return new ScheduleSlotDto(s.getId(), day, start, end, subject, courseType, school, s.getNote());
    }
}
