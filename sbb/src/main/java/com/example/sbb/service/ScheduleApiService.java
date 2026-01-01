package com.example.sbb.service;

import com.example.sbb.domain.ScheduleSlot;
import com.example.sbb.dto.ScheduleSlotDto;
import com.example.sbb.dto.ScheduleSlotRequest;
import com.example.sbb.repository.AcademyScheduleRepository;
import com.example.sbb.repository.ScheduleSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class ScheduleApiService {

    private final ScheduleSlotRepository scheduleSlotRepository;
    private final AcademyScheduleRepository scheduleRepository;

    @Transactional(readOnly = true)
    public List<ScheduleSlotDto> findAllSlots() {
        return scheduleSlotRepository.findAllByOrderByDayOfWeekAscStartTimeAsc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public ScheduleSlotDto createSlot(ScheduleSlotRequest req) {
        if (req == null || req.scheduleId() == null) {
            throw new IllegalArgumentException("시간표 ID는 필수입니다.");
        }
        var schedule = scheduleRepository.findById(req.scheduleId())
                .orElseThrow(() -> new IllegalArgumentException("시간표를 찾을 수 없습니다."));
        if (req.dayOfWeek() == null || req.startTime() == null || req.endTime() == null) {
            throw new IllegalArgumentException("요일과 시작/종료 시간은 필수입니다.");
        }
        DayOfWeek day = DayOfWeek.valueOf(req.dayOfWeek());
        LocalTime start = LocalTime.parse(req.startTime());
        LocalTime end = LocalTime.parse(req.endTime());
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("종료 시간은 시작 시간보다 늦어야 합니다.");
        }

        ScheduleSlot slot = new ScheduleSlot();
        slot.setSchedule(schedule);
        slot.setDayOfWeek(day);
        slot.setStartTime(start);
        slot.setEndTime(end);
        slot.setNote(req.note());
        return toDto(scheduleSlotRepository.save(slot));
    }

    @Transactional
    public void deleteSlot(Long id) {
        if (id == null) return;
        if (scheduleSlotRepository.existsById(id)) {
            scheduleSlotRepository.deleteById(id);
        }
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
