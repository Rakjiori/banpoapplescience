package com.example.sbb.repository;

import com.example.sbb.domain.ConsultationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConsultationRequestRepository extends JpaRepository<ConsultationRequest, Long> {
    List<ConsultationRequest> findAllByOrderByCreatedAtDesc();
    List<ConsultationRequest> findByCreatedAtAfterOrderByCreatedAtDesc(java.time.LocalDateTime after);
    List<ConsultationRequest> findByContactedFalseOrderByCreatedAtDesc();
}
