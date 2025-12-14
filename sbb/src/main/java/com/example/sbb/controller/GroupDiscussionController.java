package com.example.sbb.controller;

import com.example.sbb.domain.group.GroupMember;
import com.example.sbb.domain.group.StudyGroup;
import com.example.sbb.domain.quiz.QuestionDiscussion;
import com.example.sbb.domain.quiz.QuizQuestion;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.domain.user.UserService;
import com.example.sbb.service.DiscussionService;
import com.example.sbb.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/groups")
@RequiredArgsConstructor
public class GroupDiscussionController {

    private final GroupService groupService;
    private final DiscussionService discussionService;
    private final UserService userService;

    @GetMapping("/{groupId}/discussion/{questionId}")
    public String view(@PathVariable Long groupId,
                       @PathVariable Long questionId,
                       Principal principal,
                       Model model,
                       RedirectAttributes rttr) {
        if (principal == null) return "redirect:/login";
        SiteUser user = userService.getUser(principal.getName());
        StudyGroup group = ensureMembership(groupId, user, rttr);
        if (group == null) return "redirect:/groups";

        QuizQuestion question = discussionService.getQuestion(questionId);
        if (question == null) {
            rttr.addFlashAttribute("error", "문제를 찾을 수 없습니다.");
            return "redirect:/groups/" + groupId;
        }

        model.addAttribute("group", group);
        model.addAttribute("question", question);
        model.addAttribute("comments", discussionService.list(question));
        return "group_discussion";
    }

    @PostMapping("/{groupId}/discussion/{questionId}")
    public String add(@PathVariable Long groupId,
                      @PathVariable Long questionId,
                      @RequestParam("content") String content,
                      Principal principal,
                      RedirectAttributes rttr) {
        if (principal == null) return "redirect:/login";
        SiteUser user = userService.getUser(principal.getName());
        StudyGroup group = ensureMembership(groupId, user, rttr);
        if (group == null) return "redirect:/groups";

        QuizQuestion question = discussionService.getQuestion(questionId);
        if (question == null) {
            rttr.addFlashAttribute("error", "문제를 찾을 수 없습니다.");
            return "redirect:/groups/" + groupId;
        }

        discussionService.addComment(question, user, content);
        return "redirect:/groups/" + groupId + "/discussion/" + questionId;
    }

    @GetMapping("/{groupId}/discussion/{questionId}/list")
    @ResponseBody
    public ResponseEntity<?> listInline(@PathVariable Long groupId,
                                        @PathVariable Long questionId,
                                        Principal principal,
                                        RedirectAttributes rttr) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        SiteUser user = userService.getUser(principal.getName());
        StudyGroup group = ensureMembership(groupId, user, rttr);
        if (group == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("no group");

        QuizQuestion question = discussionService.getQuestion(questionId);
        if (question == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("no question");

        List<QuestionDiscussion> comments = discussionService.list(question);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
        List<DiscussionDto> dto = comments.stream()
                .map(c -> new DiscussionDto(
                        c.getId(),
                        c.getUser().getUsername(),
                        c.getContent(),
                        c.getCreatedAt() != null ? c.getCreatedAt().format(fmt) : ""
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{groupId}/discussion/{questionId}/add")
    @ResponseBody
    public ResponseEntity<?> addInline(@PathVariable Long groupId,
                                       @PathVariable Long questionId,
                                       @RequestParam("content") String content,
                                       Principal principal,
                                       RedirectAttributes rttr) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        SiteUser user = userService.getUser(principal.getName());
        StudyGroup group = ensureMembership(groupId, user, rttr);
        if (group == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("no group");
        QuizQuestion question = discussionService.getQuestion(questionId);
        if (question == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("no question");
        QuestionDiscussion saved = discussionService.addComment(question, user, content);
        if (saved == null) return ResponseEntity.badRequest().body("invalid");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
        return ResponseEntity.ok(new DiscussionDto(
                saved.getId(),
                saved.getUser().getUsername(),
                saved.getContent(),
                saved.getCreatedAt() != null ? saved.getCreatedAt().format(fmt) : ""
        ));
    }

    private StudyGroup ensureMembership(Long groupId, SiteUser user, RedirectAttributes rttr) {
        if (groupId == null || user == null) return null;
        List<GroupMember> memberships = groupService.memberships(user);
        Optional<StudyGroup> groupOpt = memberships.stream()
                .map(GroupMember::getGroup)
                .filter(g -> g.getId().equals(groupId))
                .findFirst();
        if (groupOpt.isEmpty()) {
            rttr.addFlashAttribute("error", "그룹에 속해 있지 않습니다.");
            return null;
        }
        return groupOpt.get();
    }

    public record DiscussionDto(Long id, String username, String content, String createdAt) {}
}
