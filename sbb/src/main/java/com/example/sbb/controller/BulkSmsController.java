package com.example.sbb.controller;

import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class BulkSmsController {

    private final UserService userService;

    @GetMapping("/sms/bulk")
    public String page(@RequestParam(required = false) String school,
                       @RequestParam(required = false) String grade,
                       Model model) {
        model.addAttribute("school", school);
        model.addAttribute("grade", grade);
        model.addAttribute("schools", userService.getAllowedSchools());
        model.addAttribute("grades", userService.getAllowedGrades());
        List<SiteUser> rows = userService.findParentsForBulkSms(school, grade);
        model.addAttribute("rows", rows);
        model.addAttribute("resultCount", rows.size());
        return "sms_bulk";
    }

    @GetMapping("/sms/bulk/export")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String school,
                                         @RequestParam(required = false) String grade) {
        List<SiteUser> rows = userService.findParentsForBulkSms(school, grade);
        StringBuilder sb = new StringBuilder();
        // UTF-8 BOM + Excel delimiter hint
        sb.append('\uFEFF');
        sb.append("sep=,\n");
        sb.append("학생이름,학부모전화번호\n");
        for (SiteUser u : rows) {
            String name = safe(u.getFullName());
            String phone = safe(u.getParentPhone());
            // ="010-1234-5678" 형태로 내려서 Excel이 숫자로 변환하지 않도록 처리
            String excelPhone = phone.isEmpty() ? "" : "=\"" + phone + "\"";
            sb.append('"').append(name).append('"').append(',');
            sb.append('"').append(excelPhone).append('"').append('\n');
        }
        byte[] data = sb.toString().getBytes(StandardCharsets.UTF_8);
        String filename = "parent-phones.csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(data);
    }

    private String safe(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }
}
