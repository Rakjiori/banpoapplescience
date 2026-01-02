package com.example.sbb.controller;

import com.example.sbb.dto.ReviewDto;
import com.example.sbb.dto.ReviewRequest;
import com.example.sbb.service.ReviewApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewApiController {

    private final ReviewApiService reviewApiService;

    @GetMapping
    public List<ReviewDto> list() {
        return reviewApiService.findAllDtos();
    }

    @PostMapping
    public ReviewDto create(@RequestBody ReviewRequest request) {
        return reviewApiService.create(request);
    }

    @PutMapping("/{id}")
    public ReviewDto update(@PathVariable Long id, @RequestBody ReviewRequest request) {
        return reviewApiService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        reviewApiService.delete(id);
    }
}
