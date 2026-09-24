package com.example.CarrerPortal.Dto;

import com.example.CarrerPortal.Model.Role;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {
    private String name;
    private String email;
    private String password;
    private String phone;
    private Role role;

}