package com.example.sbb.service;

import com.example.sbb.domain.notification.PendingNotification;
import com.example.sbb.domain.group.GroupMember;
import com.example.sbb.domain.group.GroupNotice;
import com.example.sbb.domain.group.GroupTask;
import com.example.sbb.domain.quiz.QuizQuestion;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.repository.GroupMemberRepository;
import com.example.sbb.repository.GroupNoticeRepository;
import com.example.sbb.repository.GroupTaskRepository;
import com.example.sbb.repository.PendingNotificationRepository;
import com.example.sbb.repository.QuizQuestionRepository;
import com.example.sbb.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final PendingNotificationRepository pendingNotificationRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupNoticeRepository groupNoticeRepository;
    private final GroupTaskRepository groupTaskRepository;

    @Transactional
    public void scheduleTwiceDaily() {
        LocalDate today = LocalDate.now();
        LocalDateTime[] slots = new LocalDateTime[] {
                LocalDateTime.of(today, LocalTime.of(9, 0)),
                LocalDateTime.of(today, LocalTime.of(21, 0))
        };

        userRepository.findAll().forEach(user -> {
            List<QuizQuestion> candidates =
                    quizQuestionRepository.findByUserAndSolvedFalseOrderByCreatedAtAsc(user);

            for (LocalDateTime slot : slots) {
                scheduleForSlot(user, candidates, slot, 1);
            }
        });
    }

    @Transactional
    public void scheduleForSlot(SiteUser user, List<QuizQuestion> pool, LocalDateTime dueAt, int count) {
        if (pool == null || pool.isEmpty()) return;
        int scheduled = 0;
        for (QuizQuestion q : pool) {
            if (scheduled >= count) break;
            boolean exists = pendingNotificationRepository
                    .existsByUser_IdAndQuestion_IdAndDueAt(user.getId(), q.getId(), dueAt);
            if (exists) continue;
            PendingNotification pn = new PendingNotification();
            pn.setUser(user);
            pn.setQuestion(q);
            pn.setDueAt(dueAt);
            pendingNotificationRepository.save(pn);
            scheduled++;
        }
    }

    @Transactional
    public List<PendingNotification> consumeDue(SiteUser user) {
        List<PendingNotification> due = pendingNotificationRepository
                .findByUserAndDeliveredFalseAndDueAtBefore(user, LocalDateTime.now().plusMinutes(1));
        List<PendingNotification> delivered = new ArrayList<>();
        for (PendingNotification pn : due) {
            pn.setDelivered(true);
            delivered.add(pn);
        }
        pendingNotificationRepository.saveAll(delivered);
        return delivered;
    }

    /**
     * 최근 생성된 그룹 공지/과제를 그룹원에게 알림용으로 전달.
     */
    @Transactional(readOnly = true)
    public List<SimpleNotification> recentGroupUpdates(SiteUser user) {
        if (user == null) return List.of();
        List<GroupMember> memberships = groupMemberRepository.findByUser(user);
        if (memberships == null || memberships.isEmpty()) return List.of();
        var groupIds = memberships.stream()
                .filter(m -> m.getGroup() != null)
                .map(m -> m.getGroup().getId())
                .collect(Collectors.toSet());
        if (groupIds.isEmpty()) return List.of();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(3);
        List<SimpleNotification> out = new ArrayList<>();
        // 공지
        groupIds.forEach(id -> {
            var group = memberships.stream().map(GroupMember::getGroup)
                    .filter(g -> g != null && g.getId().equals(id))
                    .findFirst().orElse(null);
            if (group == null) return;
            groupNoticeRepository.findByGroupOrderByCreatedAtDesc(group).stream()
                    .filter(n -> n.getCreatedAt() != null && n.getCreatedAt().isAfter(cutoff))
                    .forEach(n -> out.add(new SimpleNotification(
                            "새 공지: " + n.getTitle(),
                            n.getContent(),
                            "/groups/" + group.getId(),
                            n.getCreatedAt(),
                            "공지"
                    )));
            groupTaskRepository.findByGroupOrderByDueDateAscCreatedAtDesc(group).stream()
                    .filter(t -> t.getCreatedAt() != null && t.getCreatedAt().isAfter(cutoff))
                    .forEach(t -> out.add(new SimpleNotification(
                            "새 과제: " + t.getTitle(),
                            t.getDescription(),
                            "/groups/" + group.getId(),
                            t.getCreatedAt(),
                            "과제"
                    )));
        });
        // 최신순 정렬
        out.sort((a,b) -> b.createdAt().compareTo(a.createdAt()));
        // 너무 많으면 상위 10개만
        return out.size() > 10 ? out.subList(0, 10) : out;
    }

    public record SimpleNotification(String title, String body, String url, LocalDateTime createdAt, String kind) {}
}
