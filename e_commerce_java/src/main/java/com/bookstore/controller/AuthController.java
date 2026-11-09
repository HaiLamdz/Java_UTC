package com.bookstore.controller;

import com.bookstore.dto.LoginForm;
import com.bookstore.dto.RegisterForm;
import com.bookstore.model.User;
import com.bookstore.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Màn hình đăng nhập dành riêng cho Khách hàng (Client)
     */
    @GetMapping("/login")
    public String loginPage(Model model, HttpSession session) {
        if (session.getAttribute("CLIENT_USER") != null) {
            return "redirect:/";
        }
        model.addAttribute("loginForm", new LoginForm());
        return "auth/login";
    }

    /**
     * Xử lý xác thực đăng nhập Khách hàng
     */
    @PostMapping("/login")
    public String processLogin(@Valid @ModelAttribute("loginForm") LoginForm form,
                               BindingResult bindingResult,
                               HttpSession session,
                               Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/login";
        }

        Optional<User> userOpt = userService.authenticate(form.getUsername(), form.getPassword());
        if (userOpt.isEmpty() || userOpt.get().isAdmin()) {
            model.addAttribute("errorMessage", "Tên đăng nhập hoặc mật khẩu không chính xác!");
            return "auth/login";
        }

        User user = userOpt.get();
        session.setAttribute("CLIENT_USER", user);
        return "redirect:/";
    }

    /**
     * Màn hình đăng ký tài khoản khách hàng
     */
    @GetMapping("/register")
    public String registerPage(Model model, HttpSession session) {
        if (session.getAttribute("CLIENT_USER") != null) {
            return "redirect:/";
        }
        model.addAttribute("registerForm", new RegisterForm());
        return "auth/register";
    }

    /**
     * Xử lý đăng ký tài khoản
     */
    @PostMapping("/register")
    public String processRegister(@Valid @ModelAttribute("registerForm") RegisterForm form,
                                  BindingResult bindingResult,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        if (!form.isPasswordMatching()) {
            model.addAttribute("errorMessage", "Mật khẩu xác nhận không trùng khớp!");
            return "auth/register";
        }

        try {
            userService.register(form);
            redirectAttributes.addFlashAttribute("successMessage", "Đăng ký tài khoản thành công! Vui lòng đăng nhập.");
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/register";
        }
    }

    /**
     * Đăng xuất khỏi tài khoản Khách hàng
     */
    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.removeAttribute("CLIENT_USER");
        redirectAttributes.addFlashAttribute("successMessage", "Đã đăng xuất thành công!");
        return "redirect:/login";
    }
}
