package com.company.keyvault.dto.request;
import com.company.keyvault.model.enums.UserRole; import lombok.Data;
@Data public class UserUpdateRequest { private UserRole role; private Boolean active; }
