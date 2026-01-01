package com.example.sbb.controller;

import com.example.sbb.domain.group.GroupMember;
import com.example.sbb.domain.group.StudyGroup;
import com.example.sbb.domain.group.GroupNotice;
import com.example.sbb.domain.group.GroupTask;
import com.example.sbb.domain.group.AttendanceRecord;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.domain.user.UserService;
import com.example.sbb.repository.StudyGroupRepository;
import com.example.sbb.service.GroupService;
import com.example.sbb.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/groups")
@RequiredArgsConstructor
public class AdminGroupController {

    private final StudyGroupRepository studyGroupRepository;
    private final GroupService groupService;
    private final AttendanceService attendanceService;
    private final UserService userService;

    @GetMapping
    public String list(Model model, Principal principal) {
        SiteUser actor = principal != null ? userService.getUser(principal.getName()) : null;
        if (actor == null || !userService.isAdminOrRoot(actor)) {
            return "redirect:/login";
        }
        List<StudyGroup> groups = studyGroupRepository.findAll();
        List<GroupView> data = groups.stream().map(g -> {
            List<GroupMember> members = groupService.membersOf(g);
            List<GroupNotice> notices = groupService.listNotices(g);
            List<GroupTask> tasks = groupService.listTasks(g);
            List<AttendanceRecord> attendance = attendanceService.listForRange(g,
                    java.time.LocalDate.now().minusMonths(1),
                    java.time.LocalDate.now());
            return GroupView.from(g, members, notices, tasks, attendance);
        }).toList();
        model.addAttribute("groups", data);
        return "admin_group_list";
    }

    public record GroupView(
            Long id,
            String name,
            String joinCode,
            int memberCount,
            List<MemberView> members,
            String memberLines,
            int noticeCount,
            int taskCount,
            int attendanceCount,
            String noticeLines,
            String taskLines,
            String attendanceLines
    ) {
        public static GroupView from(StudyGroup g, List<GroupMember> members,
                                     List<GroupNotice> notices,
                                     List<GroupTask> tasks,
                                     List<AttendanceRecord> attendance) {
            String joinedMembers = "";
            if (members != null) {
                joinedMembers = members.stream()
                        .map(m -> {
                            SiteUser u = m.getUser();
                            if (u == null) return "";
                            return (u.getUsername() != null ? u.getUsername() : "") + " "
                                    + (u.getFullName() != null ? u.getFullName() : "") + " "
                                    + (u.getSchoolName() != null ? u.getSchoolName() : "") + " "
                                    + (u.getGrade() != null ? u.getGrade() : "");
                        })
                        .filter(s -> !s.isBlank())
                        .collect(Collectors.joining("||"));
            }
            String joinedNotices = joinNotices(notices);
            String joinedTasks = joinTasks(tasks);
            String joinedAttendance = joinAttendance(attendance);
            return new GroupView(
                    g.getId(),
                    g.getName(),
                    g.getJoinCode(),
                    members != null ? members.size() : 0,
                    members != null ? members.stream().map(MemberView::from).toList() : List.of(),
                    joinedMembers,
                    notices != null ? notices.size() : 0,
                    tasks != null ? tasks.size() : 0,
                    attendance != null ? attendance.size() : 0,
                    joinedNotices,
                    joinedTasks,
                    joinedAttendance
            );
        }

        private static String joinNotices(List<GroupNotice> notices) {
            if (notices == null) return "";
            return notices.stream()
                    .map(n -> {
                        String title = n.getTitle() != null ? n.getTitle() : "";
                        String date = n.getCreatedAt() != null ? n.getCreatedAt().toLocalDate().toString() : "";
                        return (title + " " + date).trim();
                    })
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.joining("||"));
        }

        private static String joinTasks(List<GroupTask> tasks) {
            if (tasks == null) return "";
            return tasks.stream()
                    .map(t -> {
                        String title = t.getTitle() != null ? t.getTitle() : "";
                        String due = t.getDueDate() != null ? t.getDueDate().toString() : "";
                        return (title + " " + due).trim();
                    })
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.joining("||"));
        }

        private static String joinAttendance(List<AttendanceRecord> items) {
            if (items == null) return "";
            return items.stream()
                    .map(a -> {
                        String student = a.getStudent() != null ? a.getStudent().getUsername() : "";
                        String status = a.getStatus() != null ? a.getStatus().name() : "";
                        String date = a.getDate() != null ? a.getDate().toString() : "";
                        return (student + " " + status + " " + date).trim();
                    })
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.joining("||"));
        }
    }

    public record MemberView(Long id, String username, String fullName, String schoolName, String grade) {
        public static MemberView from(GroupMember m) {
            SiteUser u = m.getUser();
            if (u == null) return new MemberView(null, "", "", "", "");
            return new MemberView(u.getId(), u.getUsername(), u.getFullName(), u.getSchoolName(), u.getGrade());
        }
    }
}
