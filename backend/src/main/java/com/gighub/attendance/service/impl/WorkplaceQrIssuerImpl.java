package com.gighub.attendance.service.impl;

import java.security.SecureRandom;

import com.gighub.attendance.mapper.QrTokenMapper;
import com.gighub.attendance.mapper.param.QrTokenInsertParam;
import com.gighub.attendance.qr.QrTokenCodec;
import com.gighub.attendance.service.WorkplaceQrIssuer;
import org.springframework.stereotype.Service;

/** 활성 고정 QR을 호출자의 트랜잭션 안에서 발급합니다. */
@Service
public class WorkplaceQrIssuerImpl implements WorkplaceQrIssuer {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final QrTokenMapper qrTokenMapper;

    public WorkplaceQrIssuerImpl(QrTokenMapper qrTokenMapper) {
        this.qrTokenMapper = qrTokenMapper;
    }

    /**
     * {@inheritDoc}
     *
     * <p>{@code @Transactional}을 붙이지 않습니다. 호출자의 트랜잭션에 참여해야 사업장과 QR이
     * 함께 Commit되거나 함께 Rollback됩니다. 여기서 새 트랜잭션을 열면 사업장 생성이
     * 실패해도 QR만 남습니다.</p>
     */
    @Override
    public byte[] issueActive(Long workplaceId, Long ownerUserId) {
        byte[] nonce = new byte[QrTokenCodec.NONCE_LENGTH];
        RANDOM.nextBytes(nonce);

        QrTokenInsertParam param = new QrTokenInsertParam();
        param.setWorkplaceId(workplaceId);
        param.setIssuedByUserId(ownerUserId);
        param.setTokenNonce(nonce);

        if (qrTokenMapper.insertActive(param) != 1) {
            throw new IllegalStateException(
                    "사업장 " + workplaceId + "의 QR 발급이 반영되지 않았습니다.");
        }
        return nonce;
    }
}
