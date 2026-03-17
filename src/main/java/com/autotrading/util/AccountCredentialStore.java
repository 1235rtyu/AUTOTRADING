package com.autotrading.util;

import com.autotrading.model.AccountCredential;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class AccountCredentialStore {
    private final AtomicReference<AccountCredential> credentialRef = new AtomicReference<>();

    public void update(String accountNo, String accountPassword) {
        AccountCredential credential = toCredential(accountNo, accountPassword);
        credentialRef.set(credential);
    }

    public void clear() {
        credentialRef.set(null);
    }

    public Optional<AccountCredential> get() {
        return Optional.ofNullable(credentialRef.get());
    }

    private AccountCredential toCredential(String accountNo, String accountPassword) {
        String safeAccount = StringUtils.hasText(accountNo) ? accountNo.trim() : "";
        String safePassword = StringUtils.hasText(accountPassword) ? accountPassword.trim() : "";
        String normalized = safeAccount.replace(" ", "");

        String cano = "";
        String productCode = "";
        if (normalized.contains("-")) {
            String[] parts = normalized.split("-", 2);
            if (parts.length == 2) {
                cano = parts[0];
                productCode = parts[1];
            }
        } else if (normalized.length() >= 10) {
            cano = normalized.substring(0, 8);
            productCode = normalized.substring(8, 10);
        } else if (normalized.length() == 8) {
            cano = normalized;
        }

        return new AccountCredential(safeAccount, safePassword, cano, productCode);
    }
}
