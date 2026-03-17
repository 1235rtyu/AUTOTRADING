package com.autotrading.service;

import com.autotrading.model.AuthSession;

import javax.servlet.http.HttpSession;

public interface AuthService {
    void login(HttpSession session, String accountNo, String accountPassword);
    void logout(HttpSession session);
    AuthSession getSession(HttpSession session);
    boolean isLoggedIn(HttpSession session);
    String getMaskedAccountNo(HttpSession session);
}
