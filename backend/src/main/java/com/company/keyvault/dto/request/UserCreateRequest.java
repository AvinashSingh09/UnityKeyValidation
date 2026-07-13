package com.company.keyvault.dto.request;
import com.company.keyvault.model.enums.UserRole; import jakarta.validation.constraints.*; import lombok.Data;
@Data public class UserCreateRequest { @NotBlank private String fullName; @NotBlank @Email private String email; @NotBlank @Size(min=10,message="Password must be at least 10 characters") private String password; @NotNull private UserRole role; }
