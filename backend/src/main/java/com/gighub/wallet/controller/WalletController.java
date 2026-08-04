package com.gighub.wallet.controller;

import com.gighub.common.api.ApiResponse;
import com.gighub.common.api.PageRequests;
import com.gighub.common.api.PageResponse;
import com.gighub.common.exception.AuthRequiredException;
import com.gighub.common.exception.ResourceNotFoundException;
import com.gighub.common.exception.ValidationException;
import com.gighub.wallet.dto.WalletBalanceResponse;
import com.gighub.wallet.dto.WalletSummary;
import com.gighub.wallet.dto.WalletTransactionItem;
import com.gighub.wallet.dto.WalletTransactionSearch;
import com.gighub.wallet.dto.WalletTransactionView;
import com.gighub.wallet.mapper.WalletQueryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.time.LocalDate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


@RestController
@RequiredArgsConstructor
public class WalletController {

    private static final String LOGIN_USER = "LOGIN_USER";
    private static final String CURRENCY_KRW = "KRW";

    private final WalletQueryMapper walletQueryMapper;

    // 내 지갑 요약, availableBalance만 대표 잔액이며 lockedBalance와 합산해 제공하지 않는다.
    @GetMapping("/api/wallet")
    public ResponseEntity<ApiResponse<WalletBalanceResponse>> getWallet(HttpSession session){
        Long loginUserId = (Long) session.getAttribute(LOGIN_USER);
        if (loginUserId == null) {
            throw new AuthRequiredException("로그인이 필요합니다.");
        }

        WalletSummary summary = walletQueryMapper.findWalletSummaryByUserId(loginUserId);
        if (summary == null) {
            throw new ResourceNotFoundException("지갑을 찾을 수 없습니다.");
        }

        return ResponseEntity.ok(
                ApiResponse.of(WalletBalanceResponse.from(CURRENCY_KRW, summary)));
    }

    private static final Set<String> ALLOWED_SORTS =
            Set.of("LATEST", "OLDEST", "AMOUNT_ASC", "AMOUNT_DESC");
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "FUNDING", "ESCROW_HOLD", "ESCROW_RELEASE", "ESCROW_REFUND",
            "WITHDRAWAL", "WITHDRAWAL_REFUND", "ADJUSTMENT");

    // 지갑 거래 내역
    @GetMapping("/api/wallet/transactions")
    public ResponseEntity<ApiResponse<PageResponse<WalletTransactionItem>>> getTransactions(
            @RequestParam(value = "workplaceId", required = false) Long workplaceId,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "minAmount", required = false) Long minAmount,
            @RequestParam(value = "maxAmount", required = false) Long maxAmount,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "sort", defaultValue = "LATEST") String sort,
            @RequestParam(value = "page", defaultValue = PageRequests.DEFAULT_PAGE_TEXT) int page,
            @RequestParam(value = "size", defaultValue = PageRequests.DEFAULT_SIZE_TEXT) int size,
            HttpSession session) {

        Long loginUserId = (Long) session.getAttribute(LOGIN_USER);
        if (loginUserId == null) {
            throw new AuthRequiredException("로그인이 필요합니다.");
        }
        PageRequests.validate(page, size);
        if (!ALLOWED_SORTS.contains(sort)) {
            throw new ValidationException("지원하지 않는 sort 값입니다.");
        }
        if (type != null && !ALLOWED_TYPES.contains(type)) {
            throw new ValidationException("지원하지 않는 type 값입니다.");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new ValidationException("from은 to보다 이후일 수 없습니다.");
        }
        if (minAmount != null && maxAmount != null && minAmount > maxAmount) {
            throw new ValidationException("minAmount는 maxAmount보다 클 수 없습니다.");
        }

        String trimmedKeyword = (keyword == null || keyword.trim().isEmpty())
                ? null : keyword.trim();

        WalletTransactionSearch search = WalletTransactionSearch.builder()
                .userId(loginUserId)
                .workplaceId(workplaceId)
                .from(from == null ? null : from.atStartOfDay())
                // to는 해당 일자를 포함하도록 다음 날 00:00 미만으로 비교한다
                .toExclusive(to == null ? null : to.plusDays(1).atStartOfDay())
                .type(type)
                .minAmount(minAmount)
                .maxAmount(maxAmount)
                .keyword(trimmedKeyword)
                .sort(sort)
                .offset(PageRequests.offset(page, size))
                .size(size)
                .build();

        long totalElements = walletQueryMapper.countTransactions(search);
        List<WalletTransactionView> rows = totalElements == 0
                ? List.of() : walletQueryMapper.findTransactions(search);

        List<WalletTransactionItem> content = new ArrayList<>();
        for (WalletTransactionView row : rows) {
            content.add(WalletTransactionItem.from(row, resolveDisplayStatus(row.getType())));
        }

        return ResponseEntity.ok(
                ApiResponse.of(PageResponse.of(content, page, size, totalElements)));
    }

    // 화면 거래구분 파생값
    private String resolveDisplayStatus(String transactionType) {
        switch (transactionType) {
            case "FUNDING":
                return "충전";
            case "ESCROW_HOLD":
                return "예치중";
            case "ESCROW_RELEASE":
                return "지급완료";
            case "ESCROW_REFUND":
                return "환불";
            case "WITHDRAWAL":
                return "출금";
            case "WITHDRAWAL_REFUND":
                return "출금환불";
            default:
                return "조정";
        }
    }
}
