package com.mans.ecommerce.b2c.controller.utill.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.mans.ecommerce.b2c.controller.utill.annotation.ValidPassword;
import com.mans.ecommerce.b2c.controller.utill.annotation.ValidUsername;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString()
public class SignupDto
{

    @ValidUsername
    private String username;

    @ValidPassword
    private String password;

    @NotNull
    @Email(message = "must be a valid email")
    private String email;

    @NotNull
    @Size(min = 2, message = "must be 2 or more characters in length")
    @Size(max = 32, message = "must be no more than 32 characters in length")
    @Pattern(regexp = "^[a-zA-Z]+$", message = "must only be characters")
    private String firstName;

    @NotNull
    @Size(min = 2, message = "must be 2 or more characters in length")
    @Size(max = 32, message = "must be no more than 32 characters in length")
    @Pattern(regexp = "^[a-zA-Z]+$", message = "must only be characters")
    private String lastName;

}