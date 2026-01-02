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
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
        Map<Long, Boolean> todayPresent = new HashMap<>();
        attendanceService.listForDate(selected, today).forEach(r -> todayPresent.put(r.getStudent().getId(), r.getStatus() == AttendanceStatus.PRESENT));

        Map<Long, Long> totalAttendance = new HashMap<>();
        attendanceRecordRepository.findByGroup(selected).forEach(r -> {
            if (r.getStudent() != null && r.getStatus() == com.example.sbb.domain.group.AttendanceStatus.PRESENT) {
                totalAttendance.merge(r.getStudent().getId(), 1L, Long::sum);
            }
        });

        java.time.LocalDate cutoff = LocalDate.now().minusMonths(2);
        Map<Long, List<String>> attendanceDays = attendanceRecordRepository.findByGroup(selected).stream()
                .filter(r -> r.getStudent() != null && r.getStatus() == AttendanceStatus.PRESENT && r.getDate() != null && !r.getDate().isBefore(cutoff))
                .collect(Collectors.groupingBy(
                        r -> r.getStudent().getId(),
                        Collectors.mapping(r -> r.getDate().format(java.time.format.DateTimeFormatter.ofPattern("MM/dd")), Collectors.toList())
                ));
        Map<Long, String> attendanceDaysJoined = attendanceDays.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream()
                                .filter(Objects::nonNull)
                                .collect(Collectors.toCollection(java.util.TreeSet::new))
                                .stream()
                                .collect(Collectors.joining(","))
                ));

        Map<Long, com.example.sbb.domain.group.FeePayment> latestFee = new HashMap<>();
        for (GroupMember member : members) {
            feePaymentService.latest(selected, member.getUser())
                    .ifPresent(p -> latestFee.put(member.getUser().getId(), p));
        }

        model.addAttribute("members", members);
        model.addAttribute("todayPresent", todayPresent);
        model.addAttribute("totalAttendance", totalAttendance);
        model.addAttribute("attendanceDays", attendanceDays);
        model.addAttribute("attendanceDaysJoined", attendanceDaysJoined);
        model.addAttribute("latestFee", latestFee);
        model.addAttribute("todayDate", today);
        model.addAttribute("recentDates", recentDates(today));
        return "admin_member_status";
    }

    private List<LocalDate> recentDates(LocalDate today) {
        return IntStream.rangeClosed(0, 29)
                .mapToObj(today::minusDays)
                .toList();
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
