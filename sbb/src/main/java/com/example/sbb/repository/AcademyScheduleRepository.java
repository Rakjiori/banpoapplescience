package com.example.sbb.repository;

import com.example.sbb.domain.AcademySchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AcademyScheduleRepository extends JpaRepository<AcademySchedule, Long> {
    List<AcademySchedule> findAllByOrderBySortOrderAscCreatedAtDesc();
}
