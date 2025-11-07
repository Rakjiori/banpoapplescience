package com.example.sbb.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Controller
@RequestMapping("/document")
public class UploadController {
  private static final String DIR = "uploads";

  @GetMapping("/upload")
  public String form() { return "document_upload"; }

  @PostMapping("/upload")
  public String upload(@RequestParam("pdfFile") MultipartFile file) throws IOException {
    File dir = new File(DIR); if(!dir.exists()) dir.mkdirs();
    String name = System.currentTimeMillis() + "_" + file.getOriginalFilename();
    file.transferTo(new File(dir, name));
    return "redirect:/question/list"; // 업로드 후 목록으로 (지금은 더미)
  }
}
