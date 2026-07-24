package com.gighub.bank.controller;

import com.gighub.bank.dto.BankAccountSummary;
import com.gighub.bank.mapper.MockBankQueryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class MockBankAccountController {
    private static final String LOGIN_USER = "LOGIN_USER";
    private static final String STATUS_ACTIVE = "ACTIVE";

    private final MockBankQueryMapper mockBankQueryMapper;

    // 내 Mock 계좌 목록
    @GetMapping("/api/mock-bank-accounts")
    public ResponseEntity<Map<String, Object>> getAccounts(
            @RequestParam(value = "status", required = false) String status,
            HttpSession session) {
        Long loginUserId = (Long) session.getAttribute(LOGIN_USER);
        if (loginUserId == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("code", "AUTH_REQUIRED", "message", "로그인이 필요합니다."));
        }

        if (status != null && !STATUS_ACTIVE.equals(status)) {
            return ResponseEntity.status(400)
                    .body(Map.of("code", "INVALID_FILTER", "message", "지원하지 않는 status 값입니다."));
        }

        List<BankAccountSummary> accounts =
                mockBankQueryMapper.findAccountsByUserId(loginUserId, status);

        List<Map<String, Object>> items = new ArrayList<>();
        for (BankAccountSummary account : accounts) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("bankAccountId", account.getBankAccountId());
            item.put("bankCode", account.getBankCode());
            item.put("maskedAccountNumber", account.getMaskedAccountNumber());
            item.put("currency", account.getCurrency());
            item.put("balance", account.getBalance());
            item.put("availableAmount", account.getAvailableAmount());
            item.put("status", account.getStatus());
            items.add(item);
        }

        return ResponseEntity.ok(Map.of("data", Map.of("items", items)));

    }
}
