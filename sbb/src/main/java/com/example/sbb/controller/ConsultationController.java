package com.example.sbb.controller;

import com.example.sbb.domain.ConsultationRequest;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.domain.user.UserService;
import com.example.sbb.repository.ConsultationRequestRepository;
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

    @PostMapping("/consultations/request")
    public ResponseEntity<?> create(@RequestBody Map<String, String> payload, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body("login required");
        SiteUser user = userService.getUser(principal.getName());
        try {
            String type = payload.get("type");
            String message = payload.getOrDefault("message", "");
            ConsultationRequest req = new ConsultationRequest();
            req.setUser(user);
            req.setType(ConsultationRequest.Type.valueOf(type));
            req.setMessage(message);
            repository.save(req);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("invalid request");
        }
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
}
