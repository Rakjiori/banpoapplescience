package com.example.sbb.service;

import com.example.sbb.domain.CourseReview;
import com.example.sbb.dto.ReviewDto;
import com.example.sbb.dto.ReviewRequest;
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
    public ReviewDto create(ReviewRequest request) {
        if (request == null) return null;
        CourseReview review = new CourseReview();
        applyRequest(review, request);
        CourseReview saved = courseReviewRepository.save(review);
        return toDto(saved);
    }

    @Transactional
    public ReviewDto update(Long id, ReviewRequest request) {
        if (id == null || request == null) return null;
        CourseReview review = courseReviewRepository.findById(id).orElse(null);
        if (review == null) {
            return null;
        }
        applyRequest(review, request);
        CourseReview saved = courseReviewRepository.save(review);
        return toDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (id == null) return;
        if (courseReviewRepository.existsById(id)) {
            courseReviewRepository.deleteById(id);
        }
    }

    private void applyRequest(CourseReview review, ReviewRequest request) {
        review.setAuthor(request.author());
        review.setRating(request.rating());
        review.setContent(request.content());
        review.setHighlight(request.highlight());
    }

    private ReviewDto toDto(CourseReview r) {
        String created = r.getCreatedAt() != null ? r.getCreatedAt().toLocalDate().toString() : null;
        return new ReviewDto(r.getId(), r.getAuthor(), r.getRating(), r.getContent(), created);
    }
}
