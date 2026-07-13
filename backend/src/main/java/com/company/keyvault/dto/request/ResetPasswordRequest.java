package com.company.keyvault.dto.request;
import jakarta.validation.constraints.*; import lombok.Data;
@Data public class ResetPasswordRequest { @NotBlank private String token; @NotBlank @Size(min=10,message="Password must be at least 10 characters") private String newPassword; }
