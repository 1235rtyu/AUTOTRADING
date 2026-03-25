package com.autotrading.service.impl;

import com.autotrading.config.KisProperties;
import com.autotrading.model.AuthSession;
import com.autotrading.service.AuthService;
import com.autotrading.util.AccountCredentialStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;

@Service
public class AuthServiceImpl implements AuthService {
    private static final String SESSION_KEY = "AUTO_TRADING_AUTH";
    private static final String SESSION_CANO = "KIS_CANO";
    private static final String SESSION_PROD = "KIS_PRDT_CD";
    private final AccountCredentialStore credentialStore;
    private final KisProperties kisProperties;
    private final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    public AuthServiceImpl(AccountCredentialStore credentialStore, KisProperties kisProperties) {
        this.credentialStore = credentialStore;
        this.kisProperties = kisProperties;
    }

    @Override
    public void login(HttpSession session, String accountNo, String accountPassword) {
        if (session == null) {
            return;
        }
        String safeAccount = StringUtils.hasText(accountNo) ? accountNo.trim() : "";
        String safePassword = StringUtils.hasText(accountPassword) ? accountPassword.trim() : "";
        AccountParts parts = parseAccountParts(safeAccount);
        AuthSession authSession = new AuthSession(safeAccount, safePassword, parts.productCode, LocalDateTime.now());
        session.setAttribute(SESSION_KEY, authSession);
        session.setAttribute(SESSION_CANO, parts.cano);
        session.setAttribute(SESSION_PROD, parts.productCode);
        credentialStore.update(safeAccount, safePassword);
        if (kisProperties != null) {
            kisProperties.updateAccountInfo(safeAccount, parts.productCode);
            logger.info("KIS account config updated from login: canoPresent={} prodPresent={}",
                    StringUtils.hasText(parts.cano),
                    StringUtils.hasText(parts.productCode));
        }
    }

    @Override
    public void logout(HttpSession session) {
        if (session == null) {
            return;
        }
        session.removeAttribute(SESSION_KEY);
        session.removeAttribute(SESSION_CANO);
        session.removeAttribute(SESSION_PROD);
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

    private AccountParts parseAccountParts(String accountNo) {
        if (!StringUtils.hasText(accountNo)) {
            return new AccountParts("", "");
        }
        String normalized = accountNo.trim().replace(" ", "");
        if (normalized.contains("-")) {
            String[] parts = normalized.split("-", 2);
            String cano = parts.length > 0 ? parts[0] : "";
            String product = parts.length > 1 ? parts[1] : "";
            return new AccountParts(cano, product);
        }
        if (normalized.length() >= 10) {
            return new AccountParts(normalized.substring(0, 8), normalized.substring(8, 10));
        }
        if (normalized.length() == 8) {
            return new AccountParts(normalized, "");
        }
        return new AccountParts("", "");
    }

    private static class AccountParts {
        private final String cano;
        private final String productCode;

        private AccountParts(String cano, String productCode) {
            this.cano = cano;
            this.productCode = productCode;
        }
    }

}
