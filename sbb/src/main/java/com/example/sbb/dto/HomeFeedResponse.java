package com.example.sbb.dto;

import java.util.List;

/**
 * 앱 홈 화면에서 한 번에 필요한 데이터를 묶어서 내려주는 응답.
 */
public record HomeFeedResponse(
        List<NoticeDto> notices,
        List<ReviewDto> reviews,
        List<ScheduleSlotDto> schedules,
        List<GroupDto> groups
) {}
