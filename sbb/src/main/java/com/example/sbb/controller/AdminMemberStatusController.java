package com.example.sbb.controller;

import com.example.sbb.domain.group.AttendanceStatus;
import com.example.sbb.domain.group.StudyGroup;
import com.example.sbb.domain.group.GroupMember;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.domain.user.UserService;
import com.example.sbb.service.AttendanceService;
import com.example.sbb.service.FeePaymentService;
import com.example.sbb.repository.GroupMemberRepository;
import com.example.sbb.repository.StudyGroupRepository;
import com.example.sbb.repository.StudySessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/members")
@RequiredArgsConstructor
public class AdminMemberStatusController {

    private final UserService userService;
    private final StudyGroupRepository studyGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final StudySessionRepository studySessionRepository;
    private final AttendanceService attendanceService;
    private final FeePaymentService feePaymentService;
    private final com.example.sbb.repository.AttendanceRecordRepository attendanceRecordRepository;

    @GetMapping
    public String view(@RequestParam(value = "groupId", required = false) Long groupId,
                       Model model,
                       Principal principal,
                       RedirectAttributes rttr) {
        SiteUser actor = currentUser(principal);
        if (actor == null) return "redirect:/login";
        if (!userService.isAdminOrRoot(actor)) {
            rttr.addFlashAttribute("error", "관리자 이상만 접근할 수 있습니다.");
            return "redirect:/";
        }

        List<StudyGroup> groups = studyGroupRepository.findAll();
        StudyGroup selected = resolveGroup(groupId, groups);

        model.addAttribute("groups", groups);
        model.addAttribute("selectedGroup", selected);

        if (selected == null) {
            model.addAttribute("members", List.of());
            return "admin_member_status";
        }

        List<GroupMember> members = groupMemberRepository.findByGroupOrderByJoinedAtAsc(selected);
        LocalDate today = LocalDate.now();
        Map<Long, AttendanceStatus> todayAttendance = new HashMap<>();
        attendanceService.listForDate(selected, today).forEach(r -> todayAttendance.put(r.getStudent().getId(), r.getStatus()));

        Map<Long, Long> totalAttendance = new HashMap<>();
        attendanceRecordRepository.findByGroup(selected).forEach(r -> {
            if (r.getStudent() != null && r.getStatus() == com.example.sbb.domain.group.AttendanceStatus.PRESENT) {
                totalAttendance.merge(r.getStudent().getId(), 1L, Long::sum);
            }
        });

        Map<Long, com.example.sbb.domain.group.FeePayment> latestFee = new HashMap<>();
        for (GroupMember member : members) {
            feePaymentService.latest(selected, member.getUser())
                    .ifPresent(p -> latestFee.put(member.getUser().getId(), p));
        }

        model.addAttribute("members", members);
        model.addAttribute("todayAttendance", todayAttendance);
        model.addAttribute("totalAttendance", totalAttendance);
        model.addAttribute("latestFee", latestFee);
        return "admin_member_status";
    }

    @PostMapping("/{groupId}/attendance")
    public String markAttendance(@PathVariable Long groupId,
                                 @RequestParam Long memberId,
                                 @RequestParam String status,
                                 @RequestParam(value = "date", required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                 Principal principal,
                                 RedirectAttributes rttr) {
        SiteUser actor = currentUser(principal);
        if (actor == null) return "redirect:/login";
        if (!userService.isAdminOrRoot(actor)) {
            rttr.addFlashAttribute("error", "관리자 이상만 접근할 수 있습니다.");
            return "redirect:/";
        }
        StudyGroup group = studyGroupRepository.findById(groupId).orElse(null);
        if (group == null) {
            rttr.addFlashAttribute("error", "그룹을 찾을 수 없습니다.");
            return "redirect:/admin/members";
        }
        GroupMember member = groupMemberRepository.findById(memberId).orElse(null);
        if (member == null || member.getUser() == null) {
            rttr.addFlashAttribute("error", "멤버를 찾을 수 없습니다.");
            return "redirect:/admin/members?groupId=" + groupId;
        }
        AttendanceStatus attendanceStatus = parseStatus(status);
        attendanceService.markAttendance(group, member.getUser(), actor, attendanceStatus, date != null ? date : LocalDate.now());
        rttr.addFlashAttribute("message", "출석을 저장했습니다.");
        return "redirect:/admin/members?groupId=" + groupId;
    }

    @PostMapping("/{groupId}/fee")
    public String recordFee(@PathVariable Long groupId,
                            @RequestParam Long memberId,
                            @RequestParam Integer amount,
                            @RequestParam(value = "paidAt", required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paidAt,
                            @RequestParam(required = false) String note,
                            Principal principal,
                            RedirectAttributes rttr) {
        SiteUser actor = currentUser(principal);
        if (actor == null) return "redirect:/login";
        if (!userService.isAdminOrRoot(actor)) {
            rttr.addFlashAttribute("error", "관리자 이상만 접근할 수 있습니다.");
            return "redirect:/";
        }
        StudyGroup group = studyGroupRepository.findById(groupId).orElse(null);
        if (group == null) {
            rttr.addFlashAttribute("error", "그룹을 찾을 수 없습니다.");
            return "redirect:/admin/members";
        }
        GroupMember member = groupMemberRepository.findById(memberId).orElse(null);
        if (member == null || member.getUser() == null) {
            rttr.addFlashAttribute("error", "멤버를 찾을 수 없습니다.");
            return "redirect:/admin/members?groupId=" + groupId;
        }
        try {
            feePaymentService.recordPayment(group, member.getUser(), actor, amount, paidAt, note);
            rttr.addFlashAttribute("message", "회비 기록을 저장했습니다.");
        } catch (IllegalArgumentException e) {
            rttr.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/members?groupId=" + groupId;
    }

    private AttendanceStatus parseStatus(String raw) {
        if (!StringUtils.hasText(raw)) return AttendanceStatus.ABSENT;
        try {
            return AttendanceStatus.valueOf(raw);
        } catch (Exception e) {
            return AttendanceStatus.ABSENT;
        }
    }

    private StudyGroup resolveGroup(Long groupId, List<StudyGroup> groups) {
        if (groupId != null) {
            return studyGroupRepository.findById(groupId).orElse(null);
        }
        if (!groups.isEmpty()) return groups.get(0);
        return null;
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
