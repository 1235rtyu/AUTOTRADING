package com.autotrading.model;

public class AccountCredential {
    private final String accountNo;
    private final String accountPassword;
    private final String cano;
    private final String accountProductCode;

    public AccountCredential(String accountNo, String accountPassword, String cano, String accountProductCode) {
        this.accountNo = accountNo;
        this.accountPassword = accountPassword;
        this.cano = cano;
        this.accountProductCode = accountProductCode;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public String getAccountPassword() {
        return accountPassword;
    }

    public String getCano() {
        return cano;
    }

    public String getAccountProductCode() {
        return accountProductCode;
    }
}
