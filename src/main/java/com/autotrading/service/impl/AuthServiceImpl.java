package com.autotrading.service.impl;

import com.autotrading.model.AuthSession;
import com.autotrading.service.AuthService;
import com.autotrading.util.AccountCredentialStore;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;

@Service
public class AuthServiceImpl implements AuthService {
    private static final String SESSION_KEY = "AUTO_TRADING_AUTH";
    private final AccountCredentialStore credentialStore;

    public AuthServiceImpl(AccountCredentialStore credentialStore) {
        this.credentialStore = credentialStore;
    }

    @Override
    public void login(HttpSession session, String accountNo, String accountPassword) {
        if (session == null) {
            return;
        }
        String safeAccount = StringUtils.hasText(accountNo) ? accountNo.trim() : "";
        String safePassword = StringUtils.hasText(accountPassword) ? accountPassword.trim() : "";
        AuthSession authSession = new AuthSession(safeAccount, safePassword, LocalDateTime.now());
        session.setAttribute(SESSION_KEY, authSession);
        credentialStore.update(safeAccount, safePassword);
    }

    @Override
    public void logout(HttpSession session) {
        if (session == null) {
            return;
        }
        session.removeAttribute(SESSION_KEY);
        credentialStore.clear();
    }

    @Override
    public AuthSession getSession(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(SESSION_KEY);
        if (value instanceof AuthSession) {
            return (AuthSession) value;
        }
        return null;
    }

    @Override
    public boolean isLoggedIn(HttpSession session) {
        AuthSession authSession = getSession(session);
        return authSession != null && StringUtils.hasText(authSession.getAccountNo());
    }

    @Override
    public String getMaskedAccountNo(HttpSession session) {
        AuthSession authSession = getSession(session);
        if (authSession == null) {
            return "";
        }
        return authSession.getMaskedAccountNo();
    }
}
