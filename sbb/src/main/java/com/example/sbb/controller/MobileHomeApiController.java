package com.example.sbb.controller;

import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.domain.user.UserService;
import com.example.sbb.dto.GroupDto;
import com.example.sbb.dto.HomeFeedResponse;
import com.example.sbb.service.AnnouncementApiService;
import com.example.sbb.service.GroupApiService;
import com.example.sbb.service.ReviewApiService;
import com.example.sbb.service.ScheduleApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/mobile")
@RequiredArgsConstructor
public class MobileHomeApiController {

    private final AnnouncementApiService announcementApiService;
    private final ReviewApiService reviewApiService;
    private final ScheduleApiService scheduleApiService;
    private final GroupApiService groupApiService;
    private final UserService userService;

    /**
     * 홈 탭에서 한 번에 쓸 기본 데이터 번들.
     * 로그인되어 있으면 내 그룹까지 포함, 아니면 비워둠.
     */
    @GetMapping("/home")
    public ResponseEntity<HomeFeedResponse> home(Principal principal) {
        List<GroupDto> groups = List.of();
        if (principal != null) {
            SiteUser user = userService.getUser(principal.getName());
            groups = groupApiService.myGroups(user);
        }

        var body = new HomeFeedResponse(
                announcementApiService.findAllDtos(),
                reviewApiService.findAllDtos(),
                scheduleApiService.findAllSlots(),
                groups
        );
        return ResponseEntity.ok(body);
    }
}
