package com.example.sbb.service;

import com.example.sbb.domain.group.AttendanceRecord;
import com.example.sbb.domain.group.AttendanceStatus;
import com.example.sbb.domain.group.StudyGroup;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.repository.AttendanceRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRecordRepository attendanceRecordRepository;

    public List<AttendanceRecord> listForDate(StudyGroup group, LocalDate date) {
        if (group == null || date == null) return List.of();
        return attendanceRecordRepository.findByGroupAndDate(group, date);
    }

    @Transactional
    public AttendanceRecord markAttendance(StudyGroup group, SiteUser student, SiteUser marker, AttendanceStatus status, LocalDate date) {
        if (group == null || student == null || status == null) return null;
        LocalDate targetDate = date != null ? date : LocalDate.now();
        AttendanceRecord record = attendanceRecordRepository
                .findByGroupAndStudentAndDate(group, student, targetDate)
                .orElseGet(AttendanceRecord::new);
        record.setGroup(group);
        record.setStudent(student);
        record.setMarkedBy(marker);
        record.setDate(targetDate);
        record.setStatus(status);
        return attendanceRecordRepository.save(record);
    }
}
