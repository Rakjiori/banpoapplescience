package com.example.sbb.controller;

import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public String list(Model model, Principal principal, RedirectAttributes rttr) {
        SiteUser actor = currentUser(principal);
        if (actor == null) return "redirect:/login";
        if (!userService.isRoot(actor)) {
            rttr.addFlashAttribute("error", "루트 계정만 접근할 수 있습니다.");
            return "redirect:/";
        }
        model.addAttribute("users", userService.findAll());
        model.addAttribute("rootUsername", actor.getUsername());
        return "admin_users";
    }

    @PostMapping("/{id}/promote")
    public String promote(@PathVariable Long id, Principal principal, RedirectAttributes rttr) {
        SiteUser actor = currentUser(principal);
        if (actor == null) return "redirect:/login";
        if (!userService.isRoot(actor)) {
            rttr.addFlashAttribute("error", "루트 계정만 관리자를 지정할 수 있습니다.");
            return "redirect:/";
        }
        try {
            userService.promoteToAdmin(actor, id);
            rttr.addFlashAttribute("message", "관리자로 지정했습니다.");
        } catch (Exception e) {
            rttr.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/revoke")
    public String revoke(@PathVariable Long id, Principal principal, RedirectAttributes rttr) {
        SiteUser actor = currentUser(principal);
        if (actor == null) return "redirect:/login";
        if (!userService.isRoot(actor)) {
            rttr.addFlashAttribute("error", "루트 계정만 관리자 해제할 수 있습니다.");
            return "redirect:/";
        }
        try {
            boolean ok = userService.revokeAdmin(actor, id);
            if (ok) rttr.addFlashAttribute("message", "관리자 권한을 해제했습니다.");
            else rttr.addFlashAttribute("error", "관리자 권한을 해제할 수 없습니다.");
        } catch (Exception e) {
            rttr.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    private SiteUser currentUser(Principal principal) {
        if (principal == null) return null;
        try {
            return userService.getUser(principal.getName());
        } catch (Exception e) {
            return null;
        }
    }
}
