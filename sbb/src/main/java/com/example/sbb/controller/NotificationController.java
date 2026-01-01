package com.example.sbb.controller;

import com.example.sbb.domain.notification.PendingNotification;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.domain.user.UserService;
import com.example.sbb.repository.ConsultationRequestRepository;
import com.example.sbb.service.NotificationService;
import com.example.sbb.service.WebPushService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;
    private final ConsultationRequestRepository consultationRequestRepository;
    private final WebPushService webPushService;

    @GetMapping("/due")
    public ResponseEntity<?> due(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        SiteUser user = userService.getUser(principal.getName());
        List<PendingNotification> due = notificationService.consumeDue(user);
        List<NotificationPayload> payloads = due.stream()
                .map(NotificationPayload::fromQuiz)
                .collect(Collectors.toList());
        notificationService.recentGroupUpdates(user).forEach(n ->
                payloads.add(NotificationPayload.fromSimple(n))
        );
        if (userService.isAdminOrRoot(user)) {
            consultationRequestRepository.findByContactedFalseOrderByCreatedAtDesc()
                    .forEach(req -> payloads.add(NotificationPayload.fromConsultation(req)));
        }
        // push to browser if VAPID configured
        webPushService.pushNotifications(user, payloads);
        return ResponseEntity.ok(payloads);
    }

    @GetMapping("/schedule-now")
    public ResponseEntity<?> scheduleNow(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        notificationService.scheduleTwiceDaily();
        return ResponseEntity.ok().build();
    }

    @Data
    @AllArgsConstructor
    static class NotificationPayload implements WebPushService.PushPayload {
        private Long id;
        private String title;
        private String body;
        private String url;
        private String kind;

        static NotificationPayload fromQuiz(PendingNotification pn) {
            String title = "새 알림";
            String body = pn.getQuestion().getQuestionText();
            String url = "/quiz/solve/" + pn.getQuestion().getId();
            return new NotificationPayload(pn.getId(), title, body, url, "공지");
        }

        static NotificationPayload fromSimple(NotificationService.SimpleNotification n) {
            return new NotificationPayload(null, n.title(), n.body(), n.url(), n.kind());
        }

        static NotificationPayload fromConsultation(com.example.sbb.domain.ConsultationRequest req) {
            String title = "상담 요청 (" + req.getType().name() + ")";
            String state = req.isContacted() ? "상담 완료" : "상담 대기";
            String body = (req.getMessage() != null ? req.getMessage() : "") + " [" + state + "]";
            return new NotificationPayload(null, title, body, "/admin/consultations", "상담");
        }

        @Override
        public String title() {
            return title;
        }

        @Override
        public String body() {
            return body;
        }

        @Override
        public String url() {
            return url;
        }
    }
}
