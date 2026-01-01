package com.example.sbb.service;

import com.example.sbb.domain.notification.WebPushSubscription;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.dto.PushSubscriptionRequest;
import com.example.sbb.repository.WebPushSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebPushService {

    static {
        // Ensure BC provider is registered for EC keys
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private final WebPushSubscriptionRepository subscriptionRepository;

    @Value("${push.vapid.public:}")
    private String vapidPublicKey;
    @Value("${push.vapid.private:}")
    private String vapidPrivateKey;
    @Value("${push.vapid.subject:mailto:admin@example.com}")
    private String vapidSubject;

    @Transactional
    public void saveSubscription(SiteUser user, PushSubscriptionRequest req) {
        if (req == null || req.endpoint() == null || req.p256dh() == null || req.auth() == null) {
            throw new IllegalArgumentException("구독 정보가 올바르지 않습니다.");
        }
        WebPushSubscription sub = subscriptionRepository
                .findByEndpoint(req.endpoint())
                .orElseGet(WebPushSubscription::new);
        sub.setEndpoint(req.endpoint());
        sub.setP256dh(req.p256dh());
        sub.setAuth(req.auth());
        sub.setUserAgent(req.userAgent());
        sub.setUser(user);
        subscriptionRepository.save(sub);
    }

    @Transactional
    public void removeByEndpoint(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) return;
        subscriptionRepository.deleteByEndpoint(endpoint);
    }

    @Transactional(readOnly = true)
    public String getPublicKey() {
        return vapidPublicKey;
    }

    @Transactional(readOnly = true)
    public void pushNotifications(SiteUser user, List<? extends PushPayload> payloads) {
        if (user == null || payloads == null || payloads.isEmpty()) return;
        if (!ready()) return;
        List<WebPushSubscription> subs = subscriptionRepository.findByUser(user);
        if (subs == null || subs.isEmpty()) return;

        subs.forEach(sub -> payloads.forEach(payload -> {
            try {
                send(sub, payload);
            } catch (Exception e) {
                log.warn("웹푸시 전송 실패 endpoint={}: {}", sub.getEndpoint(), e.getMessage());
            }
        }));
    }

    private boolean ready() {
        return vapidPublicKey != null && !vapidPublicKey.isBlank()
                && vapidPrivateKey != null && !vapidPrivateKey.isBlank();
    }

    private void send(WebPushSubscription sub, PushPayload payload) throws Exception {
        Subscription.Keys keys = new Subscription.Keys(sub.getP256dh(), sub.getAuth());
        Subscription subscription = new Subscription(sub.getEndpoint(), keys);
        String json = """
                {"title":"%s","body":"%s","url":"%s"}
                """.formatted(
                escape(payload.title()),
                escape(payload.body()),
                escape(payload.url() == null ? "" : payload.url())
        );
        Notification notification = new Notification(subscription, json);
        PushService service = new PushService(vapidPublicKey, vapidPrivateKey, vapidSubject);
        service.send(notification);
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\"", "\\\"");
    }

    public interface PushPayload {
        String title();
        String body();
        String url();
    }
}
