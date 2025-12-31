package com.example.sbb.service;

import com.example.sbb.domain.Announcement;
import com.example.sbb.dto.NoticeDto;
import com.example.sbb.repository.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnnouncementApiService {

    private final AnnouncementRepository announcementRepository;

    @Transactional(readOnly = true)
    public List<NoticeDto> findAllDtos() {
        return announcementRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public NoticeDto findById(Long id) {
        Announcement a = announcementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("공지사항을 찾을 수 없습니다."));
        return toDto(a);
    }

    private NoticeDto toDto(Announcement a) {
        String created = a.getCreatedAt() != null ? a.getCreatedAt().toLocalDate().toString() : null;
        return new NoticeDto(a.getId(), a.getTitle(), a.getContent(), created);
    }
}
