package com.example.sbb.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
// Gemini 기반 기능 제거됨
// @Service
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class GeminiQuestionService {

    @Value("${gemini.api.key:}")
    private String apiKey;

    // 멀티모달(이미지/PDF) 지원 모델 사용 (예: gemini-1.5-flash, gemini-1.5-pro 등)
    @Value("${gemini.model:gemini-2.5-flash}")
    private String modelName;

    private static final String QUESTION_PROMPT = """
            (Gemini 기능은 현재 학원 홈페이지 모드에서 비활성화되었습니다.)
            """;

    public String generateQuestionsFromMultiplePdfs(List<byte[]> pdfBytesList,
                                                    List<String> originalNames,
                                                    String stylePrompt) {

        return "⚠ 현재 학원 홈페이지 모드에서는 문제 생성 기능이 비활성화되어 있습니다.";
    }

    public String summarizeText(String text, String originalName) {
        return "⚠ 현재 학원 홈페이지 모드에서는 요약 기능이 비활성화되어 있습니다.";
    }

    public String generateQuestionsFromTexts(List<String> texts,
                                             List<String> originalNames,
                                             String stylePrompt) {
        return "⚠ 현재 학원 홈페이지 모드에서는 문제 생성 기능이 비활성화되어 있습니다.";
    }

    // =========================
    // 4) 공통 Gemini 호출 로직
    // =========================
    @SuppressWarnings("rawtypes")
    private String callGemini(Map<String, Object> body) {
        return "⚠ Gemini 기능이 비활성화되어 있습니다.";
    }

    /**
     * Gemini 호출 결과가 실패/에러로 판단되는지 여부
     */
    public boolean isFailure(String result) {
        if (result == null) return true;
        String trimmed = result.trim();
        if (trimmed.isEmpty()) return true;
        return trimmed.startsWith("⚠") || trimmed.toLowerCase().contains("error") || trimmed.toLowerCase().contains("오류");
    }

}
