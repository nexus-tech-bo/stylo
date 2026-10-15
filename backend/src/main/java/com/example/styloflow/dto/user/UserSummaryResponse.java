package com.example.styloflow.dto.user;

import com.example.styloflow.enums.Role;

public record UserSummaryResponse(Long id, String name, Role role) {
}
