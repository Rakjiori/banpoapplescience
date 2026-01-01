package com.example.sbb.repository;

import com.example.sbb.domain.notification.WebPushSubscription;
import com.example.sbb.domain.user.SiteUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WebPushSubscriptionRepository extends JpaRepository<WebPushSubscription, Long> {
    List<WebPushSubscription> findByUser(SiteUser user);
    Optional<WebPushSubscription> findByEndpoint(String endpoint);
    void deleteByEndpoint(String endpoint);
}
