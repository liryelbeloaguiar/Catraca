package com.queueflow.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record RegisterPatient(@NotBlank String fullName, @Email @NotBlank String email,
                              @Size(min = 8, max = 72) String password, @NotBlank String document,
                              @Past LocalDate birthDate, String phone) {}
