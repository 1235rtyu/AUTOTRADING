package com.autotrading.controller;

import com.autotrading.model.AuthSession;
import com.autotrading.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {
    private final AuthService authService;
    private static final String ACCOUNT_PATTERN = "^\\d{8}-\\d{2}$";

    public AuthApiController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/status")
    public Map<String, Object> status(HttpSession session) {
        boolean loggedIn = authService.isLoggedIn(session);
        String masked = authService.getMaskedAccountNo(session);
        return Map.of(
                "loggedIn", loggedIn,
                "accountMasked", masked
        );
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestParam("accountNo") String accountNo,
                                     @RequestParam("accountPassword") String accountPassword,
                                     HttpSession session) {
        if (accountNo == null || accountNo.trim().isEmpty() || accountPassword == null || accountPassword.trim().isEmpty()) {
            return Map.of("status", "ERROR", "message", "Account and password are required.");
        }
        if (!accountNo.trim().matches(ACCOUNT_PATTERN)) {
            return Map.of("status", "ERROR", "message", "AccountNo format must be 12345678-01.");
        }
        authService.login(session, accountNo, accountPassword);
        AuthSession authSession = authService.getSession(session);
        return Map.of(
                "status", "OK",
                "accountMasked", authSession != null ? authSession.getMaskedAccountNo() : ""
        );
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpSession session) {
        authService.logout(session);
        return Map.of("status", "OK");
    }
}
