package com.example.sbb.controller;

import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.domain.user.UserService;
import com.example.sbb.service.AcademyContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/admin/content")
@RequiredArgsConstructor
public class AdminContentController {

    private final AcademyContentService contentService;
    private final UserService userService;

    @GetMapping
    public String page(Model model, Principal principal, RedirectAttributes rttr) {
        SiteUser actor = currentUser(principal);
        if (actor == null) return "redirect:/login";
        if (!userService.isAdminOrRoot(actor)) {
            rttr.addFlashAttribute("error", "관리자만 접근할 수 있습니다.");
            return "redirect:/";
        }
        model.addAttribute("schedules", contentService.listSchedules());
        model.addAttribute("slots", contentService.listSlots());
        model.addAttribute("reviews", contentService.listReviews());
        model.addAttribute("announcements", contentService.listAnnouncements());
        return "admin_content";
    }

    @PostMapping("/schedules")
    public String addSchedule(@RequestParam String subject,
                              @RequestParam String courseType,
                              @RequestParam String school,
                              @RequestParam(required = false) String slotDayOfWeek,
                              @RequestParam(required = false) String slotStartTime,
                              @RequestParam(required = false) String slotEndTime,
                              @RequestParam(required = false) String slotNote,
                              @RequestParam(required = false) Integer sortOrder,
                              Principal principal,
                              RedirectAttributes rttr) {
        SiteUser actor = currentUser(principal);
        if (actor == null) return "redirect:/login";
        if (!userService.isAdminOrRoot(actor)) {
            rttr.addFlashAttribute("error", "관리자만 추가할 수 있습니다.");
            return "redirect:/";
        }
        try {
            var schedule = contentService.createSchedule(subject, courseType, school, sortOrder);
            if (slotDayOfWeek != null && !slotDayOfWeek.isBlank()
                    && slotStartTime != null && slotEndTime != null) {
                contentService.addSlot(schedule.getId(),
                        java.time.DayOfWeek.valueOf(slotDayOfWeek),
                        java.time.LocalTime.parse(slotStartTime),
                        java.time.LocalTime.parse(slotEndTime),
                        slotNote);
            }
            rttr.addFlashAttribute("message", "시간표를 추가했습니다.");
        } catch (Exception e) {
            rttr.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/content";
    }

    @PostMapping("/schedules/{id}/slots")
    public String addSlot(@PathVariable Long id,
                          @RequestParam String dayOfWeek,
                          @RequestParam String startTime,
                          @RequestParam String endTime,
                          @RequestParam(required = false) String note,
                          Principal principal,
                          RedirectAttributes rttr) {
        SiteUser actor = currentUser(principal);
        if (actor == null) return "redirect:/login";
        if (!userService.isAdminOrRoot(actor)) {
            rttr.addFlashAttribute("error", "관리자만 추가할 수 있습니다.");
            return "redirect:/";
        }
        try {
            contentService.addSlot(id,
                    java.time.DayOfWeek.valueOf(dayOfWeek),
                    java.time.LocalTime.parse(startTime),
                    java.time.LocalTime.parse(endTime),
                    note);
            rttr.addFlashAttribute("message", "시간을 추가했습니다.");
        } catch (Exception e) {
            rttr.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/content";
    }

    @PostMapping("/slots/{id}/delete")
    public String deleteSlot(@PathVariable Long id,
                             Principal principal,
                             RedirectAttributes rttr) {
        SiteUser actor = currentUser(principal);
        if (actor == null) return "redirect:/login";
        if (!userService.isAdminOrRoot(actor)) {
            rttr.addFlashAttribute("error", "관리자만 삭제할 수 있습니다.");
            return "redirect:/";
        }
        if (contentService.deleteSlot(id)) {
            rttr.addFlashAttribute("message", "시간을 삭제했습니다.");
        } else {
            rttr.addFlashAttribute("error", "삭제할 수 없습니다.");
        }
        return "redirect:/admin/content";
    }

    @PostMapping("/schedules/{id}/delete")
    public String deleteSchedule(@PathVariable Long id,
                                 Principal principal,
                                 RedirectAttributes rttr) {
        SiteUser actor = currentUser(principal);
        if (actor == null) return "redirect:/login";
        if (!userService.isAdminOrRoot(actor)) {
            rttr.addFlashAttribute("error", "관리자만 삭제할 수 있습니다.");
            return "redirect:/";
        }
        if (contentService.deleteSchedule(id)) {
            rttr.addFlashAttribute("message", "시간표를 삭제했습니다.");
        } else {
            rttr.addFlashAttribute("error", "삭제할 수 없습니다.");
        }
        return "redirect:/admin/content";
    }

    @PostMapping("/reviews")
    public String addReview(@RequestParam String author,
                            @RequestParam(required = false) String highlight,
                            @RequestParam int rating,
                            @RequestParam String content,
                            Principal principal,
                            RedirectAttributes rttr) {
        SiteUser actor = currentUser(principal);
        if (actor == null) return "redirect:/login";
        if (!userService.isAdminOrRoot(actor)) {
            rttr.addFlashAttribute("error", "관리자만 추가할 수 있습니다.");
            return "redirect:/";
        }
        try {
            contentService.createReview(author, highlight, rating, content);
            rttr.addFlashAttribute("message", "수강후기를 추가했습니다.");
        } catch (Exception e) {
            rttr.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/content";
    }

    @PostMapping("/reviews/{id}/delete")
    public String deleteReview(@PathVariable Long id,
                               Principal principal,
                               RedirectAttributes rttr) {
        SiteUser actor = currentUser(principal);
        if (actor == null) return "redirect:/login";
        if (!userService.isAdminOrRoot(actor)) {
            rttr.addFlashAttribute("error", "관리자만 삭제할 수 있습니다.");
            return "redirect:/";
        }
        if (contentService.deleteReview(id)) {
            rttr.addFlashAttribute("message", "수강후기를 삭제했습니다.");
        } else {
            rttr.addFlashAttribute("error", "삭제할 수 없습니다.");
        }
        return "redirect:/admin/content";
    }

    @PostMapping("/announcements")
    public String addAnnouncement(@RequestParam String title,
                                  @RequestParam String content,
                                  @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd") java.time.LocalDate publishedAt,
                                  Principal principal,
                                  RedirectAttributes rttr) {
        SiteUser actor = currentUser(principal);
        if (actor == null) return "redirect:/login";
        if (!userService.isAdminOrRoot(actor)) {
            rttr.addFlashAttribute("error", "관리자만 추가할 수 있습니다.");
            return "redirect:/";
        }
        try {
            contentService.createAnnouncement(title, content, publishedAt);
            rttr.addFlashAttribute("message", "공지사항을 추가했습니다.");
        } catch (Exception e) {
            rttr.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/content";
    }

    @PostMapping("/announcements/{id}/delete")
    public String deleteAnnouncement(@PathVariable Long id,
                                     Principal principal,
                                     RedirectAttributes rttr) {
        SiteUser actor = currentUser(principal);
        if (actor == null) return "redirect:/login";
        if (!userService.isAdminOrRoot(actor)) {
            rttr.addFlashAttribute("error", "관리자만 삭제할 수 있습니다.");
            return "redirect:/";
        }
        if (contentService.deleteAnnouncement(id)) {
            rttr.addFlashAttribute("message", "공지사항을 삭제했습니다.");
        } else {
            rttr.addFlashAttribute("error", "삭제할 수 없습니다.");
        }
        return "redirect:/admin/content";
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
