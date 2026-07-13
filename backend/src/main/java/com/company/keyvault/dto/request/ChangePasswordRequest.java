package com.company.keyvault.dto.request;
import jakarta.validation.constraints.*; import lombok.Data;
@Data public class ChangePasswordRequest { @NotBlank private String currentPassword; @NotBlank @Size(min=10,message="New password must be at least 10 characters") private String newPassword; }
