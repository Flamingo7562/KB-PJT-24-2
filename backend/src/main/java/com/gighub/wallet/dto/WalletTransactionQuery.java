package com.gighub.wallet.dto;

import com.gighub.common.api.PageRequests;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import java.time.LocalDate;

/** 지갑 거래 목록의 외부 Query Parameter 계약입니다. */
@Getter
@Setter
public class WalletTransactionQuery {

    private static final String TYPE_PATTERN =
            "FUNDING|ESCROW_HOLD|ESCROW_RELEASE|ESCROW_REFUND|WITHDRAWAL|"
                    + "WITHDRAWAL_REFUND|ADJUSTMENT";
    private static final String SORT_PATTERN =
            "LATEST|OLDEST|AMOUNT_ASC|AMOUNT_DESC";

    @Positive(message = "workplaceId는 1 이상이어야 합니다.")
    private Long workplaceId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate from;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate to;

    @Pattern(regexp = TYPE_PATTERN, message = "지원하지 않는 type 값입니다.")
    private String type;

    @PositiveOrZero(message = "minAmount는 0 이상이어야 합니다.")
    private Long minAmount;

    @PositiveOrZero(message = "maxAmount는 0 이상이어야 합니다.")
    private Long maxAmount;

    private String keyword;

    @Pattern(regexp = SORT_PATTERN, message = "지원하지 않는 sort 값입니다.")
    private String sort = "LATEST";

    @Min(value = 0, message = "page는 0 이상이어야 합니다.")
    private int page = PageRequests.DEFAULT_PAGE;

    @Min(value = 1, message = "size는 1 이상이어야 합니다.")
    @Max(value = PageRequests.MAX_SIZE, message = "size는 100 이하여야 합니다.")
    private int size = PageRequests.DEFAULT_SIZE;
}
