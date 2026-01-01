package com.example.sbb.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class NotificationsPageController {

    @GetMapping("/notifications")
    public String notificationsPage() {
        return "notifications";
    }
}
