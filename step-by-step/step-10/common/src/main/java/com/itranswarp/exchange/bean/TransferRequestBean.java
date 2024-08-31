package com.itranswarp.exchange.bean;

import java.math.BigDecimal;

import com.itranswarp.exchange.ApiError;
import com.itranswarp.exchange.ApiException;
import com.itranswarp.exchange.enums.AssetEnum;
import com.itranswarp.exchange.util.IdUtil;

/**
 * Transfer request.
 */
public class TransferRequestBean implements ValidatableBean {

    public String transferId;

    public AssetEnum asset;

    public Long fromUserId;

    public Long toUserId;

    public BigDecimal amount;

    @Override
    public void validate() {
        if (!IdUtil.isValidStringId(transferId)) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "transferId", "Must specify a unique transferId.");
        }
        if (fromUserId == null || fromUserId.longValue() <= 0) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "fromUserId", "Must specify fromUserId.");
        }
        if (toUserId == null || toUserId.longValue() <= 0) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "toUserId", "Must specify toUserId.");
        }
        if (fromUserId.longValue() == toUserId.longValue()) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "toUserId", "Must be different with fromUserId.");
        }
        if (asset == null) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "asset", "Must specify asset.");
        }
        if (amount == null) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "amount", "Must specify amount.");
        }
        amount = amount.setScale(AssetEnum.SCALE);
        if (amount.signum() <= 0) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "amount", "Must specify positive amount.");
        }
    }
}
