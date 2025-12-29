package com.example.sbb.repository;

import com.example.sbb.domain.group.FeePayment;
import com.example.sbb.domain.group.StudyGroup;
import com.example.sbb.domain.user.SiteUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FeePaymentRepository extends JpaRepository<FeePayment, Long> {
    List<FeePayment> findByGroupAndUserOrderByPaidAtDescCreatedAtDesc(StudyGroup group, SiteUser user);
    Optional<FeePayment> findFirstByGroupAndUserOrderByPaidAtDescCreatedAtDesc(StudyGroup group, SiteUser user);
}
