package com.example.sbb.service;

import com.example.sbb.domain.quiz.QuestionDiscussion;
import com.example.sbb.domain.quiz.QuizQuestion;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.domain.user.UserService;
import com.example.sbb.repository.QuestionDiscussionRepository;
import com.example.sbb.repository.QuizQuestionRepository;
import com.example.sbb.service.WebPushService;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscussionService {

    private final QuestionDiscussionRepository discussionRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final UserService userService;
    private final WebPushService webPushService;

    @Transactional(readOnly = true)
    public QuizQuestion getOwnedQuestion(SiteUser user, Long questionId) {
        if (user == null || questionId == null) return null;
        QuizQuestion question = quizQuestionRepository.findById(questionId).orElse(null);
        if (question == null) return null;
        if (!question.getUser().getId().equals(user.getId())) return null;
        return question;
    }

    @Transactional(readOnly = true)
    public QuizQuestion getQuestion(Long questionId) {
        if (questionId == null) return null;
        return quizQuestionRepository.findById(questionId).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<QuestionDiscussion> list(QuizQuestion question) {
        if (question == null) return Collections.emptyList();
        return discussionRepository.findByQuestionOrderByCreatedAtAsc(question);
    }

    @Transactional(readOnly = true)
    public QuestionDiscussion getComment(Long id) {
        if (id == null) return null;
        return discussionRepository.findById(id).orElse(null);
    }

    @Transactional
    public void deleteComment(QuestionDiscussion comment) {
        if (comment == null) return;
        discussionRepository.delete(comment);
    }

    @Transactional
    public QuestionDiscussion addComment(QuizQuestion question, SiteUser user, String content) {
        if (question == null || user == null || content == null || content.isBlank()) return null;
        QuestionDiscussion discussion = new QuestionDiscussion();
        discussion.setQuestion(question);
        discussion.setUser(user);
        discussion.setContent(content.trim());
        QuestionDiscussion saved = discussionRepository.save(discussion);
        notifyAdmins(question, user, saved);
        return saved;
    }

    private void notifyAdmins(QuizQuestion question, SiteUser author, QuestionDiscussion comment) {
        try {
            var admins = userService.findAdminsAndRoot();
            if (admins == null || admins.isEmpty()) return;
            String title = "자유게시판 새 댓글";
            String body = (author != null ? author.getUsername() : "익명") +
                    "님: " + (comment != null ? comment.getContent() : "");
            String url = "/quiz/discussion/" + (question != null ? question.getId() : "");
            var payload = new AdminDiscussionPayload(title, body, url);
            admins.forEach(a -> webPushService.pushNotifications(a, List.of(payload)));
        } catch (Exception e) {
            log.warn("관리자 댓글 알림 실패: {}", e.getMessage());
        }
    }

    private record AdminDiscussionPayload(String title, String body, String url) implements WebPushService.PushPayload {}
}
