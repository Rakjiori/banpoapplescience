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
                               @RequestParam(required = false, name = "accountType") String accountTypeRaw,
                               @RequestParam(required = false, name = "fullName") String fullName,
                               @RequestParam(required = false, name = "schoolName") String schoolName,
                               @RequestParam(required = false, name = "grade") String grade,
                               Model model) {
        model.addAttribute("accountTypes", AccountType.values());
        model.addAttribute("enteredUsername", username);
        model.addAttribute("enteredFullName", fullName);
        model.addAttribute("enteredSchoolName", schoolName);
        model.addAttribute("enteredGrade", grade);
        model.addAttribute("selectedAccountType", accountTypeRaw);

        try {
            AccountType accountType = parseAccountType(accountTypeRaw);
            userService.createUser(username, password, accountType, fullName, schoolName, grade);
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
