package com.example.sbb.controller;

import com.example.sbb.domain.group.AttendanceStatus;
import com.example.sbb.domain.group.GroupMember;
import com.example.sbb.domain.group.GroupSharedQuestion;
import com.example.sbb.domain.group.StudyGroup;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.domain.user.UserService;
import com.example.sbb.service.AttendanceService;
import com.example.sbb.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;
    private final UserService userService;
    private final AttendanceService attendanceService;

    @GetMapping
    public String list(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        SiteUser user = userService.getUser(principal.getName());
        List<GroupMember> memberships = groupService.memberships(user);
        model.addAttribute("memberships", memberships);
        model.addAttribute("canCreateGroup", userService.isAdminOrRoot(user));
        return "group_list";
    }

    @PostMapping
    public String create(@RequestParam("name") String name,
                         Principal principal,
                         RedirectAttributes rttr) {
        if (principal == null) return "redirect:/login";
        SiteUser user = userService.getUser(principal.getName());
        if (!userService.isAdminOrRoot(user)) {
            rttr.addFlashAttribute("error", "그룹 생성은 관리자/루트만 가능합니다.");
            return "redirect:/groups";
        }
        StudyGroup group = groupService.createGroup(name, user);
        rttr.addFlashAttribute("message", "그룹이 생성되었습니다. 코드: " + group.getJoinCode());
        return "redirect:/groups";
    }

    @PostMapping("/join")
    public String join(@RequestParam("code") String code,
                       Principal principal,
                       RedirectAttributes rttr) {
        if (principal == null) return "redirect:/login";
        SiteUser user = userService.getUser(principal.getName());
        Optional<StudyGroup> group = groupService.joinGroupByCode(code.trim(), user);
        if (group.isPresent()) {
            rttr.addFlashAttribute("message", "그룹에 참여했습니다: " + group.get().getName());
        } else {
            rttr.addFlashAttribute("error", "그룹 코드를 찾을 수 없습니다.");
        }
        return "redirect:/groups";
    }

    @PostMapping("/{groupId}/share/{questionId}")
    public String share(@PathVariable Long groupId,
                        @PathVariable Long questionId,
                        Principal principal,
                        RedirectAttributes rttr) {
        if (principal == null) return "redirect:/login";
        SiteUser user = userService.getUser(principal.getName());
        boolean ok = groupService.shareQuestion(groupId, questionId, user);
        if (ok) {
            rttr.addFlashAttribute("message", "그룹에 문제를 공유했습니다.");
        } else {
            rttr.addFlashAttribute("error", "공유할 수 없습니다. 그룹 멤버인지 또는 자신의 문제인지 확인하세요.");
        }
        return "redirect:/groups/" + groupId;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         Principal principal,
                         Model model,
                         RedirectAttributes rttr) {
        if (principal == null) return "redirect:/login";
        SiteUser user = userService.getUser(principal.getName());
        List<GroupMember> memberships = groupService.memberships(user);
        Optional<StudyGroup> groupOpt = memberships.stream()
                .map(GroupMember::getGroup)
                .filter(g -> g.getId().equals(id))
                .findFirst();
        if (groupOpt.isEmpty()) {
            rttr.addFlashAttribute("error", "그룹에 속해 있지 않습니다.");
            return "redirect:/groups";
        }
        StudyGroup group = groupOpt.get();
        model.addAttribute("group", group);
        model.addAttribute("sharedQuestions", groupService.listShared(group));
        model.addAttribute("memberships", memberships);
        model.addAttribute("members", groupService.membersOf(group));
        model.addAttribute("isOwner", groupService.isOwner(group, user));
        boolean canManage = groupService.isOwner(group, user) || userService.isAdminOrRoot(user);
        model.addAttribute("canManage", canManage);
        var memberUserIds = groupService.membersOf(group).stream()
                .filter(m -> m.getUser() != null)
                .map(m -> m.getUser().getId())
                .collect(Collectors.toSet());
        model.addAttribute("groupNotices", groupService.listNotices(group));
        model.addAttribute("groupTasks", groupService.listTasks(group));
        var weekStart = java.time.LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        var weekEnd = weekStart.plusDays(6);
        var weekDates = java.util.stream.IntStream.range(0,7)
                .mapToObj(weekStart::plusDays)
                .toList();
        var weekRecords = attendanceService.listForRange(group, weekStart, weekEnd);
        var attendanceMap = weekRecords.stream()
                .filter(r -> r.getStudent() != null)
                .collect(java.util.stream.Collectors.toMap(
                        r -> r.getStudent().getId() + "_" + r.getDate(),
                        r -> r.getStatus()
                ));
        model.addAttribute("weekDates", weekDates);
        model.addAttribute("weekAttendance", attendanceMap);
        model.addAttribute("memberUserIds", groupService.membersOf(group).stream()
                .filter(m -> m.getUser() != null)
                .map(m -> m.getUser().getId())
                .collect(Collectors.toSet()));
        if (canManage) {
            model.addAttribute("inviteCandidates", userService.findAll().stream()
                    .filter(u -> u.getId() != null && !memberUserIds.contains(u.getId()))
                    .toList());
        }
        return "group_detail";
    }

    @PostMapping("/use-shared/{sharedId}")
    public String useShared(@PathVariable Long sharedId,
                            Principal principal,
                            RedirectAttributes rttr) {
        if (principal == null) return "redirect:/login";
        SiteUser user = userService.getUser(principal.getName());
        GroupSharedQuestion shared = groupService.getShared(sharedId);
        if (shared == null) {
            rttr.addFlashAttribute("error", "공유된 문제를 찾을 수 없습니다.");
            return "redirect:/groups";
        }
        var copy = groupService.cloneSharedForUser(sharedId, user);
        if (copy == null) {
            rttr.addFlashAttribute("error", "공유된 문제를 사용할 수 없습니다.");
            return "redirect:/groups";
        }
        String redirect = "redirect:/quiz/solve/" + copy.getId();
        if (shared.getGroup() != null) {
            redirect += "?groupId=" + shared.getGroup().getId() + "&discussionQuestionId=" + shared.getQuestion().getId();
        }
        return redirect;
    }

    @PostMapping("/shared/{sharedId}/delete")
    public String deleteShared(@PathVariable Long sharedId,
                               Principal principal,
                               RedirectAttributes rttr) {
        if (principal == null) return "redirect:/login";
        SiteUser user = userService.getUser(principal.getName());
        boolean ok = groupService.deleteShared(sharedId, user);
        if (!ok) {
            rttr.addFlashAttribute("error", "삭제 권한이 없거나 항목이 없습니다.");
        } else {
            rttr.addFlashAttribute("message", "공유된 문제를 삭제했습니다.");
        }
        return "redirect:/groups";
    }

    @PostMapping("/{id}/delete")
    public String deleteGroup(@PathVariable Long id,
                              Principal principal,
                              RedirectAttributes rttr) {
        if (principal == null) return "redirect:/login";
        SiteUser user = userService.getUser(principal.getName());
        List<GroupMember> memberships = groupService.memberships(user);
        Optional<StudyGroup> groupOpt = memberships.stream()
                .map(GroupMember::getGroup)
                .filter(g -> g.getId().equals(id))
                .findFirst();
        if (groupOpt.isEmpty() || !groupService.isOwner(groupOpt.get(), user)) {
            rttr.addFlashAttribute("error", "삭제 권한이 없습니다.");
            return "redirect:/groups";
        }
        groupService.deleteGroup(groupOpt.get());
        rttr.addFlashAttribute("message", "그룹을 삭제했습니다.");
        return "redirect:/groups";
    }

    @PostMapping("/{groupId}/members/invite")
    public String inviteFriends(@PathVariable Long groupId,
                                @RequestParam(value = "userIds", required = false) List<Long> userIds,
                                Principal principal,
                                RedirectAttributes rttr) {
        if (principal == null) return "redirect:/login";
        SiteUser user = userService.getUser(principal.getName());
        List<SiteUser> targets = userIds == null ? List.of() : userIds.stream()
                .map(userService::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
        int invited = groupService.inviteFriends(groupId, user, targets);
        if (invited > 0) rttr.addFlashAttribute("message", invited + "명을 그룹에 바로 추가했습니다.");
        else rttr.addFlashAttribute("error", "추가에 실패했습니다. 권한 또는 선택을 확인하세요.");
        return "redirect:/groups/" + groupId;
    }

    @PostMapping("/{groupId}/members/{memberId}/remove")
    public String removeMember(@PathVariable Long groupId,
                               @PathVariable Long memberId,
                               Principal principal,
                               RedirectAttributes rttr) {
        if (principal == null) return "redirect:/login";
        SiteUser user = userService.getUser(principal.getName());
        boolean ok = groupService.removeMember(groupId, memberId, user);
        if (ok) {
            rttr.addFlashAttribute("message", "멤버를 삭제했습니다.");
        } else {
            rttr.addFlashAttribute("error", "제거할 수 없습니다. 그룹장 권한을 확인하세요.");
        }
        return "redirect:/groups/" + groupId;
    }

    @PostMapping("/{groupId}/attendance")
    public String markAttendance(@PathVariable Long groupId,
                                 @RequestParam("studentId") Long studentId,
                                 @RequestParam("date") java.time.LocalDate date,
                                 @RequestParam("status") String status,
                                 Principal principal,
                                 RedirectAttributes rttr) {
        if (principal == null) return "redirect:/login";
        SiteUser actor = userService.getUser(principal.getName());
        List<GroupMember> memberships = groupService.memberships(actor);
        Optional<StudyGroup> groupOpt = memberships.stream()
                .map(GroupMember::getGroup)
                .filter(g -> g.getId().equals(groupId))
                .findFirst();
        if (groupOpt.isEmpty()) {
            rttr.addFlashAttribute("error", "수업에 속해 있지 않습니다.");
            return "redirect:/groups";
        }
        StudyGroup group = groupOpt.get();
        boolean canManage = groupService.isOwner(group, actor) || userService.isAdminOrRoot(actor);
        if (!canManage) {
            rttr.addFlashAttribute("error", "출석을 체크할 권한이 없습니다.");
            return "redirect:/groups/" + groupId;
        }
        Optional<GroupMember> targetMember = groupService.membersOf(group).stream()
                .filter(m -> m.getUser() != null && m.getUser().getId().equals(studentId))
                .findFirst();
        if (targetMember.isEmpty()) {
            rttr.addFlashAttribute("error", "멤버를 찾을 수 없습니다.");
            return "redirect:/groups/" + groupId;
        }
        try {
            AttendanceStatus statusEnum = AttendanceStatus.valueOf(status);
            attendanceService.markAttendance(group, targetMember.get().getUser(), actor, statusEnum, date);
            rttr.addFlashAttribute("message", "출석을 업데이트했습니다.");
        } catch (Exception e) {
            rttr.addFlashAttribute("error", "출석 처리에 실패했습니다.");
        }
        return "redirect:/groups/" + groupId;
    }

    @PostMapping("/{groupId}/notices")
    public String addNotice(@PathVariable Long groupId,
                            @RequestParam String title,
                            @RequestParam(required = false) String content,
                            Principal principal,
                            RedirectAttributes rttr) {
        if (principal == null) return "redirect:/login";
        SiteUser actor = userService.getUser(principal.getName());
        var notice = groupService.addNotice(groupId, actor, title, content);
        if (notice == null) rttr.addFlashAttribute("error", "공지 추가에 실패했습니다. 권한 또는 내용을 확인하세요.");
        else rttr.addFlashAttribute("message", "공지사항을 추가했습니다.");
        return "redirect:/groups/" + groupId;
    }

    @PostMapping("/{groupId}/notices/{noticeId}/delete")
    public String deleteNotice(@PathVariable Long groupId,
                               @PathVariable Long noticeId,
                               Principal principal,
                               RedirectAttributes rttr) {
        if (principal == null) return "redirect:/login";
        SiteUser actor = userService.getUser(principal.getName());
        if (!groupService.deleteNotice(noticeId, actor)) {
            rttr.addFlashAttribute("error", "공지 삭제 권한이 없습니다.");
        } else {
            rttr.addFlashAttribute("message", "공지를 삭제했습니다.");
        }
        return "redirect:/groups/" + groupId;
    }

    @PostMapping("/{groupId}/tasks")
    public String addTask(@PathVariable Long groupId,
                          @RequestParam String title,
                          @RequestParam(required = false) String description,
                          @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd") java.time.LocalDate dueDate,
                          Principal principal,
                          RedirectAttributes rttr) {
        if (principal == null) return "redirect:/login";
        SiteUser actor = userService.getUser(principal.getName());
        var task = groupService.addTask(groupId, actor, title, description, dueDate);
        if (task == null) rttr.addFlashAttribute("error", "과제 추가에 실패했습니다. 권한 또는 내용을 확인하세요.");
        else rttr.addFlashAttribute("message", "과제를 추가했습니다.");
        return "redirect:/groups/" + groupId;
    }

    @PostMapping("/{groupId}/tasks/{taskId}/delete")
    public String deleteTask(@PathVariable Long groupId,
                             @PathVariable Long taskId,
                             Principal principal,
                             RedirectAttributes rttr) {
        if (principal == null) return "redirect:/login";
        SiteUser actor = userService.getUser(principal.getName());
        if (!groupService.deleteTask(taskId, actor)) {
            rttr.addFlashAttribute("error", "과제 삭제 권한이 없습니다.");
        } else {
            rttr.addFlashAttribute("message", "과제를 삭제했습니다.");
        }
        return "redirect:/groups/" + groupId;
    }

    @PostMapping("/{id}/leave")
    public String leave(@PathVariable Long id,
                        Principal principal,
                        RedirectAttributes rttr) {
        if (principal == null) return "redirect:/login";
        SiteUser user = userService.getUser(principal.getName());
        List<GroupMember> memberships = groupService.memberships(user);
        Optional<StudyGroup> groupOpt = memberships.stream()
                .map(GroupMember::getGroup)
                .filter(g -> g.getId().equals(id))
                .findFirst();
        if (groupOpt.isEmpty()) {
            rttr.addFlashAttribute("error", "그룹에 속해 있지 않습니다.");
            return "redirect:/groups";
        }
        boolean ok = groupService.leaveGroup(groupOpt.get(), user);
        if (!ok) {
            rttr.addFlashAttribute("error", "그룹을 나갈 수 없습니다.");
        } else {
            rttr.addFlashAttribute("message", "그룹에서 나갔습니다.");
        }
        return "redirect:/groups";
    }
}
