package com.example.sbb.domain.document;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Document {
  private String filename;
  private String originalName;
  private String extracted; // DB 붙이면 엔티티 컬럼으로 이동
}
