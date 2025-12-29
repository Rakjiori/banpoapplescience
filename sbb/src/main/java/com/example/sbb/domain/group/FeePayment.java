package com.example.sbb.domain.group;

import com.example.sbb.domain.user.SiteUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "group_fee_payment")
@Getter
@Setter
public class FeePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private StudyGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private SiteUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by")
    private SiteUser recordedBy;

    @Column(nullable = false)
    private Integer amount;

    @Column(name = "paid_at", nullable = false)
    private LocalDate paidAt;

    @Column(length = 255)
    private String note;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
