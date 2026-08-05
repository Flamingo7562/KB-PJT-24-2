package com.gighub.workplace.dto;

import java.math.BigDecimal;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.gighub.workplace.validation.CoordinatePair;

/**
 * 승인된 사업장 등록 입력과 정규화·검증 계약입니다.
 *
 * <p>소유자 식별자는 인증 Principal에서만 정하므로 입력 필드로 두지 않습니다. 인증 반경도
 * 사용자가 정할 수 없어 {@code radiusMeters}, {@code radiusM}은 허용되지 않은 필드로
 * 거절합니다.</p>
 *
 * <p>검증을 통과한 뒤 값이 바뀌지 않도록 불변으로 두고 역직렬화와 정규화는 Builder가
 * 담당합니다. Builder 없이 Jackson이 직접 필드를 채우면 검증 이후 Setter로 값을 바꿀 수
 * 있는 경로가 남습니다.</p>
 */
@CoordinatePair
@JsonDeserialize(builder = WorkplaceCreateRequest.Builder.class)
public final class WorkplaceCreateRequest {

    @NotBlank(message = "사업자등록번호는 필수입니다.")
    @Pattern(regexp = "^[0-9]{10}$", message = "사업자등록번호는 숫자 10자리여야 합니다.")
    private final String businessRegistrationNumber;

    @NotBlank(message = "상호명은 필수입니다.")
    @Size(max = 120, message = "상호명은 120자 이하여야 합니다.")
    private final String name;

    @NotBlank(message = "대표자명은 필수입니다.")
    @Size(max = 100, message = "대표자명은 100자 이하여야 합니다.")
    private final String representativeName;

    @NotBlank(message = "도로명주소는 필수입니다.")
    @Size(max = 255, message = "도로명주소는 255자 이하여야 합니다.")
    private final String roadAddress;

    @Size(max = 100, message = "상세주소는 100자 이하여야 합니다.")
    private final String detailAddress;

    @NotBlank(message = "사업장 전화번호는 필수입니다.")
    @Pattern(regexp = "^0\\d{8,10}$", message = "전화번호 형식이 올바르지 않습니다.")
    private final String phone;

    @DecimalMin(value = "-90", message = "위도는 -90 이상이어야 합니다.")
    @DecimalMax(value = "90", message = "위도는 90 이하여야 합니다.")
    @Digits(integer = 3, fraction = 7, message = "위도는 소수점 7자리까지만 허용합니다.")
    private final BigDecimal latitude;

    @DecimalMin(value = "-180", message = "경도는 -180 이상이어야 합니다.")
    @DecimalMax(value = "180", message = "경도는 180 이하여야 합니다.")
    @Digits(integer = 3, fraction = 7, message = "경도는 소수점 7자리까지만 허용합니다.")
    private final BigDecimal longitude;

    private WorkplaceCreateRequest(Builder builder) {
        this.businessRegistrationNumber = builder.businessRegistrationNumber;
        this.name = builder.name;
        this.representativeName = builder.representativeName;
        this.roadAddress = builder.roadAddress;
        this.detailAddress = builder.detailAddress;
        this.phone = builder.phone;
        this.latitude = builder.latitude;
        this.longitude = builder.longitude;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getBusinessRegistrationNumber() {
        return businessRegistrationNumber;
    }

    public String getName() {
        return name;
    }

    public String getRepresentativeName() {
        return representativeName;
    }

    public String getRoadAddress() {
        return roadAddress;
    }

    public String getDetailAddress() {
        return detailAddress;
    }

    public String getPhone() {
        return phone;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    /**
     * 승인 입력을 정규화해 불변 요청으로 만듭니다.
     *
     * <p>{@code withPrefix = ""}가 없으면 Jackson이 {@code withPhone} 형태를 찾다가 값을
     * 채우지 못하므로 반드시 유지합니다.</p>
     */
    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {

        private String businessRegistrationNumber;
        private String name;
        private String representativeName;
        private String roadAddress;
        private String detailAddress;
        private String phone;
        private BigDecimal latitude;
        private BigDecimal longitude;

        private Builder() {
        }

        public Builder businessRegistrationNumber(String businessRegistrationNumber) {
            this.businessRegistrationNumber =
                    WorkplaceNormalizer.normalizeBusinessRegistrationNumber(businessRegistrationNumber);
            return this;
        }

        public Builder name(String name) {
            this.name = WorkplaceNormalizer.normalizeText(name);
            return this;
        }

        public Builder representativeName(String representativeName) {
            this.representativeName = WorkplaceNormalizer.normalizeText(representativeName);
            return this;
        }

        public Builder roadAddress(String roadAddress) {
            this.roadAddress = WorkplaceNormalizer.normalizeText(roadAddress);
            return this;
        }

        public Builder detailAddress(String detailAddress) {
            this.detailAddress = WorkplaceNormalizer.normalizeOptionalText(detailAddress);
            return this;
        }

        public Builder phone(String phone) {
            this.phone = WorkplaceNormalizer.normalizePhone(phone);
            return this;
        }

        public Builder latitude(BigDecimal latitude) {
            this.latitude = latitude;
            return this;
        }

        public Builder longitude(BigDecimal longitude) {
            this.longitude = longitude;
            return this;
        }

        /** 명세에 없는 사업장 필드는 조용히 무시하지 않고 요청 오류로 처리합니다. */
        @JsonAnySetter
        public void rejectUnknownField(String fieldName, Object value) {
            throw new IllegalArgumentException("허용되지 않은 사업장 필드입니다: " + fieldName);
        }

        public WorkplaceCreateRequest build() {
            return new WorkplaceCreateRequest(this);
        }
    }
}
