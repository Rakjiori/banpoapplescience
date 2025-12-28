package com.example.sbb.repository;

import com.example.sbb.domain.ScheduleSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScheduleSlotRepository extends JpaRepository<ScheduleSlot, Long> {
    List<ScheduleSlot> findBySchedule_Id(Long scheduleId);
    List<ScheduleSlot> findAllByOrderByDayOfWeekAscStartTimeAsc();
}
