package com.example.sbb.controller;

import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.domain.user.UserService;
import com.example.sbb.dto.GroupDto;
import com.example.sbb.dto.GroupNoticeDto;
import com.example.sbb.dto.GroupTaskDto;
import com.example.sbb.service.GroupApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupApiController {

    private final GroupApiService groupApiService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<GroupDto>> myGroups(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        SiteUser user = userService.getUser(principal.getName());
        return ResponseEntity.ok(groupApiService.myGroups(user));
    }

    @GetMapping("/{groupId}/notices")
    public ResponseEntity<List<GroupNoticeDto>> notices(@PathVariable Long groupId, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        SiteUser user = userService.getUser(principal.getName());
        return ResponseEntity.ok(groupApiService.notices(groupId, user));
    }

    @GetMapping("/{groupId}/tasks")
    public ResponseEntity<List<GroupTaskDto>> tasks(@PathVariable Long groupId, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        SiteUser user = userService.getUser(principal.getName());
        return ResponseEntity.ok(groupApiService.tasks(groupId, user));
    }
}
