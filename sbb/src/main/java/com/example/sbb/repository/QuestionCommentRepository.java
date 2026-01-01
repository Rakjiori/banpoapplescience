package com.example.sbb.repository;

import com.example.sbb.domain.QuestionComment;
import com.example.sbb.domain.QuestionPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionCommentRepository extends JpaRepository<QuestionComment, Long> {
    List<QuestionComment> findByPostOrderByCreatedAtDesc(QuestionPost post);
    Optional<QuestionComment> findByIdAndPost(Long id, QuestionPost post);
}
