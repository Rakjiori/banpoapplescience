package com.example.sbb.repository;

import com.example.sbb.domain.QuestionPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionPostRepository extends JpaRepository<QuestionPost, Long> {
    List<QuestionPost> findAllByOrderByCreatedAtDesc();
}
