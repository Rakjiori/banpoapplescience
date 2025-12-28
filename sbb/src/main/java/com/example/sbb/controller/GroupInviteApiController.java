package com.example.sbb.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups/invite")
@RequiredArgsConstructor
public class GroupInviteApiController {

    /**
     * 그룹 초대 기능을 사용하지 않으므로 빈 응답을 반환합니다.
     */
    @GetMapping("/inbox")
    public ResponseEntity<?> inbox() {
        return ResponseEntity.ok(java.util.Map.of("received", List.of(), "sent", List.of()));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<?> accept() {
        return ResponseEntity.status(410).body("group invite disabled");
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> reject() {
        return ResponseEntity.status(410).body("group invite disabled");
    }
}
