package com.example.sbb.service;

import com.example.sbb.domain.Announcement;
import com.example.sbb.dto.NoticeDto;
import com.example.sbb.dto.NoticeRequest;
import com.example.sbb.repository.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDate;

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

    @Transactional
    public NoticeDto create(NoticeRequest request) {
        if (request == null || request.title() == null || request.title().isBlank()) {
            throw new IllegalArgumentException("제목은 필수입니다.");
        }
        if (request.content() == null || request.content().isBlank()) {
            throw new IllegalArgumentException("내용은 필수입니다.");
        }
        Announcement a = new Announcement();
        a.setTitle(request.title().trim());
        a.setContent(request.content().trim());
        if (request.publishedAt() != null && !request.publishedAt().isBlank()) {
            a.setPublishedAt(LocalDate.parse(request.publishedAt().trim()));
        }
        return toDto(announcementRepository.save(a));
    }

    @Transactional
    public NoticeDto update(Long id, NoticeRequest request) {
        Announcement a = announcementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("공지사항을 찾을 수 없습니다."));
        if (request.title() != null && !request.title().isBlank()) {
            a.setTitle(request.title().trim());
        }
        if (request.content() != null && !request.content().isBlank()) {
            a.setContent(request.content().trim());
        }
        if (request.publishedAt() != null && !request.publishedAt().isBlank()) {
            a.setPublishedAt(LocalDate.parse(request.publishedAt().trim()));
        }
        return toDto(announcementRepository.save(a));
    }

    @Transactional
    public void delete(Long id) {
        if (id == null) return;
        if (!announcementRepository.existsById(id)) return;
        announcementRepository.deleteById(id);
    }

    private NoticeDto toDto(Announcement a) {
        String created = a.getCreatedAt() != null ? a.getCreatedAt().toLocalDate().toString() : null;
        return new NoticeDto(a.getId(), a.getTitle(), a.getContent(), created);
    }
}
