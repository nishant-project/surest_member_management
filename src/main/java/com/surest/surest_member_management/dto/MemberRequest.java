package com.surest.surest_member_management.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.*;


import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberRequest {


    @NotBlank(message = "first_name is required")
    private String firstName;


    @NotBlank(message = "last_name is required")
    private String lastName;


    @NotNull(message = "date_of_birth is required")
    @Past(message = "date_of_birth must be in the past")
    private LocalDate dateOfBirth;


    @NotBlank(message = "email is required")
    @Email(message = "email must be valid")
    private String email;
}