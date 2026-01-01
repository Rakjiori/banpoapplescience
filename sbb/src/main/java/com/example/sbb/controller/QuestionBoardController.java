package com.example.sbb.controller;

import com.example.sbb.domain.QuestionComment;
import com.example.sbb.domain.QuestionPost;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.domain.user.UserService;
import com.example.sbb.repository.QuestionCommentRepository;
import com.example.sbb.repository.QuestionPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionBoardController {

    private final QuestionPostRepository postRepository;
    private final QuestionCommentRepository commentRepository;
    private final UserService userService;

    @GetMapping
    public List<PostDto> list(Principal principal) {
        SiteUser actor = principal != null ? userService.getUser(principal.getName()) : null;
        return postRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(p -> PostDto.from(p, canDelete(actor, p.getAuthor())))
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> detail(@PathVariable Long id, Principal principal) {
        SiteUser actor = principal != null ? userService.getUser(principal.getName()) : null;
        return postRepository.findById(id)
                .map(post -> ResponseEntity.ok(new PostDetailDto(
                        PostDto.from(post, canDelete(actor, post.getAuthor())),
                        commentRepository.findByPostOrderByCreatedAtDesc(post).stream()
                                .map(c -> CommentDto.from(c, canDelete(actor, c.getAuthor())))
                                .toList()
                )))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateRequest req, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        if (req == null || req.title == null || req.title.isBlank() || req.content == null || req.content.isBlank()) {
            return ResponseEntity.badRequest().body("invalid");
        }
        SiteUser author = userService.getUser(principal.getName());
        QuestionPost post = new QuestionPost();
        post.setTitle(req.title.trim());
        post.setContent(req.content.trim());
        post.setAuthor(author);
        QuestionPost saved = postRepository.save(post);
        return ResponseEntity.ok(PostDto.from(saved, true));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<?> comment(@PathVariable Long id, @RequestBody CreateRequest req, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        if (req == null || req.content == null || req.content.isBlank()) {
            return ResponseEntity.badRequest().body("invalid");
        }
        QuestionPost post = postRepository.findById(id).orElse(null);
        if (post == null) return ResponseEntity.notFound().build();
        SiteUser author = userService.getUser(principal.getName());
        QuestionComment c = new QuestionComment();
        c.setPost(post);
        c.setAuthor(author);
        c.setContent(req.content.trim());
        QuestionComment saved = commentRepository.save(c);
        return ResponseEntity.ok(CommentDto.from(saved, true));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        SiteUser actor = userService.getUser(principal.getName());
        QuestionPost post = postRepository.findById(id).orElse(null);
        if (post == null) return ResponseEntity.notFound().build();
        if (!canDelete(actor, post.getAuthor())) return ResponseEntity.status(403).build();
        postRepository.delete(post);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Long postId,
                                           @PathVariable Long commentId,
                                           Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        SiteUser actor = userService.getUser(principal.getName());
        QuestionPost post = postRepository.findById(postId).orElse(null);
        if (post == null) return ResponseEntity.notFound().build();
        QuestionComment comment = commentRepository.findByIdAndPost(commentId, post).orElse(null);
        if (comment == null) return ResponseEntity.notFound().build();
        if (!canDelete(actor, comment.getAuthor())) return ResponseEntity.status(403).build();
        commentRepository.delete(comment);
        return ResponseEntity.ok().build();
    }

    public record CreateRequest(String title, String content) {}

    public record PostDto(Long id, String title, String content, String author, String createdAt, boolean canDelete) {
        static PostDto from(QuestionPost post, boolean canDelete) {
            String date = post.getCreatedAt() != null
                    ? post.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"))
                    : "";
            return new PostDto(
                    post.getId(),
                    post.getTitle(),
                    post.getContent(),
                    post.getAuthor() != null ? post.getAuthor().getUsername() : "익명",
                    date,
                    canDelete
            );
        }
    }

    public record CommentDto(Long id, String content, String author, String createdAt, boolean canDelete) {
        static CommentDto from(QuestionComment c, boolean canDelete) {
            String date = c.getCreatedAt() != null
                    ? c.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"))
                    : "";
            return new CommentDto(
                    c.getId(),
                    c.getContent(),
                    c.getAuthor() != null ? c.getAuthor().getUsername() : "익명",
                    date,
                    canDelete
            );
        }
    }

    public record PostDetailDto(PostDto post, List<CommentDto> comments) {}

    private boolean canDelete(SiteUser actor, SiteUser author) {
        if (actor == null) return false;
        if (author != null && actor.getId().equals(author.getId())) return true;
        return userService.isAdminOrRoot(actor);
    }
}
