package com.gighub.wallet.mapper;

import com.gighub.wallet.dto.FundingOrder;
import com.gighub.wallet.mapper.param.FundingOrderParam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FundingMapper {

    FundingOrder findByIdempotencyKeyForShare(@Param("idempotencyKey") String idempotencyKey);

    int insertFundingOrder(FundingOrderParam param);

    int completeFundingOrder(@Param("id") Long id,
                             @Param("transferredAmount") Long transferredAmount,
                             @Param("bankTransactionId") Long bankTransactionId);
}
