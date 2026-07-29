package com.gighub.wallet.mapper;

import com.gighub.wallet.dto.WithdrawalOrder;
import com.gighub.wallet.mapper.param.WithdrawalOrderParam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WithdrawalMapper {

    WithdrawalOrder findByIdempotencyKeyForShare(
            @Param("idempotencyKey") String idempotencyKey);

    int insertWithdrawalRequest(WithdrawalOrderParam param);

    int completeWithdrawalRequest(@Param("id") Long id,
                                  @Param("bankTransactionId") Long bankTransactionId);
}
