package com.example.sbb.controller;

import com.example.sbb.domain.Folder;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.domain.user.UserService;
import com.example.sbb.domain.document.DocumentFile;
import com.example.sbb.domain.quiz.QuizQuestion;
import com.example.sbb.repository.FolderRepository;
import com.example.sbb.repository.DocumentFileRepository;
import com.example.sbb.repository.QuizQuestionRepository;
import com.example.sbb.repository.ProblemRepository;
import com.example.sbb.repository.GroupSharedQuestionRepository;
import com.example.sbb.repository.PendingNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@RequestMapping("/folders")
@RequiredArgsConstructor
public class FolderController {

    private final FolderRepository folderRepository;
    private final UserService userService;
    private final DocumentFileRepository documentFileRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final ProblemRepository problemRepository;
    private final GroupSharedQuestionRepository sharedQuestionRepository;
    private final PendingNotificationRepository pendingNotificationRepository;

    @GetMapping
    public String list(Model model, Principal principal) {
        return "redirect:/";
    }

    @PostMapping
    public String create(@RequestParam("name") String name,
                         Principal principal,
                         Model model) {
        return "redirect:/";
    }

    @PostMapping("/delete")
    @Transactional
    public String delete(@RequestParam("folderId") Long folderId,
                         Principal principal,
                         Model model) {
        return "redirect:/";
    }
}
