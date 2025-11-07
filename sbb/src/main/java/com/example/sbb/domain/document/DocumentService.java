package com.example.sbb.domain.document;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class DocumentService {
  public String extractText(String path) throws Exception {
    try (PDDocument doc = PDDocument.load(new File(path))) {
      return new PDFTextStripper().getText(doc);
    }
  }
}
