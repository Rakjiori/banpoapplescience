package com.example.sbb.dto;

import com.example.sbb.domain.user.AccountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {
    private Long id;
    private String username;
    private String fullName;
    private String schoolName;
    private String grade;
    private AccountType accountType;
    private String role;
}
