package com.example.sbb.dto;

public record ReviewRequest(String author, String highlight, int rating, String content) {
}
