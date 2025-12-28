package com.example.sbb.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class AcademySchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String summary;

    // 예: 중등, 고1, 고2, 고3, 학교별 등
    @Column(nullable = false)
    private String grade;

    // 예: 물리, 화학, 생명, 지구
    @Column(nullable = false)
    private String subject;

    // 예: 1과목, 2과목
    @Column(nullable = false)
    private String courseType;

    // 예: 세화고, 세화여고 등
    @Column(nullable = false)
    private String school;

    @Column(columnDefinition = "TEXT")
    private String description;

    // 요일/시간 안내 (줄바꿈으로 여러 타임 입력)
    @Column(columnDefinition = "TEXT")
    private String weeklyPlan;

    @Column(nullable = false, columnDefinition = "int default 0")
    private int sortOrder = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
