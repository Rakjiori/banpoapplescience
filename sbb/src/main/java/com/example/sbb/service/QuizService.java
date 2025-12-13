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
            Pattern.compile("^\\s*\\[?(\\d+)\\]?\\s*[\\.:\\)\\-]?\\s*(.*)$");
    private static final Pattern ANSWER_LINE_PATTERN =
            Pattern.compile("^\\s*\\[?정답]?\\s*[:\\-\\.]?\\s*(.*)$", Pattern.CASE_INSENSITIVE);

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

        String[] lines = rawText.split("\\R"); // 개행 기준 split
        QuizQuestion current = null;
        StringBuilder questionBuffer = new StringBuilder();
        StringBuilder choiceBuffer = new StringBuilder();
        boolean readingChoices = false;

        for (String line : lines) {
            String trimmed = line.trim();

            if (readingChoices &&
                    !trimmed.startsWith("[정답]") &&
                    !trimmed.startsWith("[해설]")) {
                if (current != null && !trimmed.isEmpty()) {
                    choiceBuffer.append(trimmed);
                    if (!trimmed.endsWith("\n")) {
                        choiceBuffer.append("\n");
                    }
                }
                continue;
            }

            if (trimmed.startsWith("(보기)")) {
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
            } else if (ANSWER_LINE_PATTERN.matcher(trimmed).matches()) {
                if (current != null) {
                    if (choiceBuffer.length() > 0) {
                        current.setMultipleChoice(true);
                        current.setChoices(choiceBuffer.toString().trim());
                    }
                    Matcher ansMatcher = ANSWER_LINE_PATTERN.matcher(trimmed);
                    if (ansMatcher.find()) {
                        String ans = ansMatcher.group(1).trim();
                        current.setAnswer(ans);
                    }
                }
                readingChoices = false;
            } else if (trimmed.startsWith("[해설]")) {
                if (current != null) {
                    if (choiceBuffer.length() > 0 && current.getChoices() == null) {
                        current.setMultipleChoice(true);
                        current.setChoices(choiceBuffer.toString().trim());
                    }
                    String exp = trimmed.replaceFirst("^\\[해설]\\s*", "");
                    current.setExplanation(exp);
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

                    String afterNumber = matcher.group(2);
                    readingChoices = false;
                    choiceBuffer.setLength(0);
                    questionBuffer.append(afterNumber).append(" ");

                } else if (current != null) {
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
     * 결과를 6개 객관식 + 4개 단답형으로 맞춰 반환한다.
     * 부족한 단답형은 남는 객관식을 단답형으로 변환해서 채운다.
     */
    private List<QuizQuestion> normalizeCount(List<QuizQuestion> parsed) {
        List<QuizQuestion> mc = new ArrayList<>();
        List<QuizQuestion> shortAns = new ArrayList<>();
        for (QuizQuestion q : parsed) {
            if (q.isMultipleChoice()) mc.add(q);
            else shortAns.add(q);
        }

        List<QuizQuestion> result = new ArrayList<>();

        // 6개 객관식 우선
        for (QuizQuestion q : mc) {
            if (result.size() >= 6) break;
            result.add(q);
        }

        // 단답형 확보
        int shortAdded = 0;
        for (QuizQuestion q : shortAns) {
            if (shortAdded >= 4 || result.size() >= 10) break;
            result.add(q);
            shortAdded++;
        }

        // 단답형 부족분을 남은 객관식으로 변환
        int shortNeeded = 4 - shortAdded;
        if (shortNeeded > 0) {
            for (QuizQuestion q : mc) {
                if (result.contains(q)) continue;
                if (shortNeeded <= 0) break;
                q.setMultipleChoice(false);
                q.setChoices(null);
                result.add(q);
                shortNeeded--;
                shortAdded++;
            }
        }

        // 만약 여전히 부족하면 남은 주관식/객관식에서 추가하되 총 10개로 제한
        if (result.size() < 10) {
            for (QuizQuestion q : parsed) {
                if (result.contains(q)) continue;
                result.add(q);
                if (result.size() >= 10) break;
            }
        }

        // 최종 10개로 슬라이스
        if (result.size() > 10) {
            result = new ArrayList<>(result.subList(0, 10));
        }

        // 단답형이 4개 미만일 때 추가 변환 (안전망)
        long shortCount = result.stream().filter(q -> !q.isMultipleChoice()).count();
        if (shortCount < 4) {
            for (QuizQuestion q : result) {
                if (shortCount >= 4) break;
                if (q.isMultipleChoice()) {
                    q.setMultipleChoice(false);
                    q.setChoices(null);
                    shortCount++;
                }
            }
        }

        return result;
    }
}
