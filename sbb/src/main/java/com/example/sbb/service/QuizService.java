package com.example.sbb.service;

import com.example.sbb.domain.document.DocumentFile;
import com.example.sbb.domain.quiz.QuizQuestion;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.repository.QuizQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizQuestionRepository quizQuestionRepository;
    private static final Pattern QUESTION_LINE_PATTERN =
            Pattern.compile("^\\s*Q?\\[?(\\d+)\\]?\\s*[\\.:\\)]?\\s*(\\d+\\)\\s+)?(.+)$");
    private static final Pattern ANSWER_LINE_PATTERN =
            Pattern.compile("^\\s*\\[?정답]?\\s*[:\\-\\.\\)]?\\s*(.*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXPLANATION_LINE_PATTERN =
            Pattern.compile("^\\s*\\[?해설]?\\s*[:\\-\\.\\)]?\\s*(.*)$", Pattern.CASE_INSENSITIVE);

    /**
     * Gemini가 반환한 전체 텍스트를 파싱해서
     * QuizQuestion 엔티티로 저장
     */
    public List<QuizQuestion> saveFromRawText(String rawText,
                                              SiteUser user,
                                              List<DocumentFile> sourceDocs,
                                              com.example.sbb.domain.Folder defaultFolder) {

        List<QuizQuestion> parsed = new ArrayList<>();

        if (rawText == null || rawText.isBlank()) {
            return parsed;
        }

        String cleaned = rawText.replace("**", "").replace("\r", "\n");
        // 구분자 기반 응답을 Qn: 형태로 치환
        cleaned = cleaned.replaceAll("<<<END>>>", "\n\n");
        cleaned = cleaned.replaceAll("<<<Q(\\d+)>>>", "\nQ$1:");
        // 강제로 질문 시작(Qn:) 앞에 줄바꿈 삽입
        cleaned = cleaned.replaceAll("(?<!\\n)(Q\\d+[:\\.])", "\n$1");
        String[] lines = cleaned.split("\\R"); // 개행 기준 split
        QuizQuestion current = null;
        StringBuilder questionBuffer = new StringBuilder();
        StringBuilder choiceBuffer = new StringBuilder();
        boolean readingChoices = false;

        for (String line : lines) {
            String trimmed = line.trim();

            if (readingChoices &&
                    !trimmed.startsWith("[정답]") &&
                    !trimmed.toLowerCase().startsWith("정답") &&
                    !trimmed.startsWith("[해설]") &&
                    !trimmed.toLowerCase().startsWith("해설")) {
                if (current != null && !trimmed.isEmpty()) {
                    choiceBuffer.append(trimmed);
                    if (!trimmed.endsWith("\n")) {
                        choiceBuffer.append("\n");
                    }
                }
                continue;
            }

            // "(보기)" 없이 바로 "1) ..." 이 나오면 선택지로 처리
            if (current != null && !readingChoices && trimmed.matches("^\\d+[).]\\s+.+$")) {
                current.setMultipleChoice(true);
                choiceBuffer.setLength(0);
                choiceBuffer.append(trimmed);
                if (!trimmed.endsWith("\n")) choiceBuffer.append("\n");
                readingChoices = true;
                continue;
            }

            if (trimmed.equalsIgnoreCase("보기:")) {
                if (current != null) {
                    current.setMultipleChoice(true);
                    choiceBuffer.setLength(0);
                    readingChoices = true;
                }
            } else if (trimmed.startsWith("(보기)")) {
                if (current != null) {
                    current.setMultipleChoice(true);
                    choiceBuffer.setLength(0);
                    String choicePart = trimmed.replaceFirst("^\\(보기\\)\\s*", "");
                    choiceBuffer.append(choicePart);
                    if (!choicePart.endsWith("\n")) {
                        choiceBuffer.append("\n");
                    }
                    readingChoices = true;
                }
            } else if (ANSWER_LINE_PATTERN.matcher(trimmed).matches() || trimmed.toLowerCase().startsWith("정답")) {
                if (current != null) {
                    if (choiceBuffer.length() > 0) {
                        current.setMultipleChoice(true);
                        current.setChoices(choiceBuffer.toString().trim());
                    }
                    Matcher ansMatcher = ANSWER_LINE_PATTERN.matcher(trimmed);
                    if (ansMatcher.find()) {
                        String ans = ansMatcher.group(1).trim();
                        current.setAnswer(ans);
                    } else {
                        String ans = trimmed.replaceFirst("(?i)^정답\\s*[:\\-\\.]?\\s*", "").trim();
                        current.setAnswer(ans);
                    }
                }
                readingChoices = false;
            } else if (EXPLANATION_LINE_PATTERN.matcher(trimmed).matches() || trimmed.toLowerCase().startsWith("해설")) {
                if (current != null) {
                    if (choiceBuffer.length() > 0 && current.getChoices() == null) {
                        current.setMultipleChoice(true);
                        current.setChoices(choiceBuffer.toString().trim());
                    }
                    Matcher expMatcher = EXPLANATION_LINE_PATTERN.matcher(trimmed);
                    if (expMatcher.find()) {
                        current.setExplanation(expMatcher.group(1).trim());
                    } else {
                        current.setExplanation(trimmed.replaceFirst("(?i)^해설\\s*[:\\-\\.]?\\s*", "").trim());
                    }
                }
                readingChoices = false;
            } else {
                Matcher matcher = QUESTION_LINE_PATTERN.matcher(trimmed);
                if (matcher.matches()) {
                    if (current != null) {
                        finalizeQuestion(current, questionBuffer, choiceBuffer, parsed);
                    }

                    current = new QuizQuestion();
                    current.setUser(user);

                    if (sourceDocs != null && !sourceDocs.isEmpty()) {
                        DocumentFile firstDoc = sourceDocs.get(0);
                        current.setDocument(firstDoc);
                        current.setFolder(firstDoc.getFolder());
                    } else if (defaultFolder != null) {
                        current.setFolder(defaultFolder);
                    }

                    questionBuffer.setLength(0);

                    String numStr = matcher.group(1);
                    try {
                        current.setNumberTag(Integer.parseInt(numStr));
                    } catch (NumberFormatException e) {
                        current.setNumberTag(null);
                    }

                    // group(2)는 "1) "처럼 잘못 붙은 선택지 숫자 제거
                    String afterNumber = matcher.group(3);
                    if (afterNumber == null) afterNumber = "";
                    readingChoices = false;
                    choiceBuffer.setLength(0);
                    questionBuffer.append(afterNumber).append(" ");

                } else if (current != null) {
                    questionBuffer.append(trimmed).append(" ");
                } else if (!trimmed.isEmpty()) {
                    // 번호가 없더라도 새 질문으로 시작
                    current = new QuizQuestion();
                    current.setUser(user);
                    if (sourceDocs != null && !sourceDocs.isEmpty()) {
                        DocumentFile firstDoc = sourceDocs.get(0);
                        current.setDocument(firstDoc);
                        current.setFolder(firstDoc.getFolder());
                    } else if (defaultFolder != null) {
                        current.setFolder(defaultFolder);
                    }
                    current.setNumberTag(parsed.size() + 1);
                    questionBuffer.setLength(0);
                    choiceBuffer.setLength(0);
                    readingChoices = false;
                    questionBuffer.append(trimmed).append(" ");
                }
            }
        }

        // 마지막 문제 마무리
        if (current != null) {
            finalizeQuestion(current, questionBuffer, choiceBuffer, parsed);
        }

        // ✅ 무조건 6개 객관식 + 4개 단답형(주관식)만 저장
        List<QuizQuestion> normalized = normalizeCount(parsed);
        normalized.forEach(quizQuestionRepository::save);
        return normalized;
    }

    private void finalizeQuestion(QuizQuestion current,
                                  StringBuilder questionBuffer,
                                  StringBuilder choiceBuffer,
                                  List<QuizQuestion> result) {
        String questionText = questionBuffer.toString().trim();
        if (questionText.isBlank()) {
            questionBuffer.setLength(0);
            choiceBuffer.setLength(0);
            return;
        }
        current.setQuestionText(questionText);
        if (choiceBuffer.length() > 0 && (current.getChoices() == null || current.getChoices().isBlank())) {
            current.setMultipleChoice(true);
            current.setChoices(choiceBuffer.toString().trim());
        }
        result.add(current);
        questionBuffer.setLength(0);
        choiceBuffer.setLength(0);
    }

    /**
     * 결과를 모두 객관식으로 제한하고 최대 10개만 남긴다.
     */
    private List<QuizQuestion> normalizeCount(List<QuizQuestion> parsed) {
        List<QuizQuestion> result = new ArrayList<>();
        for (QuizQuestion q : parsed) {
            // 객관식만 유지 (보기 없으면 건너뜀)
            if (q.getChoices() == null || q.getChoices().isBlank()) continue;
            // 보기 4개까지만 허용
            String[] choiceLines = q.getChoices().split("\\R");
            List<String> filtered = new ArrayList<>();
            for (String line : choiceLines) {
                String t = line.trim();
                if (t.isBlank()) continue;
                if (filtered.size() >= 4) break;
                filtered.add(t);
            }
            if (filtered.size() < 2) continue; // 보기 최소 2개 미만이면 스킵
            q.setChoices(String.join("\n", filtered));
            q.setMultipleChoice(true);
            result.add(q);
            if (result.size() >= 10) break;
        }
        return result;
    }
}
