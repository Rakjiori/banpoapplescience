package com.example.sbb.controller;

import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.domain.user.UserService;
import com.example.sbb.dto.PushSubscriptionRequest;
import com.example.sbb.service.WebPushService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class PushSubscriptionController {

    private final WebPushService webPushService;
    private final UserService userService;

    @GetMapping("/public-key")
    public ResponseEntity<String> publicKey() {
        String key = webPushService.getPublicKey();
        if (key == null || key.isBlank()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(key);
    }

    @PostMapping("/subscribe")
    public ResponseEntity<?> subscribe(@RequestBody PushSubscriptionRequest request, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        SiteUser user = userService.getUser(principal.getName());
        webPushService.saveSubscription(user, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<?> unsubscribe(@RequestBody PushSubscriptionRequest request, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        webPushService.removeByEndpoint(request.endpoint());
        return ResponseEntity.ok().build();
    }
}
