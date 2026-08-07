package com.gighub.document.storage;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** 계약 문서 Checksum(BINARY(32))을 계산합니다. */
public final class Sha256 {

    private Sha256() {
    }

    public static byte[] digest(byte[] content) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(content);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}
