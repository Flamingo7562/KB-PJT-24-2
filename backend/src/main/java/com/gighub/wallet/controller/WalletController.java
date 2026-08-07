package com.gighub.wallet.controller;

import com.gighub.auth.security.AuthPrincipals;
import com.gighub.common.api.ApiResponse;
import com.gighub.common.api.PageResponse;
import com.gighub.wallet.dto.WalletBalanceResponse;
import com.gighub.wallet.dto.WalletTransactionItem;
import com.gighub.wallet.dto.WalletTransactionQuery;
import com.gighub.wallet.service.WalletQueryService;
import com.gighub.wallet.service.command.WalletTransactionCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
public class WalletController {

    private final WalletQueryService walletQueryService;

    // 내 지갑 요약, availableBalance만 대표 잔액이며 lockedBalance와 합산해 제공하지 않는다.
    @GetMapping("/api/wallet")
    public ResponseEntity<ApiResponse<WalletBalanceResponse>> getWallet(Authentication authentication) {
        Long loginUserId = AuthPrincipals.resolve(authentication).getUserId();
        return ResponseEntity.ok(ApiResponse.of(walletQueryService.getWallet(loginUserId)));
    }

    // 지갑 거래 내역
    @GetMapping("/api/wallet/transactions")
    public ResponseEntity<ApiResponse<PageResponse<WalletTransactionItem>>> getTransactions(
            @Valid @ModelAttribute WalletTransactionQuery query,
            Authentication authentication) {
        Long loginUserId = AuthPrincipals.resolve(authentication).getUserId();
        return ResponseEntity.ok(
                ApiResponse.of(walletQueryService.getTransactions(
                        loginUserId,
                        toCriteria(query)
                ))
        );
    }

    private WalletTransactionCriteria toCriteria(WalletTransactionQuery query) {
        return WalletTransactionCriteria.builder()
                .workplaceId(query.getWorkplaceId())
                .from(query.getFrom())
                .to(query.getTo())
                .type(query.getType())
                .minAmount(query.getMinAmount())
                .maxAmount(query.getMaxAmount())
                .keyword(query.getKeyword())
                .sort(query.getSort())
                .page(query.getPage())
                .size(query.getSize())
                .build();
    }
}
