package com.example.sbb.controller;

import com.example.sbb.domain.question.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/question")
public class QuestionController {
  private final QuestionService questionService;

  @GetMapping("/list")
  public String list(Model model) {
    model.addAttribute("questions", questionService.getDummyList()); // DB 전 단계
    return "question_list";
  }
}
