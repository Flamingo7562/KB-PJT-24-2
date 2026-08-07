package com.gighub.work.domain;

import java.util.Objects;

/**
 * 근무 등록 시점에 사업장 주소를 근무 조건 Snapshot 문자열로 결합합니다.
 *
 * <p>API_SPEC 4.0.0이 결합 규칙을 고정했습니다. {@code road_address}를 trim하고, trim한
 * {@code detail_address}가 비어 있지 않을 때만 한 칸을 사이에 두어 결합합니다. 상세주소가
 * 없거나 공백뿐이면 도로명주소만 남습니다.</p>
 */
public final class WorkCaseAddress {

    private WorkCaseAddress() {
    }

    /**
     * 도로명주소와 상세주소를 근무 조건 Snapshot 문자열로 결합합니다.
     *
     * @param roadAddress   사업장 도로명주소
     * @param detailAddress 사업장 상세주소. 없으면 {@code null}
     * @return trim한 도로명주소, 상세주소가 있으면 한 칸을 두고 이어붙인 문자열
     */
    public static String combine(String roadAddress, String detailAddress) {
        Objects.requireNonNull(roadAddress, "roadAddress");

        String trimmedRoad = roadAddress.trim();
        String trimmedDetail = detailAddress == null ? "" : detailAddress.trim();

        return trimmedDetail.isEmpty() ? trimmedRoad : trimmedRoad + " " + trimmedDetail;
    }
}
