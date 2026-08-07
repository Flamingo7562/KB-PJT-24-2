package com.gighub.invitation.service.impl;

import com.gighub.common.exception.ConflictException;
import com.gighub.invitation.service.AcceptEscrowHold;
import com.gighub.wallet.dto.WalletBalanceSnapshot;
import com.gighub.wallet.mapper.WalletMapper;
import com.gighub.wallet.mapper.param.WalletTransactionParam;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Objects;

/**
 * 지갑 도메인의 검증된 SQL로 예치만 수행합니다.
 *
 * <p>잠금과 상태 전이는 호출부(수락 Aggregate)가 이미 마쳤다고 전제합니다. 이 Component가
 * 근무를 다시 잠그거나 상태를 바꾸면 잠금 순서가 둘로 갈라져 교착이 생깁니다.</p>
 */
@Service
public class AcceptEscrowHoldImpl implements AcceptEscrowHold {

    private static final String TRANSACTION_TYPE = "ESCROW_HOLD";
    private static final String REFERENCE_TYPE = "ESCROW";

    /** 원장 Key 앞에 붙는 승인된 표지입니다. */
    private static final String LEDGER_KEY_PREFIX = "EHLD:";

    /** 원장 Key 파생 입력의 Operation 표지입니다. */
    private static final String OPERATION_LABEL = "INVITATION_ACCEPT\n";

    private static final String INSUFFICIENT_BALANCE =
            "사장님의 예치 가능 잔액이 부족하여 근무를 확정할 수 없습니다.";

    private final WalletMapper walletMapper;

    public AcceptEscrowHoldImpl(WalletMapper walletMapper) {
        this.walletMapper = walletMapper;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public long hold(
            long employerId,
            long workCaseId,
            long amount,
            long claimId,
            LocalDateTime acceptedAt) {
        WalletBalanceSnapshot wallet = walletMapper.getWalletSnapshotForUpdate(employerId);
        if (wallet == null) {
            throw new IllegalStateException("OWNER 지갑을 찾을 수 없습니다.");
        }
        if (wallet.getAvailableBalance() < amount) {
            // 잔액 자체는 알리지 않습니다. 상대 사업장의 자금 사정이 드러납니다.
            throw new ConflictException(INSUFFICIENT_BALANCE);
        }

        long availableAfter = Math.subtractExact(wallet.getAvailableBalance(), amount);
        long lockedAfter = Math.addExact(wallet.getLockedBalance(), amount);

        // 조건부 UPDATE라 잠금과 갱신 사이에 잔액이 줄어도 음수가 되지 않습니다.
        if (walletMapper.lockEmployerFunds(employerId, amount) != 1) {
            throw new IllegalStateException("예치 반영 결과가 예상과 다릅니다.");
        }
        if (walletMapper.insertHeldEscrowAt(workCaseId, amount, acceptedAt) != 1) {
            throw new IllegalStateException("에스크로를 생성하지 못했습니다.");
        }

        long escrowId = Objects.requireNonNull(
                walletMapper.getEscrowIdByWorkCaseId(workCaseId), "생성된 에스크로 식별자");

        walletMapper.insertWalletTransaction(WalletTransactionParam.builder()
                .walletId(wallet.getWalletId())
                .workCaseId(workCaseId)
                .transactionType(TRANSACTION_TYPE)
                .amount(amount)
                .availableBefore(wallet.getAvailableBalance())
                .availableAfter(availableAfter)
                .lockedBefore(wallet.getLockedBalance())
                .lockedAfter(lockedAfter)
                .referenceType(REFERENCE_TYPE)
                .referenceId(escrowId)
                .idempotencyKey(ledgerKey(claimId))
                .build());

        return escrowId;
    }

    /**
     * 원장 Key를 멱등 Claim 식별자에서 파생합니다.
     *
     * <p>요청 Header의 Key 원문을 그대로 쓰지 않습니다. 원장은 오래 보존되는데 Header Key는
     * 저장·로그에 남기지 않기로 한 값이라, 이미 저장된 Claim 식별자만으로 결정론적으로
     * 만듭니다. 같은 수락을 다시 처리하면 같은 Key가 나와 원장 Unique가 이중 기록을
     * 막습니다.</p>
     */
    private static String ledgerKey(long claimId) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((OPERATION_LABEL + claimId).getBytes(StandardCharsets.UTF_8));
            return LEDGER_KEY_PREFIX + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", exception);
        }
    }
}
