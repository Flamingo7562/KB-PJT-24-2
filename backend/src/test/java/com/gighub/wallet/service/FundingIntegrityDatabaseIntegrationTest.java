package com.gighub.wallet.service;

import com.gighub.bank.exception.BankAccountForbiddenException;
import com.gighub.bank.service.BankAccountPreflightCommand;
import com.gighub.bank.service.BankTransferGateway;
import com.gighub.config.RootConfig;
import com.gighub.wallet.dto.FundingOrder;
import com.gighub.wallet.dto.WalletBalanceSnapshot;
import com.gighub.wallet.idempotency.WalletIdempotencyKeys;
import com.gighub.wallet.mapper.FundingMapper;
import com.gighub.wallet.mapper.WalletMapper;
import com.gighub.wallet.mapper.param.FundingOrderParam;
import com.gighub.wallet.service.command.FundingCommand;
import com.gighub.wallet.service.command.WithdrawalCommand;
import com.gighub.wallet.service.result.FundingResult;
import com.gighub.wallet.service.result.WithdrawalResult;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("database")
class FundingIntegrityDatabaseIntegrationTest {

    @Test
    @Timeout(15)
    void concurrentFundingMovesMoneyOnceAndReplaysOriginalSnapshot() throws Exception {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(RootConfig.class)) {
            JdbcTemplate jdbcTemplate =
                    new JdbcTemplate(context.getBean(DataSource.class));
            FundingService fundingService = context.getBean(FundingService.class);
            FundingFixture fixture = createFundingFixture(jdbcTemplate);
            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch start = new CountDownLatch(1);
            FundingCommand command = FundingCommand.builder()
                    .employerId(fixture.userId())
                    .bankCode(FIXTURE_BANK_CODE)
                    .accountNo(fixture.accountNo())
                    .pin(FIXTURE_PIN)
                    .amount(1_000L)
                    .idempotencyKey(fixture.idempotencyKey())
                    .build();

            try {
                Future<FundingResult> first = executor.submit(() -> {
                    await(start);
                    return fundingService.fund(command);
                });
                Future<FundingResult> second = executor.submit(() -> {
                    await(start);
                    return fundingService.fund(command);
                });
                start.countDown();

                FundingResult firstResult = first.get(8, TimeUnit.SECONDS);
                FundingResult secondResult = second.get(8, TimeUnit.SECONDS);
                assertTrue(firstResult.isReplayed() ^ secondResult.isReplayed());
                assertEquals(firstResult.getFundingOrderId(), secondResult.getFundingOrderId());
                assertEquals(firstResult.getBankTransactionId(),
                        secondResult.getBankTransactionId());
                assertEquals(1_000L, firstResult.getAvailableBalance());
                assertEquals(1_000L, secondResult.getAvailableBalance());

                assertEquals(1, count(jdbcTemplate,
                        "SELECT COUNT(*) FROM funding_orders WHERE idempotency_key = ?",
                        fixture.idempotencyKey()));
                assertEquals(1, count(jdbcTemplate,
                        "SELECT COUNT(*) FROM mock_bank_transactions"
                                + " WHERE account_id = ?",
                        fixture.accountId()));
                assertEquals(1, count(jdbcTemplate,
                        "SELECT COUNT(*) FROM wallet_transactions"
                                + " WHERE wallet_id = ? AND transaction_type = 'FUNDING'",
                        fixture.walletId()));
                assertEquals(999_000L, value(jdbcTemplate,
                        "SELECT balance FROM mock_bank_accounts WHERE id = ?",
                        fixture.accountId()));
                assertEquals(999_000L, value(jdbcTemplate,
                        "SELECT available_amount FROM mock_bank_accounts WHERE id = ?",
                        fixture.accountId()));
                assertEquals(1_000L, value(jdbcTemplate,
                        "SELECT available_balance FROM wallets WHERE id = ?",
                        fixture.walletId()));

                jdbcTemplate.update(
                        "UPDATE wallets SET available_balance = available_balance + 7"
                                + " WHERE id = ?",
                        fixture.walletId()
                );
                FundingResult replayed = fundingService.fund(command);
                assertTrue(replayed.isReplayed());
                assertEquals(1_000L, replayed.getAvailableBalance());
            } finally {
                start.countDown();
                executor.shutdownNow();
                assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
                deleteFundingFixture(jdbcTemplate, fixture);
            }
        }
    }

    @Test
    @Timeout(20)
    void concurrentDifferentKeyFundingRequestsBothComplete() throws Exception {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(RootConfig.class)) {
            JdbcTemplate jdbcTemplate =
                    new JdbcTemplate(context.getBean(DataSource.class));
            FundingService fundingService = context.getBean(FundingService.class);
            FundingFixture fixture = createFundingFixture(jdbcTemplate);
            ExecutorService executor = Executors.newFixedThreadPool(2);
            // 서로 다른 키를 동시에 시작해 같은 지갑·계좌의 신규 충전 경합을 재현한다.
            CountDownLatch start = new CountDownLatch(1);
            FundingCommand first = FundingCommand.builder()
                    .employerId(fixture.userId())
                    .bankCode(FIXTURE_BANK_CODE)
                    .accountNo(fixture.accountNo())
                    .pin(FIXTURE_PIN)
                    .amount(1_000L)
                    .idempotencyKey(fixture.idempotencyKey() + "-A")
                    .build();
            FundingCommand second = FundingCommand.builder()
                    .employerId(fixture.userId())
                    .bankCode(FIXTURE_BANK_CODE)
                    .accountNo(fixture.accountNo())
                    .pin(FIXTURE_PIN)
                    .amount(1_000L)
                    .idempotencyKey(fixture.idempotencyKey() + "-B")
                    .build();

            try {
                Future<FundingResult> firstFuture = executor.submit(() -> {
                    await(start);
                    return fundingService.fund(first);
                });
                Future<FundingResult> secondFuture = executor.submit(() -> {
                    await(start);
                    return fundingService.fund(second);
                });
                start.countDown();

                FundingResult firstResult = firstFuture.get(15, TimeUnit.SECONDS);
                FundingResult secondResult = secondFuture.get(15, TimeUnit.SECONDS);

                assertFalse(firstResult.isReplayed());
                assertFalse(secondResult.isReplayed());
                assertNotEquals(firstResult.getFundingOrderId(),
                        secondResult.getFundingOrderId());
                assertEquals(2, count(jdbcTemplate,
                        "SELECT COUNT(*) FROM funding_orders WHERE employer_id = ?",
                        fixture.userId()));
                assertEquals(2, count(jdbcTemplate,
                        "SELECT COUNT(*) FROM mock_bank_transactions WHERE account_id = ?",
                        fixture.accountId()));
                assertEquals(2, count(jdbcTemplate,
                        "SELECT COUNT(*) FROM wallet_transactions"
                                + " WHERE wallet_id = ? AND transaction_type = 'FUNDING'",
                        fixture.walletId()));
                assertEquals(998_000L, value(jdbcTemplate,
                        "SELECT balance FROM mock_bank_accounts WHERE id = ?",
                        fixture.accountId()));
                assertEquals(2_000L, value(jdbcTemplate,
                        "SELECT available_balance FROM wallets WHERE id = ?",
                        fixture.walletId()));
            } finally {
                start.countDown();
                executor.shutdownNow();
                assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
                deleteFundingFixture(jdbcTemplate, fixture);
            }
        }
    }

    @Test
    @Timeout(20)
    void concurrentFundingAndWithdrawalPreserveTotalBalance() throws Exception {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(RootConfig.class)) {
            JdbcTemplate jdbcTemplate =
                    new JdbcTemplate(context.getBean(DataSource.class));
            FundingService fundingService = context.getBean(FundingService.class);
            WithdrawalService withdrawalService = context.getBean(WithdrawalService.class);
            FundingFixture fixture = createFundingFixture(jdbcTemplate);
            ExecutorService executor = Executors.newFixedThreadPool(2);
            // 같은 지갑·계좌에서 충전과 출금이 겹치는 실제 경합 상황을 재현한다.
            CountDownLatch start = new CountDownLatch(1);
            FundingCommand funding = FundingCommand.builder()
                    .employerId(fixture.userId())
                    .bankCode(FIXTURE_BANK_CODE)
                    .accountNo(fixture.accountNo())
                    .pin(FIXTURE_PIN)
                    .amount(3_000L)
                    .idempotencyKey(fixture.idempotencyKey() + "-F")
                    .build();
            WithdrawalCommand withdrawal = WithdrawalCommand.builder()
                    .userId(fixture.userId())
                    .bankCode(FIXTURE_BANK_CODE)
                    .accountNo(fixture.accountNo())
                    .amount(2_000L)
                    .idempotencyKey(fixture.idempotencyKey() + "-W")
                    .build();

            try {
                jdbcTemplate.update(
                        "UPDATE wallets SET available_balance = 10000 WHERE id = ?",
                        fixture.walletId()
                );
                Future<FundingResult> fundingFuture = executor.submit(() -> {
                    await(start);
                    return fundingService.fund(funding);
                });
                Future<WithdrawalResult> withdrawalFuture = executor.submit(() -> {
                    await(start);
                    return withdrawalService.withdraw(withdrawal);
                });
                start.countDown();

                FundingResult fundingResult = fundingFuture.get(15, TimeUnit.SECONDS);
                WithdrawalResult withdrawalResult =
                        withdrawalFuture.get(15, TimeUnit.SECONDS);

                assertFalse(fundingResult.isReplayed());
                assertFalse(withdrawalResult.isReplayed());
                assertEquals("COMPLETED", fundingResult.getStatus());
                assertEquals("COMPLETED", withdrawalResult.getStatus());
                assertEquals(1, count(jdbcTemplate,
                        "SELECT COUNT(*) FROM funding_orders WHERE employer_id = ?",
                        fixture.userId()));
                assertEquals(1, count(jdbcTemplate,
                        "SELECT COUNT(*) FROM withdrawal_requests WHERE user_id = ?",
                        fixture.userId()));
                assertEquals(2, count(jdbcTemplate,
                        "SELECT COUNT(*) FROM mock_bank_transactions WHERE account_id = ?",
                        fixture.accountId()));
                assertEquals(2, count(jdbcTemplate,
                        "SELECT COUNT(*) FROM wallet_transactions WHERE wallet_id = ?",
                        fixture.walletId()));
                assertEquals(999_000L, value(jdbcTemplate,
                        "SELECT balance FROM mock_bank_accounts WHERE id = ?",
                        fixture.accountId()));
                assertEquals(11_000L, value(jdbcTemplate,
                        "SELECT available_balance FROM wallets WHERE id = ?",
                        fixture.walletId()));
                // 순이동 금액과 관계없이 은행·지갑을 합친 전체 금액은 보존되어야 한다.
                assertEquals(1_010_000L,
                        value(jdbcTemplate,
                                "SELECT balance FROM mock_bank_accounts WHERE id = ?",
                                fixture.accountId())
                                + value(jdbcTemplate,
                                "SELECT available_balance FROM wallets WHERE id = ?",
                                fixture.walletId()));
            } finally {
                start.countDown();
                executor.shutdownNow();
                assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
                deleteFundingFixture(jdbcTemplate, fixture);
            }
        }
    }

    @Test
    @Timeout(15)
    void concurrentDuplicateCanReadCommittedClaimWithForShare() throws Exception {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(RootConfig.class)) {
            FundingMapper fundingMapper = context.getBean(FundingMapper.class);
            PlatformTransactionManager transactionManager =
                    context.getBean(PlatformTransactionManager.class);
            JdbcTemplate jdbcTemplate =
                    new JdbcTemplate(context.getBean(DataSource.class));
            FundingFixture fixture = createFundingFixture(jdbcTemplate);
            String key = "IT-" + UUID.randomUUID();
            CountDownLatch ownerInserted = new CountDownLatch(1);
            CountDownLatch duplicateStarted = new CountDownLatch(1);
            CountDownLatch releaseOwner = new CountDownLatch(1);
            ExecutorService executor = Executors.newFixedThreadPool(2);

            try {
                Future<Long> owner = executor.submit(() ->
                        new TransactionTemplate(transactionManager).execute(status -> {
                            FundingOrderParam param =
                                    orderParam(fixture.userId(), fixture.accountId(), key);
                            assertEquals(1, fundingMapper.insertFundingOrder(param));
                            assertNotNull(param.getId());
                            ownerInserted.countDown();
                            await(releaseOwner);
                            return param.getId();
                        })
                );

                Future<FundingOrder> duplicate = executor.submit(() -> {
                    await(ownerInserted);
                    return new TransactionTemplate(transactionManager).execute(status -> {
                        duplicateStarted.countDown();
                        try {
                            fundingMapper.insertFundingOrder(orderParam(
                                    fixture.userId(), fixture.accountId(), key));
                            throw new AssertionError("중복 멱등 키 INSERT가 성공했습니다.");
                        } catch (DuplicateKeyException expected) {
                            return fundingMapper.findByIdempotencyKeyForShare(key);
                        }
                    });
                });

                assertTrue(duplicateStarted.await(3, TimeUnit.SECONDS));
                assertThrows(
                        TimeoutException.class,
                        () -> duplicate.get(200, TimeUnit.MILLISECONDS)
                );
                releaseOwner.countDown();

                Long ownerId = owner.get(5, TimeUnit.SECONDS);
                FundingOrder replayed = duplicate.get(5, TimeUnit.SECONDS);
                assertNotNull(replayed);
                assertEquals(ownerId, replayed.getId());
                assertEquals(fixture.userId(), replayed.getEmployerId());
                assertEquals(fixture.accountId(), replayed.getLinkedAccountId());
                assertEquals(1L, replayed.getExpectedAmount());
            } finally {
                releaseOwner.countDown();
                executor.shutdownNow();
                assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
                jdbcTemplate.update(
                        "DELETE FROM funding_orders WHERE idempotency_key = ?",
                        key
                );
                deleteFundingFixture(jdbcTemplate, fixture);
            }
        }
    }

    @Test
    @Timeout(20)
    void ownerRollbackLetsTwoWaitersRetryAndCompleteOnce() throws Exception {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(RootConfig.class)) {
            JdbcTemplate jdbcTemplate =
                    new JdbcTemplate(context.getBean(DataSource.class));
            FundingService fundingService = context.getBean(FundingService.class);
            FundingMapper fundingMapper = context.getBean(FundingMapper.class);
            PlatformTransactionManager transactionManager =
                    context.getBean(PlatformTransactionManager.class);
            FundingFixture fixture = createFundingFixture(jdbcTemplate);
            ExecutorService executor = Executors.newFixedThreadPool(3);
            CountDownLatch ownerInserted = new CountDownLatch(1);
            CountDownLatch releaseOwner = new CountDownLatch(1);
            CountDownLatch waitersStarted = new CountDownLatch(2);
            FundingCommand command = FundingCommand.builder()
                    .employerId(fixture.userId())
                    .bankCode(FIXTURE_BANK_CODE)
                    .accountNo(fixture.accountNo())
                    .pin(FIXTURE_PIN)
                    .amount(1_000L)
                    .idempotencyKey(fixture.idempotencyKey())
                    .build();

            try {
                Future<Void> owner = executor.submit(() ->
                        new TransactionTemplate(transactionManager).execute(status -> {
                            FundingOrderParam param = FundingOrderParam.builder()
                                    .employerId(fixture.userId())
                                    .linkedAccountId(fixture.accountId())
                                    .expectedAmount(1_000L)
                                    .idempotencyKey(fixture.idempotencyKey())
                                    .build();
                            assertEquals(1, fundingMapper.insertFundingOrder(param));
                            ownerInserted.countDown();
                            await(releaseOwner);
                            status.setRollbackOnly();
                            return null;
                        })
                );
                assertTrue(ownerInserted.await(5, TimeUnit.SECONDS));

                Future<FundingResult> first = executor.submit(() -> {
                    waitersStarted.countDown();
                    return fundingService.fund(command);
                });
                Future<FundingResult> second = executor.submit(() -> {
                    waitersStarted.countDown();
                    return fundingService.fund(command);
                });
                assertTrue(waitersStarted.await(5, TimeUnit.SECONDS));
                assertThrows(
                        TimeoutException.class,
                        () -> first.get(200, TimeUnit.MILLISECONDS)
                );
                assertThrows(
                        TimeoutException.class,
                        () -> second.get(200, TimeUnit.MILLISECONDS)
                );

                releaseOwner.countDown();
                owner.get(5, TimeUnit.SECONDS);
                FundingResult firstResult = first.get(8, TimeUnit.SECONDS);
                FundingResult secondResult = second.get(8, TimeUnit.SECONDS);

                assertTrue(firstResult.isReplayed() ^ secondResult.isReplayed());
                assertEquals(firstResult.getFundingOrderId(), secondResult.getFundingOrderId());
                assertEquals(1, count(jdbcTemplate,
                        "SELECT COUNT(*) FROM funding_orders WHERE idempotency_key = ?",
                        fixture.idempotencyKey()));
                assertEquals(1, count(jdbcTemplate,
                        "SELECT COUNT(*) FROM mock_bank_transactions WHERE account_id = ?",
                        fixture.accountId()));
                assertEquals(1, count(jdbcTemplate,
                        "SELECT COUNT(*) FROM wallet_transactions WHERE wallet_id = ?",
                        fixture.walletId()));
                assertEquals(999_000L, value(jdbcTemplate,
                        "SELECT balance FROM mock_bank_accounts WHERE id = ?",
                        fixture.accountId()));
                assertEquals(1_000L, value(jdbcTemplate,
                        "SELECT available_balance FROM wallets WHERE id = ?",
                        fixture.walletId()));
            } finally {
                releaseOwner.countDown();
                executor.shutdownNow();
                assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
                deleteFundingFixture(jdbcTemplate, fixture);
            }
        }
    }

    @Test
    void downstreamLedgerDuplicateRollsBackAllFundingMutations() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(RootConfig.class)) {
            JdbcTemplate jdbcTemplate =
                    new JdbcTemplate(context.getBean(DataSource.class));
            FundingService fundingService = context.getBean(FundingService.class);
            FundingFixture fixture = createFundingFixture(jdbcTemplate);
            String ledgerKey =
                    WalletIdempotencyKeys.funding(fixture.idempotencyKey());

            try {
                jdbcTemplate.update(
                        "INSERT INTO wallet_transactions"
                                + " (wallet_id, work_case_id, transaction_type, amount,"
                                + " available_before, available_after, locked_before,"
                                + " locked_after, reference_type, reference_id,"
                                + " idempotency_key)"
                                + " VALUES (?, NULL, 'FUNDING', 1, 0, 1, 0, 0,"
                                + " 'FUNDING_ORDER', 999999, ?)",
                        fixture.walletId(),
                        ledgerKey
                );
                FundingCommand command = FundingCommand.builder()
                        .employerId(fixture.userId())
                        .bankCode(FIXTURE_BANK_CODE)
                        .accountNo(fixture.accountNo())
                        .pin(FIXTURE_PIN)
                        .amount(1_000L)
                        .idempotencyKey(fixture.idempotencyKey())
                        .build();

                assertThrows(
                        DuplicateKeyException.class,
                        () -> fundingService.fund(command)
                );

                assertEquals(0, count(jdbcTemplate,
                        "SELECT COUNT(*) FROM funding_orders WHERE idempotency_key = ?",
                        fixture.idempotencyKey()));
                assertEquals(0, count(jdbcTemplate,
                        "SELECT COUNT(*) FROM mock_bank_transactions WHERE account_id = ?",
                        fixture.accountId()));
                assertEquals(1_000_000L, value(jdbcTemplate,
                        "SELECT balance FROM mock_bank_accounts WHERE id = ?",
                        fixture.accountId()));
                assertEquals(0L, value(jdbcTemplate,
                        "SELECT available_balance FROM wallets WHERE id = ?",
                        fixture.walletId()));
                assertEquals(1, count(jdbcTemplate,
                        "SELECT COUNT(*) FROM wallet_transactions WHERE idempotency_key = ?",
                        ledgerKey));
            } finally {
                deleteFundingFixture(jdbcTemplate, fixture);
            }
        }
    }

    /**
     * 잘못된 PIN은 자금 이동 전 실패이므로 멱등 키를 소모하지 않고, 같은 키로 PIN만 고쳐
     * 재시도할 수 있어야 한다(DEC-IDEMPOTENCY-STORAGE).
     */
    @Test
    void wrongPinDoesNotConsumeIdempotencyKeyAndAllowsRetry() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(RootConfig.class)) {
            JdbcTemplate jdbcTemplate =
                    new JdbcTemplate(context.getBean(DataSource.class));
            FundingService fundingService = context.getBean(FundingService.class);
            FundingFixture fixture = createFundingFixture(jdbcTemplate);

            try {
                assertThrows(
                        BankAccountForbiddenException.class,
                        () -> fundingService.fund(fundingCommand(fixture, "9999", 1_000L))
                );

                // 검증 실패는 claim으로 저장되지 않아야 하고 자금도 움직이지 않아야 한다.
                assertEquals(0, count(jdbcTemplate,
                        "SELECT COUNT(*) FROM funding_orders WHERE idempotency_key = ?",
                        fixture.idempotencyKey()));
                assertEquals(0, count(jdbcTemplate,
                        "SELECT COUNT(*) FROM mock_bank_transactions WHERE account_id = ?",
                        fixture.accountId()));
                assertEquals(1_000_000L, value(jdbcTemplate,
                        "SELECT balance FROM mock_bank_accounts WHERE id = ?",
                        fixture.accountId()));
                assertEquals(0L, value(jdbcTemplate,
                        "SELECT available_balance FROM wallets WHERE id = ?",
                        fixture.walletId()));

                // 같은 멱등 키로 올바른 PIN 재시도가 성공해야 한다.
                FundingResult retried =
                        fundingService.fund(fundingCommand(fixture, FIXTURE_PIN, 1_000L));

                assertFalse(retried.isReplayed());
                assertEquals("COMPLETED", retried.getStatus());
                assertEquals(1_000L, value(jdbcTemplate,
                        "SELECT available_balance FROM wallets WHERE id = ?",
                        fixture.walletId()));
                assertEquals(999_000L, value(jdbcTemplate,
                        "SELECT balance FROM mock_bank_accounts WHERE id = ?",
                        fixture.accountId()));
            } finally {
                deleteFundingFixture(jdbcTemplate, fixture);
            }
        }
    }

    /**
     * 비활성 계좌는 PIN이 맞아도 계좌 미존재·PIN 불일치와 구분 없는 승인 오류로 거부한다
     * (DEC-BANK-ERROR-CATALOG).
     */
    @Test
    void inactiveAccountRejectsFundingWithoutRevealingState() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(RootConfig.class)) {
            JdbcTemplate jdbcTemplate =
                    new JdbcTemplate(context.getBean(DataSource.class));
            FundingService fundingService = context.getBean(FundingService.class);
            FundingFixture fixture = createFundingFixture(jdbcTemplate);

            try {
                for (String status : new String[]{"BLOCKED", "CLOSED"}) {
                    jdbcTemplate.update(
                            "UPDATE mock_bank_accounts SET status = ? WHERE id = ?",
                            status, fixture.accountId());

                    BankAccountForbiddenException blocked = assertThrows(
                            BankAccountForbiddenException.class,
                            () -> fundingService.fund(
                                    fundingCommand(fixture, FIXTURE_PIN, 1_000L))
                    );
                    // 미존재·PIN 불일치와 같은 문구여야 상태를 추론할 수 없다.
                    assertEquals("계좌를 사용할 수 없습니다.", blocked.getMessage());

                    assertEquals(0, count(jdbcTemplate,
                            "SELECT COUNT(*) FROM funding_orders WHERE idempotency_key = ?",
                            fixture.idempotencyKey()));
                    assertEquals(1_000_000L, value(jdbcTemplate,
                            "SELECT balance FROM mock_bank_accounts WHERE id = ?",
                            fixture.accountId()));
                    assertEquals(0L, value(jdbcTemplate,
                            "SELECT available_balance FROM wallets WHERE id = ?",
                            fixture.walletId()));
                }

                // 존재하지 않는 계좌번호도 같은 오류로 거부한다.
                BankAccountForbiddenException missing = assertThrows(
                        BankAccountForbiddenException.class,
                        () -> fundingService.fund(FundingCommand.builder()
                                .employerId(fixture.userId())
                                .bankCode(FIXTURE_BANK_CODE)
                                .accountNo("00000000000000")
                                .pin(FIXTURE_PIN)
                                .amount(1_000L)
                                .idempotencyKey(fixture.idempotencyKey() + "-X")
                                .build())
                );
                assertEquals("계좌를 사용할 수 없습니다.", missing.getMessage());
            } finally {
                jdbcTemplate.update(
                        "UPDATE mock_bank_accounts SET status = 'ACTIVE' WHERE id = ?",
                        fixture.accountId());
                deleteFundingFixture(jdbcTemplate, fixture);
            }
        }
    }

    /**
     * 완료된 충전의 Replay는 이후 계좌가 비활성으로 바뀌어도 저장 결과를 그대로 반환한다
     * (DEC-IDEMPOTENCY-STORAGE).
     */
    @Test
    void completedFundingReplayIgnoresLaterBlockedAccount() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(RootConfig.class)) {
            JdbcTemplate jdbcTemplate =
                    new JdbcTemplate(context.getBean(DataSource.class));
            FundingService fundingService = context.getBean(FundingService.class);
            FundingFixture fixture = createFundingFixture(jdbcTemplate);

            try {
                FundingResult original =
                        fundingService.fund(fundingCommand(fixture, FIXTURE_PIN, 1_000L));
                assertFalse(original.isReplayed());

                jdbcTemplate.update(
                        "UPDATE mock_bank_accounts SET status = 'BLOCKED' WHERE id = ?",
                        fixture.accountId());

                FundingResult replayed =
                        fundingService.fund(fundingCommand(fixture, FIXTURE_PIN, 1_000L));

                assertTrue(replayed.isReplayed());
                assertEquals(original.getFundingOrderId(), replayed.getFundingOrderId());
                assertEquals(
                        original.getBankTransactionId(), replayed.getBankTransactionId());
                assertEquals(1, count(jdbcTemplate,
                        "SELECT COUNT(*) FROM mock_bank_transactions WHERE account_id = ?",
                        fixture.accountId()));
                assertEquals(1_000L, value(jdbcTemplate,
                        "SELECT available_balance FROM wallets WHERE id = ?",
                        fixture.walletId()));
            } finally {
                jdbcTemplate.update(
                        "UPDATE mock_bank_accounts SET status = 'ACTIVE' WHERE id = ?",
                        fixture.accountId());
                deleteFundingFixture(jdbcTemplate, fixture);
            }
        }
    }

    private FundingCommand fundingCommand(FundingFixture fixture, String pin, long amount) {
        return FundingCommand.builder()
                .employerId(fixture.userId())
                .bankCode(FIXTURE_BANK_CODE)
                .accountNo(fixture.accountNo())
                .pin(pin)
                .amount(amount)
                .idempotencyKey(fixture.idempotencyKey())
                .build();
    }

    @Test
    void snapshotMappingAndMandatoryGatewayContractWorkOnMySql() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(RootConfig.class)) {
            DataSource dataSource = context.getBean(DataSource.class);
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            FundingFixture fixture = createFundingFixture(jdbcTemplate);
            WalletMapper walletMapper = context.getBean(WalletMapper.class);
            BankTransferGateway gateway = context.getBean(BankTransferGateway.class);
            PlatformTransactionManager transactionManager =
                    context.getBean(PlatformTransactionManager.class);
            TransactionTemplate transaction = new TransactionTemplate(transactionManager);

            try {
                assertThrows(
                        IllegalTransactionStateException.class,
                        () -> gateway.preflight(BankAccountPreflightCommand.builder()
                                .accountId(fixture.accountId())
                                .pin(FIXTURE_PIN)
                                .build())
                );

                WalletBalanceSnapshot snapshot = transaction.execute(status -> {
                    WalletBalanceSnapshot locked =
                            walletMapper.getWalletSnapshotForUpdate(fixture.userId());
                    gateway.preflight(BankAccountPreflightCommand.builder()
                            .accountId(fixture.accountId())
                            .pin(FIXTURE_PIN)
                            .build());
                    status.setRollbackOnly();
                    return locked;
                });

                assertNotNull(snapshot);
                assertNotNull(snapshot.getWalletId());
                assertEquals(fixture.userId(), snapshot.getUserId());
                assertNotNull(snapshot.getAvailableBalance());
                assertNotNull(snapshot.getLockedBalance());
            } finally {
                deleteFundingFixture(jdbcTemplate, fixture);
            }
        }
    }

    private FundingOrderParam orderParam(Long employerId, Long accountId, String key) {
        return FundingOrderParam.builder()
                .employerId(employerId)
                .linkedAccountId(accountId)
                .expectedAmount(1L)
                .idempotencyKey(key)
                .build();
    }

    private FundingFixture createFundingFixture(JdbcTemplate jdbcTemplate) {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String loginId = "it_owner_" + token;
        String email = loginId + "@example.invalid";
        String fintechUseNumber = "IT-FIN-" + token;
        String accountNumber = "99" + token;
        String idempotencyKey = "IT-FUND-" + token;

        jdbcTemplate.update(
                "INSERT INTO users"
                        + " (login_id, email, password_hash, name, role, status)"
                        + " VALUES (?, ?, 'integration-test', '통합 테스트', 'OWNER', 'ACTIVE')",
                loginId,
                email
        );
        Long userId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE login_id = ?",
                Long.class,
                loginId
        );
        jdbcTemplate.update(
                "INSERT INTO wallets"
                        + " (user_id, currency, available_balance, locked_balance)"
                        + " VALUES (?, 'KRW', 0, 0)",
                userId
        );
        Long walletId = jdbcTemplate.queryForObject(
                "SELECT id FROM wallets WHERE user_id = ?",
                Long.class,
                userId
        );
        jdbcTemplate.update(
                "INSERT INTO mock_bank_accounts"
                        + " (bank_code, mock_account_number, pin,"
                        + " mock_fintech_use_num, currency, balance,"
                        + " available_amount, status)"
                        + " VALUES ('999', ?, '0000', ?, 'KRW',"
                        + " 1000000, 1000000, 'ACTIVE')",
                accountNumber,
                fintechUseNumber
        );
        Long accountId = jdbcTemplate.queryForObject(
                "SELECT id FROM mock_bank_accounts WHERE mock_fintech_use_num = ?",
                Long.class,
                fintechUseNumber
        );
        return new FundingFixture(
                userId, walletId, accountId, accountNumber, idempotencyKey
        );
    }

    private void deleteFundingFixture(
            JdbcTemplate jdbcTemplate, FundingFixture fixture) {
        jdbcTemplate.update(
                "DELETE FROM wallet_transactions WHERE wallet_id = ?",
                fixture.walletId()
        );
        jdbcTemplate.update(
                "DELETE FROM funding_orders WHERE employer_id = ?",
                fixture.userId()
        );
        jdbcTemplate.update(
                "DELETE FROM withdrawal_requests WHERE user_id = ?",
                fixture.userId()
        );
        jdbcTemplate.update(
                "DELETE FROM mock_bank_transactions WHERE account_id = ?",
                fixture.accountId()
        );
        jdbcTemplate.update(
                "DELETE FROM mock_bank_accounts WHERE id = ?",
                fixture.accountId()
        );
        jdbcTemplate.update(
                "DELETE FROM wallets WHERE id = ?",
                fixture.walletId()
        );
        jdbcTemplate.update(
                "DELETE FROM users WHERE id = ?",
                fixture.userId()
        );
    }

    private int count(JdbcTemplate jdbcTemplate, String sql, Object argument) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, argument);
        return count == null ? 0 : count;
    }

    private long value(JdbcTemplate jdbcTemplate, String sql, Object argument) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, argument);
        if (value == null) {
            throw new IllegalStateException("통합 테스트 잔액을 조회할 수 없습니다.");
        }
        return value;
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("동시성 테스트 대기 시간이 초과되었습니다.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("동시성 테스트가 중단되었습니다.", e);
        }
    }

    private static final String FIXTURE_BANK_CODE = "999";
    private static final String FIXTURE_PIN = "0000";

    private record FundingFixture(
            Long userId,
            Long walletId,
            Long accountId,
            String accountNo,
            String idempotencyKey) {
    }
}
