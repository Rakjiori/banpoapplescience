package com.example.sbb.service;

import com.example.sbb.domain.AdminNote;
import com.example.sbb.domain.AdminNoteComment;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.service.WebPushService.PushPayload;
import com.example.sbb.service.WebPushService;
import com.example.sbb.domain.user.UserService;
import com.example.sbb.repository.AdminNoteCommentRepository;
import com.example.sbb.repository.AdminNoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminNoteService {

    private final AdminNoteRepository adminNoteRepository;
    private final AdminNoteCommentRepository adminNoteCommentRepository;
    private final WebPushService webPushService;
    private final UserService userService;

    public List<AdminNote> list() {
        return adminNoteRepository.findAllByOrderByCreatedAtDesc();
    }

    public AdminNote get(Long id) {
        return adminNoteRepository.findWithComments(id)
                .orElseThrow(() -> new IllegalArgumentException("글을 찾을 수 없습니다."));
    }

    @Transactional
    public AdminNote create(String title, String content, SiteUser author) {
        if (!StringUtils.hasText(title) || !StringUtils.hasText(content)) {
            throw new IllegalArgumentException("제목과 내용을 입력해주세요.");
        }
        AdminNote note = new AdminNote();
        note.setTitle(title.trim());
        note.setContent(content.trim());
        note.setAuthor(author);
        AdminNote saved = adminNoteRepository.save(note);
        notifyAdmins(saved);
        return saved;
    }

    @Transactional
    public AdminNote update(Long id, String title, String content, SiteUser editor) {
        AdminNote note = get(id);
        if (!StringUtils.hasText(title) || !StringUtils.hasText(content)) {
            throw new IllegalArgumentException("제목과 내용을 입력해주세요.");
        }
        note.setTitle(title.trim());
        note.setContent(content.trim());
        note.setAuthor(note.getAuthor()); // author unchanged
        return adminNoteRepository.save(note);
    }

    @Transactional
    public void delete(Long id) {
        adminNoteRepository.deleteById(id);
    }

    @Transactional
    public AdminNoteComment addComment(Long noteId, String content, SiteUser author) {
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("댓글 내용을 입력해주세요.");
        }
        AdminNote note = get(noteId);
        AdminNoteComment comment = new AdminNoteComment();
        comment.setNote(note);
        comment.setAuthor(author);
        comment.setContent(content.trim());
        return adminNoteCommentRepository.save(comment);
    }

    @Transactional
    public void deleteComment(Long commentId) {
        adminNoteCommentRepository.deleteById(commentId);
    }

    private void notifyAdmins(AdminNote note) {
        if (note == null) return;
        var admins = userService.findAdminsAndRoot();
        if (admins == null || admins.isEmpty()) return;
        var payload = new AdminNotePayload(
                "[관리자 공지] " + note.getTitle(),
                note.getAuthor() != null ? note.getAuthor().getUsername() : "",
                "/admin/notes/" + note.getId()
        );
        admins.forEach(admin -> webPushService.pushNotifications(admin, List.of(payload)));
    }

    private record AdminNotePayload(String title, String body, String url) implements PushPayload {}
}
