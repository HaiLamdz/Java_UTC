package com.bookstore.dto;

import jakarta.validation.constraints.NotBlank;

public class LoginForm {

    @NotBlank(message = "Vui lòng nhập tên đăng nhập")
    private String username;

    @NotBlank(message = "Vui lòng nhập mật khẩu")
    private String password;

    public LoginForm() {
    }

    public LoginForm(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
