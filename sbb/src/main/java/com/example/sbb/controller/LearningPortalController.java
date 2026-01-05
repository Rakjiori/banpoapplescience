package com.example.sbb.controller;

import com.example.sbb.domain.group.GroupMember;
import com.example.sbb.domain.group.GroupNotice;
import com.example.sbb.domain.group.GroupTask;
import com.example.sbb.domain.group.StudyGroup;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.domain.user.UserService;
import com.example.sbb.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class LearningPortalController {

    private final UserService userService;
    private final GroupService groupService;

    @GetMapping("/learning")
    public String portal(Model model, Principal principal, RedirectAttributes rttr) {
        if (principal == null) return "redirect:/login";
        SiteUser user = userService.getUser(principal.getName());
        List<GroupMember> memberships = groupService.memberships(user);

        Map<Long, PortalTaskSection> taskSections = new LinkedHashMap<>();
        Map<Long, PortalNoticeSection> noticeSections = new LinkedHashMap<>();
        int taskCount = 0;
        int noticeCount = 0;

        for (GroupMember membership : memberships) {
            StudyGroup group = membership.getGroup();
            if (group == null) continue;
            var tasks = new ArrayList<>(groupService.listTasks(group));
            tasks.sort(Comparator.comparing((GroupTask t) -> t.getDueDate() == null ? java.time.LocalDate.MAX : t.getDueDate())
                    .thenComparing(GroupTask::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
            taskSections.put(group.getId(), new PortalTaskSection(group, tasks));
            taskCount += tasks.size();

            var notices = new ArrayList<>(groupService.listNotices(group));
            notices.sort(Comparator.comparing(GroupNotice::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
            noticeSections.put(group.getId(), new PortalNoticeSection(group, notices));
            noticeCount += notices.size();
        }

        model.addAttribute("memberships", memberships);
        model.addAttribute("taskSections", taskSections.values());
        model.addAttribute("noticeSections", noticeSections.values());
        model.addAttribute("taskCount", taskCount);
        model.addAttribute("noticeCount", noticeCount);
        return "learning_portal";
    }

    public record PortalTaskSection(StudyGroup group, List<GroupTask> tasks) {}
    public record PortalNoticeSection(StudyGroup group, List<GroupNotice> notices) {}
}
