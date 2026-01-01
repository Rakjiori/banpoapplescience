package com.example.sbb.controller;

import com.example.sbb.domain.user.AccountType;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.domain.user.UserService;
import com.example.sbb.dto.AdminUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserApiController {

    private final UserService userService;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ROOT')")
    @GetMapping
    public ResponseEntity<Page<AdminUserResponse>> listUsers(
            @RequestParam(required = false) String school,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) AccountType accountType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        Page<SiteUser> result = userService.searchUsers(school, grade, accountType, pageable);
        Page<AdminUserResponse> mapped = result.map(user -> AdminUserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .schoolName(user.getSchoolName())
                .grade(user.getGrade())
                .accountType(user.getAccountType())
                .role(user.getRole())
                .build());
        return ResponseEntity.ok(mapped);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ROOT')")
    @PostMapping("/{username}/reset-password")
    public ResponseEntity<Void> resetPassword(@PathVariable String username) {
        userService.resetPasswordTo(username, "apple");
        return ResponseEntity.ok().build();
    }
}
