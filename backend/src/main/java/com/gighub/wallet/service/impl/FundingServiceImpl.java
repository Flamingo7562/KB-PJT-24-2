package com.gighub.wallet.service.impl;

import com.gighub.bank.dto.MockBankAccount;
import com.gighub.bank.exception.BankAccountForbiddenException;
import com.gighub.bank.mapper.MockBankMapper;
import com.gighub.bank.service.BankTransferCommand;
import com.gighub.bank.service.BankTransferGateway;
import com.gighub.bank.service.BankTransferResult;
import com.gighub.wallet.dto.FundingOrder;
import com.gighub.wallet.mapper.param.FundingOrderParam;
import com.gighub.wallet.exception.IdempotencyKeyReusedException;
import com.gighub.wallet.exception.InvalidWalletStateException;
import com.gighub.wallet.mapper.FundingMapper;
import com.gighub.wallet.mapper.WalletMapper;
import com.gighub.wallet.mapper.param.WalletTransactionParam;
import com.gighub.wallet.service.FundingService;
import com.gighub.wallet.service.command.FundingCommand;
import com.gighub.wallet.service.result.FundingResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FundingServiceImpl implements FundingService {

    private static final String TX_FUNDING = "FUNDING";
    private static final String REF_FUNDING_ORDER = "FUNDING_ORDER";
    /** wallet_transactions.idempotency_key는 전역 UNIQUE이므로 도메인 접두어를 붙인다. */
    private static final String LEDGER_KEY_PREFIX = "FUND:";

    private final FundingMapper fundingMapper;
    private final WalletMapper walletMapper;
    private final MockBankMapper mockBankMapper;
    private final BankTransferGateway bankTransferGateway;

    @Override
    @Transactional
    public FundingResult fund(FundingCommand command) {
        // 1. 멱등성: 같은 키면 저장된 결과를 재응답, 요청 본문이 다르면 409
        FundingOrder existing = fundingMapper.findByIdempotencyKey(command.getIdempotencyKey());
        if (existing != null) {
            if (!existing.getEmployerId().equals(command.getEmployerId())
                    || !existing.getLinkedAccountId().equals(command.getLinkedAccountId())
                    || !existing.getExpectedAmount().equals(command.getAmount())) {
                throw new IdempotencyKeyReusedException("같은 멱등 키로 다른 요청이 접수되었습니다.");
            }
            return replay(existing, command.getEmployerId());
        }

        // 2. 계좌 소유권 (쓰기 전에 403으로 거른다)
        MockBankAccount account = mockBankMapper.getAccountById(command.getLinkedAccountId());
        if (account == null || !account.getUserId().equals(command.getEmployerId())) {
            throw new BankAccountForbiddenException("본인 소유 계좌가 아닙니다.");
        }

        // 3. 잠금 순서 고정: 지갑 → 계좌 (출금 흐름과 반대로 잠그면 데드락)
        Long availableBefore = walletMapper.getAvailableBalanceForUpdate(command.getEmployerId());
        if (availableBefore == null) {
            throw new InvalidWalletStateException("지갑을 찾을 수 없습니다.");
        }
        Long lockedBefore = walletMapper.getLockedBalance(command.getEmployerId());
        Long walletId = walletMapper.getWalletIdByUserId(command.getEmployerId());

        // 4. 주문 생성 (은행 원장의 reference_id로 쓰이므로 PK가 먼저 필요하다)
        FundingOrderParam order = FundingOrderParam.builder()
                .employerId(command.getEmployerId())
                .linkedAccountId(command.getLinkedAccountId())
                .expectedAmount(command.getAmount())
                .idempotencyKey(command.getIdempotencyKey())
                .build();
        fundingMapper.insertFundingOrder(order);

        // 5. 계좌 차감 + 은행 원장 (게이트웨이가 계좌를 잠근다)
        BankTransferResult transfer = bankTransferGateway.withdraw(BankTransferCommand.builder()
                .accountId(command.getLinkedAccountId())
                .userId(command.getEmployerId())
                .amount(command.getAmount())
                .referenceType(REF_FUNDING_ORDER)
                .referenceId(order.getId())
                .build());

        // 6. 주문 완료
        if (fundingMapper.completeFundingOrder(
                order.getId(), command.getAmount(), transfer.getBankTransactionId()) != 1) {
            throw new InvalidWalletStateException("충전 주문 상태 갱신에 실패했습니다.");
        }

        // 7. 지갑 증가
        if (walletMapper.addAvailableBalance(command.getEmployerId(), command.getAmount()) != 1) {
            throw new InvalidWalletStateException("지갑 입금에 실패했습니다.");
        }

        // 8. 지갑 원장 (충전은 available만 증가, locked 불변, work_case_id는 NULL)
        walletMapper.insertWalletTransaction(WalletTransactionParam.builder()
                .walletId(walletId)
                .workCaseId(null)
                .transactionType(TX_FUNDING)
                .amount(command.getAmount())
                .availableBefore(availableBefore)
                .availableAfter(availableBefore + command.getAmount())
                .lockedBefore(lockedBefore)
                .lockedAfter(lockedBefore)
                .referenceType(REF_FUNDING_ORDER)
                .referenceId(order.getId())
                .idempotencyKey(LEDGER_KEY_PREFIX + command.getIdempotencyKey())
                .build());

        return FundingResult.builder()
                .fundingOrderId(order.getId())
                .status("COMPLETED")
                .bankTransactionId(transfer.getBankTransactionId())
                .availableBalance(availableBefore + command.getAmount())
                .lockedBalance(lockedBefore)
                .replayed(false)
                .build();
    }

    private FundingResult replay(FundingOrder order, Long employerId) {
        return FundingResult.builder()
                .fundingOrderId(order.getId())
                .status(order.getStatus())
                .bankTransactionId(order.getMockBankTransactionId())
                .availableBalance(walletMapper.getAvailableBalance(employerId))
                .lockedBalance(walletMapper.getLockedBalance(employerId))
                .replayed(true)
                .build();
    }
}
