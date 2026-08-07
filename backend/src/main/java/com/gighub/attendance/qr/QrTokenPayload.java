package com.gighub.attendance.qr;

/**
 * 검증에 성공한 QR Token이 담고 있던 값입니다.
 *
 * <p>{@code nonce}는 방어 복사해 보관합니다. 호출자가 돌려받은 배열을 고쳐도 검증 결과가
 * 바뀌지 않아야 합니다.</p>
 */
public final class QrTokenPayload {

    private final long workplaceId;
    private final byte[] nonce;

    QrTokenPayload(long workplaceId, byte[] nonce) {
        this.workplaceId = workplaceId;
        this.nonce = nonce.clone();
    }

    public long workplaceId() {
        return workplaceId;
    }

    public byte[] nonce() {
        return nonce.clone();
    }
}
