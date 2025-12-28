package com.example.sbb.repository;

import com.example.sbb.domain.group.AttendanceRecord;
import com.example.sbb.domain.group.StudyGroup;
import com.example.sbb.domain.user.SiteUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    List<AttendanceRecord> findByGroupAndDate(StudyGroup group, LocalDate date);
    Optional<AttendanceRecord> findByGroupAndStudentAndDate(StudyGroup group, SiteUser student, LocalDate date);
}
