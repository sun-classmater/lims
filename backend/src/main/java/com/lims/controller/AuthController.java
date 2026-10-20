package com.lims.controller;

import com.lims.common.R;
import com.lims.dto.LoginRequest;
import com.lims.service.AuthService;
import com.lims.vo.LoginResponse;
import com.lims.vo.UserInfoVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return R.ok(authService.login(request));
    }

    @GetMapping("/me")
    public R<UserInfoVO> me(@RequestAttribute("userId") String userId) {
        return R.ok(authService.getCurrentUser(userId));
    }
}
