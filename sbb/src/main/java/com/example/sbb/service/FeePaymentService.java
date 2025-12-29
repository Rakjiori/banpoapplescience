package com.example.sbb.service;

import com.example.sbb.domain.group.FeePayment;
import com.example.sbb.domain.group.StudyGroup;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.repository.FeePaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FeePaymentService {

    private final FeePaymentRepository feePaymentRepository;

    public Optional<FeePayment> latest(StudyGroup group, SiteUser user) {
        if (group == null || user == null) return Optional.empty();
        return feePaymentRepository.findFirstByGroupAndUserOrderByPaidAtDescCreatedAtDesc(group, user);
    }

    public List<FeePayment> history(StudyGroup group, SiteUser user) {
        if (group == null || user == null) return List.of();
        return feePaymentRepository.findByGroupAndUserOrderByPaidAtDescCreatedAtDesc(group, user);
    }

    @Transactional
    public FeePayment recordPayment(StudyGroup group, SiteUser user, SiteUser recorder, Integer amount, LocalDate paidAt, String note) {
        if (group == null || user == null || amount == null || amount < 0) {
            throw new IllegalArgumentException("회비 금액을 확인해주세요.");
        }
        FeePayment payment = new FeePayment();
        payment.setGroup(group);
        payment.setUser(user);
        payment.setRecordedBy(recorder);
        payment.setAmount(amount);
        payment.setPaidAt(paidAt != null ? paidAt : LocalDate.now());
        payment.setNote(note != null ? note.trim() : null);
        return feePaymentRepository.save(payment);
    }
}
