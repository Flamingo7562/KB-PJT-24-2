package com.gighub.wallet.service;

import com.gighub.common.api.PageResponse;
import com.gighub.wallet.dto.WalletBalanceResponse;
import com.gighub.wallet.dto.WalletTransactionItem;
import com.gighub.wallet.service.command.WalletTransactionCriteria;

/** 인증 사용자의 지갑 잔액과 원장 조회를 제공합니다. */
public interface WalletQueryService {

    WalletBalanceResponse getWallet(Long userId);

    PageResponse<WalletTransactionItem> getTransactions(
            Long userId,
            WalletTransactionCriteria criteria);
}
