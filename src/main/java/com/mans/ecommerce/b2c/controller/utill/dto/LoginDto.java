package com.mans.ecommerce.b2c.controller.utill.dto;

import com.mans.ecommerce.b2c.controller.utill.annotation.ValidPassword;
import com.mans.ecommerce.b2c.controller.utill.annotation.ValidUsername;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@ToString(exclude = { "password" })
public class LoginDto
{
    @ValidUsername
    private String username;

    @ValidPassword
    private String password;

}
