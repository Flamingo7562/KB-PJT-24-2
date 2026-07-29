package com.gighub.wallet.controller;

import com.gighub.wallet.dto.WalletSummary;
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
import java.time.LocalDateTime;
import java.time.ZoneId;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


@RestController
@RequiredArgsConstructor
public class WalletController {

    private static final String LOGIN_USER = "LOGIN_USER";
    private static final String CURRENCY_KRW = "KRW";

    private final WalletQueryMapper walletQueryMapper;

    // 내 지갑 요약, availableBalance만 대표 잔액이며 lockedBalance와 합산해 제공하지 않는다.
    @GetMapping("/api/wallet")
    public ResponseEntity<Map<String, Object>> getWallet(HttpSession session){
        Long loginUserId = (Long) session.getAttribute(LOGIN_USER);
        if (loginUserId == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("code", "AUTH_REQUIRED", "message", "로그인이 필요합니다."));
        }

        WalletSummary summary = walletQueryMapper.findWalletSummaryByUserId(loginUserId);
        if (summary == null) {
            return ResponseEntity.status(404)
                    .body(Map.of("code", "WALLET_NOT_FOUND", "message", "지갑을 찾을 수 없습니다."));
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("currency", CURRENCY_KRW);
        data.put("availableBalance", summary.getAvailableBalance());
        data.put("lockedBalance", summary.getLockedBalance());

        return ResponseEntity.ok(Map.of("data", data));
    }

    private static final int MAX_PAGE_SIZE = 100;
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final Set<String> ALLOWED_SORTS =
            Set.of("LATEST", "OLDEST", "AMOUNT_ASC", "AMOUNT_DESC");
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "FUNDING", "ESCROW_HOLD", "ESCROW_RELEASE", "ESCROW_REFUND",
            "WITHDRAWAL", "WITHDRAWAL_REFUND", "ADJUSTMENT");

    // 지갑 거래 내역
    @GetMapping("/api/wallet/transactions")
    public ResponseEntity<Map<String, Object>> getTransactions(
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
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            HttpSession session) {

        Long loginUserId = (Long) session.getAttribute(LOGIN_USER);
        if (loginUserId == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("code", "AUTH_REQUIRED", "message", "로그인이 필요합니다."));
        }
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            return invalidFilter("page는 0 이상, size는 1~100 사이여야 합니다.");
        }
        if (!ALLOWED_SORTS.contains(sort)) {
            return invalidFilter("지원하지 않는 sort 값입니다.");
        }
        if (type != null && !ALLOWED_TYPES.contains(type)) {
            return invalidFilter("지원하지 않는 type 값입니다.");
        }
        if (from != null && to != null && from.isAfter(to)) {
            return invalidFilter("from은 to보다 이후일 수 없습니다.");
        }
        if (minAmount != null && maxAmount != null && minAmount > maxAmount) {
            return invalidFilter("minAmount는 maxAmount보다 클 수 없습니다.");
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
                .offset((long)page * size)
                .size(size)
                .build();

        long totalElements = walletQueryMapper.countTransactions(search);
        List<WalletTransactionView> rows = totalElements == 0
                ? List.of() : walletQueryMapper.findTransactions(search);

        List<Map<String, Object>> content = new ArrayList<>();
        for (WalletTransactionView row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("transactionId", row.getTransactionId());
            item.put("type", row.getType());
            item.put("amount", row.getAmount());
            item.put("availableAfter", row.getAvailableAfter());
            item.put("lockedAfter", row.getLockedAfter());
            item.put("workCaseId", row.getWorkCaseId());
            item.put("workTitle", row.getWorkTitle());
            item.put("workplaceName", row.getWorkplaceName());
            item.put("displayStatus", resolveDisplayStatus(row.getType()));
            item.put("createdAt", toIsoOffset(row.getCreatedAt()));
            content.add(item);
        }

        Map<String, Object> pageInfo = new LinkedHashMap<>();
        pageInfo.put("number", page);
        pageInfo.put("size", size);
        pageInfo.put("totalElements", totalElements);
        pageInfo.put("totalPages", (int) Math.ceil((double) totalElements / size));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("content", content);
        data.put("page", pageInfo);

        return ResponseEntity.ok(Map.of("data", data));
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

    // DB의 datetime을 Asia/Seoul 기준 ISO-8601 offset 문자열로 변환한다 (API-004).
    private String toIsoOffset(LocalDateTime createdAt) {
        return createdAt == null ? null : createdAt.atZone(SEOUL).toOffsetDateTime().toString();
    }

    private ResponseEntity<Map<String, Object>> invalidFilter(String message) {
        return ResponseEntity.status(400)
                .body(Map.of("code", "INVALID_FILTER", "message", message));
    }
}
