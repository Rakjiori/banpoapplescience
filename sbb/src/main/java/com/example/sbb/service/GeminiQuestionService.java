package com.example.sbb.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class GeminiQuestionService {

    @Value("${gemini.api.key:}")
    private String apiKey;

    // 멀티모달(이미지/PDF) 지원 모델 사용 (예: gemini-1.5-flash, gemini-1.5-pro 등)
    @Value("${gemini.model:gemini-2.5-flash}")
    private String modelName;

    // 간단히 내부에서 생성 (원하면 @Bean으로 주입해도 됨)
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String QUESTION_PROMPT = """
            너는 대학 강의자료나 교재 텍스트를 기반으로 학습용 객관식 문제를 만들어주는 도우미야.

            반드시 10문제를 만들어줘 (Q1~Q10). 모든 문제는 4지선다 객관식이어야 해.

            아래 구분자와 형식을 정확히 사용해 출력해줘. 다른 형식(마크다운, 불릿, 표, 굵게 표시 등)은 절대 쓰지 말 것.

            <<<Q{번호}>>>
            Q{번호}: 문제 내용
            보기:
            1) ...
            2) ...
            3) ...
            4) ...
            정답: 보기 번호만 숫자로 (예: 2)
            해설: 한두 문장 이유 설명
            <<<END>>>

            모든 문항 사이에는 반드시 <<<END>>> 다음에 줄바꿈을 한 번 넣어 구분해.
            <<<Q...>>> 와 <<<END>>> 구분자는 문제 시작/끝에만 사용하고, 그 외에는 사용하지 마.
            """;
   
    public String generateQuestionsFromMultiplePdfs(List<byte[]> pdfBytesList,
                                                    List<String> originalNames,
                                                    String stylePrompt) {

        if (apiKey == null || apiKey.isBlank()) {
            return "⚠ Gemini API 키가 설정되지 않았습니다.";
        }

        if (pdfBytesList == null || pdfBytesList.isEmpty()) {
            return "⚠ 전달된 PDF가 없습니다.";
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + modelName + ":generateContent?key=" + apiKey;

        StringBuilder titleBuilder = new StringBuilder();
        if (originalNames != null && !originalNames.isEmpty()) {
            titleBuilder.append("다음 ").append(originalNames.size()).append("개의 문서를 기반으로 문제를 만들어줘.\n");
            for (int i = 0; i < originalNames.size(); i++) {
                titleBuilder.append("- ").append(originalNames.get(i)).append("\n");
            }
            titleBuilder.append("\n");
        }

        StringBuilder promptBuilder = new StringBuilder(QUESTION_PROMPT).append("\n").append(titleBuilder);
        if (stylePrompt != null && !stylePrompt.isBlank()) {
            promptBuilder
                    .append("\n사용자가 원하는 문제 스타일/중점 사항:\n")
                    .append(stylePrompt)
                    .append("\n가능한 한 위 요구사항을 반영해서 문제를 만들어줘.\n");
        }

        // 1) parts 리스트 구성: 첫 번째 part는 텍스트 prompt
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", promptBuilder.toString()));

        // 2) 각 PDF를 inlineData로 추가
        for (byte[] pdfBytes : pdfBytesList) {
            if (pdfBytes == null || pdfBytes.length == 0) continue;

            String base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);
            parts.add(
                    Map.of(
                            "inlineData", Map.of(
                                    "mimeType", "application/pdf",
                                    "data", base64Pdf
                            )
                    )
            );
        }

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", parts
                        )
                )
        );

        return callGemini(body);
    }

    public String summarizeText(String text, String originalName) {
        if (text == null || text.isBlank()) {
            return "⚠ 요약할 텍스트가 없습니다.";
        }

        String titlePart = (originalName != null && !originalName.isBlank())
                ? "자료 제목: " + originalName + "\n\n"
                : "";

        String prompt ="""
                너는 대학 강의자료나 PDF 텍스트를 읽고 핵심을 요약하는 조교야.
                아래 텍스트를 5줄 이내의 불릿으로 정리해줘.
                - 가장 중요한 개념/정의/수치 중심으로 간결하게
                - 불필요한 수식어나 예시는 제거
                - 한국어로 자연스럽게 작성
                """;

        String fullPrompt = prompt + "\n" + titlePart + "텍스트:\n" + text;

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", List.of(
                                        Map.of("text", fullPrompt)
                                )
                        )
                )
        );

        return callGemini(body);
    }

    public String generateQuestionsFromTexts(List<String> texts,
                                             List<String> originalNames,
                                             String stylePrompt) {
        if (apiKey == null || apiKey.isBlank()) {
            return "⚠ Gemini API 키가 설정되지 않았습니다.";
        }
        if (texts == null || texts.isEmpty()) {
            return "⚠ 전달된 텍스트가 없습니다.";
        }

        StringBuilder textBundle = new StringBuilder();
        for (int i = 0; i < texts.size(); i++) {
            String label = (originalNames != null && i < originalNames.size())
                    ? originalNames.get(i)
                    : "자료 " + (i + 1);
            textBundle.append(label).append(":\n")
                    .append(texts.get(i)).append("\n\n");
        }

        StringBuilder prompt = new StringBuilder(QUESTION_PROMPT).append("\n");

        if (stylePrompt != null && !stylePrompt.isBlank()) {
            prompt.append("사용자 요청 스타일:\n").append(stylePrompt).append("\n\n");
        }
        prompt.append("아래 텍스트 전체를 분석해서 문제를 만들어줘:\n");
        prompt.append(textBundle);

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", List.of(
                                        Map.of("text", prompt.toString())
                                )
                        )
                )
        );

        return callGemini(body);
    }

    // =========================
    // 4) 공통 Gemini 호출 로직
    // =========================
    @SuppressWarnings("rawtypes")
    private String callGemini(Map<String, Object> body) {

        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + modelName + ":generateContent?key=" + apiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            Map data = response.getBody();
            if (data == null) {
                return "⚠ Gemini 응답이 비어 있습니다.";
            }

            List candidates = (List) data.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                return "⚠ Gemini에서 후보 응답을 받지 못했습니다.";
            }

            Map first = (Map) candidates.get(0);
            Map content = (Map) first.get("content");
            if (content == null) {
                return "⚠ Gemini 응답 형식이 예상과 다릅니다.(content 없음)";
            }

            List partsResp = (List) content.get("parts");
            if (partsResp == null || partsResp.isEmpty()) {
                return "⚠ Gemini 응답 형식이 예상과 다릅니다.(parts 없음)";
            }

            Map firstPart = (Map) partsResp.get(0);
            Object textObj = firstPart.get("text");
            if (textObj == null) {
                return "⚠ Gemini 응답에 text가 없습니다.";
            }

            return textObj.toString();

        } catch (Exception e) {
            return "⚠ Gemini 오류: " + e.getMessage();
        }
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
