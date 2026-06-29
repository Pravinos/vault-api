package com.vfa.vault.dto;

public class AuthDTO {

    public record PasswordRequest(String password) {
    }

    public record ResetPasswordRequest(String newPassword) {
    }

    public record ChangePasswordRequest(String currentPassword, String newPassword) {
    }

    private AuthDTO() {
    }
}
