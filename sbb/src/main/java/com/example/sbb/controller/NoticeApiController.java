package com.example.sbb.controller;

import com.example.sbb.dto.NoticeDto;
import com.example.sbb.service.AnnouncementApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeApiController {

    private final AnnouncementApiService announcementApiService;

    @GetMapping
    public List<NoticeDto> list() {
        return announcementApiService.findAllDtos();
    }

    @GetMapping("/{id}")
    public NoticeDto detail(@PathVariable Long id) {
        return announcementApiService.findById(id);
    }
}
