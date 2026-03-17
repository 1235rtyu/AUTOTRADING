package com.autotrading.model;

import java.time.LocalDateTime;

public class AuthSession {
    private final String accountNo;
    private final String accountPassword;
    private final LocalDateTime loggedInAt;

    public AuthSession(String accountNo, String accountPassword, LocalDateTime loggedInAt) {
        this.accountNo = accountNo;
        this.accountPassword = accountPassword;
        this.loggedInAt = loggedInAt;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public String getAccountPassword() {
        return accountPassword;
    }

    public LocalDateTime getLoggedInAt() {
        return loggedInAt;
    }

    public String getMaskedAccountNo() {
        if (accountNo == null || accountNo.isEmpty()) {
            return "";
        }
        if (accountNo.length() <= 4) {
            return "****" + accountNo;
        }
        String tail = accountNo.substring(accountNo.length() - 4);
        return "****" + tail;
    }
}
