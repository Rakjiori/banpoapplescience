package com.example.sbb.domain.question;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionService {
  // DB 붙이기 전까지는 더미 데이터 제공 (SBB도 초반에 더미로 진행)
  public List<Question> getDummyList() {
    return List.of(
      Question.builder().title("광합성의 주요 기관은?").answer("엽록체").build(),
      Question.builder().title("세포 호흡의 최종 산물은?").answer("이산화탄소").build()
    );
  }
}
