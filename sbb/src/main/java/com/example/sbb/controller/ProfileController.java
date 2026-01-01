package com.example.sbb.controller;

import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;
    private final com.example.sbb.repository.GroupMemberRepository groupMemberRepository;
    private final com.example.sbb.repository.GroupInviteRepository groupInviteRepository;
    private final com.example.sbb.repository.DocumentFileRepository documentFileRepository;
    private final com.example.sbb.repository.QuizQuestionRepository quizQuestionRepository;
    private final com.example.sbb.repository.ProblemRepository problemRepository;

    @GetMapping("/profile")
    public String redirectProfile() {
        return "redirect:/account";
    }

    @GetMapping("/account")
    public String account(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        SiteUser user = userService.getUser(principal.getName());
        model.addAttribute("user", user);
        return "account_settings";
    }

    @PostMapping("/account")
    public String updateAccount(@RequestParam String username,
                                @RequestParam(required = false) String fullName,
                                @RequestParam(required = false) String schoolName,
                                @RequestParam(required = false) String grade,
                                @RequestParam(required = false) String newPassword,
                                @RequestParam(required = false) String newPasswordConfirm,
                                @RequestParam("currentPassword") String currentPassword,
                                Principal principal,
                                Model model,
                                RedirectAttributes rttr) {
        if (principal == null) return "redirect:/login";
        SiteUser user = userService.getUser(principal.getName());

        if (newPassword != null && !newPassword.isBlank()) {
            if (!newPassword.equals(newPasswordConfirm)) {
                model.addAttribute("error", "비밀번호 확인이 일치하지 않습니다.");
                model.addAttribute("user", previewUser(user, username, fullName, schoolName, grade));
                return "account_settings";
            }
        }

        try {
            SiteUser updated = userService.updateAccount(user, username, fullName, schoolName, grade, newPassword, currentPassword);
            rttr.addFlashAttribute("message", "내 정보를 수정했습니다.");
            return "redirect:/account";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("user", previewUser(user, username, fullName, schoolName, grade));
            return "account_settings";
        }
    }

    @PostMapping("/account/delete")
    public String deleteAccount(@RequestParam("currentPassword") String currentPassword,
                                Principal principal,
                                RedirectAttributes rttr) {
        if (principal == null) return "redirect:/login";
        SiteUser user = userService.getUser(principal.getName());
        try {
            userService.selfDelete(user, currentPassword, groupMemberRepository,
                    groupInviteRepository, documentFileRepository, quizQuestionRepository, problemRepository);
            rttr.addFlashAttribute("message", "계정을 탈퇴했습니다.");
            return "redirect:/logout";
        } catch (IllegalArgumentException e) {
            rttr.addFlashAttribute("error", e.getMessage());
            return "redirect:/account";
        }
    }

    private SiteUser previewUser(SiteUser original,
                                 String username,
                                 String fullName,
                                 String schoolName,
                                 String grade) {
        SiteUser copy = new SiteUser();
        copy.setId(original.getId());
        copy.setUsername(username);
        copy.setFullName(fullName);
        copy.setSchoolName(schoolName);
        copy.setGrade(grade);
        copy.setAccountType(original.getAccountType());
        return copy;
    }
}
