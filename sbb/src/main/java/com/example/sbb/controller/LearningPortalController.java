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
import java.util.List;

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

        List<PortalTask> tasks = new ArrayList<>();
        List<PortalNotice> notices = new ArrayList<>();
        for (GroupMember membership : memberships) {
            StudyGroup group = membership.getGroup();
            if (group == null) continue;
            groupService.listTasks(group).forEach(task -> tasks.add(new PortalTask(group, task)));
            groupService.listNotices(group).forEach(notice -> notices.add(new PortalNotice(group, notice)));
        }

        tasks.sort(Comparator.comparing((PortalTask t) -> t.task().getDueDate() == null ? java.time.LocalDate.MAX : t.task().getDueDate())
                .thenComparing(t -> t.task().getCreatedAt(), Comparator.nullsLast(Comparator.reverseOrder())));
        notices.sort(Comparator.comparing((PortalNotice n) -> n.notice().getCreatedAt(), Comparator.nullsLast(Comparator.reverseOrder())));

        model.addAttribute("memberships", memberships);
        model.addAttribute("portalTasks", tasks);
        model.addAttribute("portalNotices", notices);
        return "learning_portal";
    }

    public record PortalTask(StudyGroup group, GroupTask task) {}
    public record PortalNotice(StudyGroup group, GroupNotice notice) {}
}
