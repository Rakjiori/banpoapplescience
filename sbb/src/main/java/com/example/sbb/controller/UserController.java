package com.example.sbb.controller;

import com.example.sbb.domain.user.AccountType;
import com.example.sbb.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("accountTypes", AccountType.values());
        return "user/signup_form";
    }

    @PostMapping("/signup")
    public String signupSubmit(@RequestParam String username,
                               @RequestParam String password,
                               @RequestParam(name = "confirmPassword") String confirmPassword,
                               @RequestParam(required = false, name = "accountType") String accountTypeRaw,
                               @RequestParam(required = false, name = "fullName") String fullName,
                               @RequestParam(required = false, name = "schoolName") String schoolName,
                               @RequestParam(required = false, name = "grade") String grade,
                               @RequestParam(required = false, name = "studentPhone") String studentPhone,
                               @RequestParam(required = false, name = "parentPhone") String parentPhone,
                               @RequestParam(required = false, name = "assistantPhone") String assistantPhone,
                               Model model) {
        model.addAttribute("accountTypes", AccountType.values());
        model.addAttribute("enteredUsername", username);
        model.addAttribute("enteredFullName", fullName);
        model.addAttribute("enteredSchoolName", schoolName);
        model.addAttribute("enteredGrade", grade);
        model.addAttribute("enteredStudentPhone", studentPhone);
        model.addAttribute("enteredParentPhone", parentPhone);
        model.addAttribute("enteredAssistantPhone", assistantPhone);
        model.addAttribute("selectedAccountType", accountTypeRaw);

        try {
            if (password == null || !password.equals(confirmPassword)) {
                throw new IllegalArgumentException("비밀번호와 비밀번호 확인이 일치하지 않습니다.");
            }
            AccountType accountType = parseAccountType(accountTypeRaw);
            userService.createUser(username, password, accountType, fullName, schoolName, grade, studentPhone, parentPhone, assistantPhone);
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "user/signup_form";
        }
    }

    @GetMapping("/login")
    public String loginForm() {
        return "user/login_form";
    }

    private AccountType parseAccountType(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return AccountType.valueOf(raw);
        } catch (Exception e) {
            throw new IllegalArgumentException("계정 유형을 다시 선택해주세요.");
        }
    }
}
