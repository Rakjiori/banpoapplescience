package com.example.sbb.controller;

import com.example.sbb.domain.ConsultationRequest;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.domain.user.UserService;
import com.example.sbb.repository.ConsultationRequestRepository;
import com.example.sbb.service.WebPushService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.security.Principal;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ConsultationController {

    private final ConsultationRequestRepository repository;
    private final UserService userService;
    private final WebPushService webPushService;

    @PostMapping("/consultations/request")
    public ResponseEntity<?> create(@RequestBody Map<String, String> payload, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        SiteUser user = userService.getUser(principal.getName());
        try {
            String type = payload.get("type");
            String message = payload.getOrDefault("message", "");
            ConsultationRequest req = new ConsultationRequest();
            req.setUser(user);
            req.setType(ConsultationRequest.Type.valueOf(type));
            req.setMessage(message);
            repository.save(req);
            notifyAdmins(req);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("invalid request");
        }
    }

    @GetMapping("/visitcall")
    public String visitCall(Principal principal, Model model) {
        SiteUser actor = principal != null ? userService.getUser(principal.getName()) : null;
        if (actor == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", actor);
        return "visitcall";
    }

    @GetMapping("/admin/consultations")
    public String adminPage(Model model, Principal principal) {
        SiteUser actor = principal != null ? userService.getUser(principal.getName()) : null;
        if (!userService.isAdminOrRoot(actor)) {
            return "redirect:/";
        }
        model.addAttribute("consultations", repository.findAllByOrderByCreatedAtDesc());
        return "admin_consultations";
    }

    @PostMapping("/admin/consultations/{id}/toggle")
    public String toggleContacted(@PathVariable Long id, Principal principal) {
        SiteUser actor = principal != null ? userService.getUser(principal.getName()) : null;
        if (!userService.isAdminOrRoot(actor)) {
            return "redirect:/";
        }
        repository.findById(id).ifPresent(req -> {
            req.setContacted(!req.isContacted());
            repository.save(req);
        });
        return "redirect:/admin/consultations";
    }

    private void notifyAdmins(ConsultationRequest req) {
        if (req == null) return;
        var admins = userService.findAdminsAndRoot();
        if (admins == null || admins.isEmpty()) return;
        var payload = new AdminConsultationPayload(
                "상담 요청 (" + req.getType().name() + ")",
                req.getMessage() == null ? "" : req.getMessage(),
                "/admin/consultations",
                "상담"
        );
        admins.forEach(admin -> webPushService.pushNotifications(admin, java.util.List.of(payload)));
    }

    private record AdminConsultationPayload(String title, String body, String url, String kind) implements WebPushService.PushPayload {}
}
