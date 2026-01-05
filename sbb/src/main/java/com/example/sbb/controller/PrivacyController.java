package com.example.sbb.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PrivacyController {

    @GetMapping("/privacy")
    public String privacy() {
        // static/privacy.html 로 포워드
        return "forward:/privacy.html";
    }
}
