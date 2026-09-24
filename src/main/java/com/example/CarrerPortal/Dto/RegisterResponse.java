package com.example.CarrerPortal.Dto;

import com.example.CarrerPortal.Model.Role;

import lombok.*;

@Getter
@Setter
@Builder
public class RegisterResponse {
	
	private String name;
    private String email;
    private String phone;
    private Role role;

}
