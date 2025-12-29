package com.example.sbb.controller;

import com.example.sbb.domain.AdminNote;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.domain.user.UserService;
import com.example.sbb.service.AdminNoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/admin/notes")
@RequiredArgsConstructor
public class AdminNoteController {

    private final AdminNoteService adminNoteService;
    private final UserService userService;

    @GetMapping
    public String list(Model model, Principal principal, RedirectAttributes rttr) {
        SiteUser actor = currentUser(principal);
        if (!ensureAdmin(actor, rttr)) return "redirect:/";
        model.addAttribute("notes", adminNoteService.list());
        return "admin_notes_list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model, Principal principal, RedirectAttributes rttr) {
        SiteUser actor = currentUser(principal);
        if (!ensureAdmin(actor, rttr)) return "redirect:/";
        AdminNote note = adminNoteService.get(id);
        model.addAttribute("note", note);
        return "admin_notes_detail";
    }

    @GetMapping("/new")
    public String newForm(Model model, Principal principal, RedirectAttributes rttr) {
        SiteUser actor = currentUser(principal);
        if (!ensureAdmin(actor, rttr)) return "redirect:/";
        return "admin_notes_form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, Principal principal, RedirectAttributes rttr) {
        SiteUser actor = currentUser(principal);
        if (!ensureAdmin(actor, rttr)) return "redirect:/";
        AdminNote note = adminNoteService.get(id);
        model.addAttribute("note", note);
        return "admin_notes_form";
    }

    @PostMapping
    public String create(@RequestParam String title,
                         @RequestParam String content,
                         Principal principal,
                         RedirectAttributes rttr) {
        SiteUser actor = currentUser(principal);
        if (!ensureAdmin(actor, rttr)) return "redirect:/";
        try {
            AdminNote note = adminNoteService.create(title, content, actor);
            return "redirect:/admin/notes/" + note.getId();
        } catch (IllegalArgumentException e) {
            rttr.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/notes/new";
        }
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @RequestParam String title,
                         @RequestParam String content,
                         Principal principal,
                         RedirectAttributes rttr) {
        SiteUser actor = currentUser(principal);
        if (!ensureAdmin(actor, rttr)) return "redirect:/";
        try {
            adminNoteService.update(id, title, content, actor);
            rttr.addFlashAttribute("message", "수정되었습니다.");
            return "redirect:/admin/notes/" + id;
        } catch (IllegalArgumentException e) {
            rttr.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/notes/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes rttr) {
        SiteUser actor = currentUser(principal);
        if (!ensureAdmin(actor, rttr)) return "redirect:/";
        adminNoteService.delete(id);
        rttr.addFlashAttribute("message", "삭제했습니다.");
        return "redirect:/admin/notes";
    }

    @PostMapping("/{id}/comments")
    public String addComment(@PathVariable Long id,
                             @RequestParam String content,
                             Principal principal,
                             RedirectAttributes rttr) {
        SiteUser actor = currentUser(principal);
        if (!ensureAdmin(actor, rttr)) return "redirect:/";
        try {
            adminNoteService.addComment(id, content, actor);
        } catch (IllegalArgumentException e) {
            rttr.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/notes/" + id;
    }

    @PostMapping("/{noteId}/comments/{commentId}/delete")
    public String deleteComment(@PathVariable Long noteId,
                                @PathVariable Long commentId,
                                Principal principal,
                                RedirectAttributes rttr) {
        SiteUser actor = currentUser(principal);
        if (!ensureAdmin(actor, rttr)) return "redirect:/";
        adminNoteService.deleteComment(commentId);
        return "redirect:/admin/notes/" + noteId;
    }

    private boolean ensureAdmin(SiteUser actor, RedirectAttributes rttr) {
        if (actor == null) return false;
        if (!userService.isAdminOrRoot(actor)) {
            rttr.addFlashAttribute("error", "관리자 이상만 접근할 수 있습니다.");
            return false;
        }
        return true;
    }

    private SiteUser currentUser(Principal principal) {
        if (principal == null) return null;
        try {
            return userService.getUser(principal.getName());
        } catch (Exception e) {
            return null;
        }
    }
}
