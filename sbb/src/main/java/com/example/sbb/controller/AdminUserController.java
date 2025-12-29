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
    private final com.example.sbb.repository.GroupMemberRepository groupMemberRepository;
    private final com.example.sbb.repository.FriendRepository friendRepository;
    private final com.example.sbb.repository.FriendRequestRepository friendRequestRepository;
    private final com.example.sbb.repository.FriendShareRequestRepository friendShareRequestRepository;
    private final com.example.sbb.repository.GroupInviteRepository groupInviteRepository;
    private final com.example.sbb.repository.DocumentFileRepository documentFileRepository;
    private final com.example.sbb.repository.QuizQuestionRepository quizQuestionRepository;
    private final com.example.sbb.repository.ProblemRepository problemRepository;

    @GetMapping
    public String list(Model model, Principal principal, RedirectAttributes rttr) {
        SiteUser actor = currentUser(principal);
        if (actor == null) return "redirect:/login";
        if (!userService.isAdminOrRoot(actor)) {
            rttr.addFlashAttribute("error", "관리자 이상만 접근할 수 있습니다.");
            return "redirect:/";
        }
        var users = userService.findAll().stream()
                .filter(u -> !"ROLE_DELETED".equals(u.getRole()))
                .toList();
        model.addAttribute("users", users);
        model.addAttribute("rootUsername", actor.getUsername());
        model.addAttribute("bySchool", groupBySchool(users));
        model.addAttribute("byGrade", groupByGrade(users));
        model.addAttribute("isRoot", userService.isRoot(actor));
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

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes rttr) {
        SiteUser actor = currentUser(principal);
        if (actor == null) return "redirect:/login";
        if (!userService.isAdminOrRoot(actor)) {
            rttr.addFlashAttribute("error", "관리자 이상만 계정을 삭제할 수 있습니다.");
            return "redirect:/";
        }
        boolean ok = userService.deleteUser(actor, id, groupMemberRepository, friendRepository, friendRequestRepository,
                friendShareRequestRepository, groupInviteRepository, documentFileRepository, quizQuestionRepository, problemRepository);
        if (ok) rttr.addFlashAttribute("message", "계정을 삭제했습니다.");
        else rttr.addFlashAttribute("error", "계정을 삭제할 수 없습니다.");
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

    private java.util.Map<String, java.util.List<SiteUser>> groupBySchool(java.util.List<SiteUser> users) {
        return users.stream()
                .filter(u -> u.getSchoolName() != null && !u.getSchoolName().isBlank())
                .collect(java.util.stream.Collectors.groupingBy(
                        u -> u.getSchoolName().trim(),
                        java.util.TreeMap::new,
                        java.util.stream.Collectors.toList()
                ));
    }

    private java.util.Map<String, java.util.List<SiteUser>> groupByGrade(java.util.List<SiteUser> users) {
        return users.stream()
                .filter(u -> u.getGrade() != null && !u.getGrade().isBlank())
                .collect(java.util.stream.Collectors.groupingBy(
                        u -> u.getGrade().trim(),
                        java.util.TreeMap::new,
                        java.util.stream.Collectors.toList()
                ));
    }
}
