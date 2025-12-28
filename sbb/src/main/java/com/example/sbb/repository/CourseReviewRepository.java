package com.example.sbb.repository;

import com.example.sbb.domain.CourseReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {
    List<CourseReview> findAllByOrderByCreatedAtDesc();
}
