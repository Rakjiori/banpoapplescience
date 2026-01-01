package com.example.sbb.service;

import com.example.sbb.domain.CourseReview;
import com.example.sbb.dto.ReviewDto;
import com.example.sbb.repository.CourseReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewApiService {

    private final CourseReviewRepository courseReviewRepository;

    @Transactional(readOnly = true)
    public List<ReviewDto> findAllDtos() {
        return courseReviewRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void delete(Long id) {
        if (id == null) return;
        if (courseReviewRepository.existsById(id)) {
            courseReviewRepository.deleteById(id);
        }
    }

    private ReviewDto toDto(CourseReview r) {
        String created = r.getCreatedAt() != null ? r.getCreatedAt().toLocalDate().toString() : null;
        return new ReviewDto(r.getId(), r.getAuthor(), r.getRating(), r.getContent(), created);
    }
}
