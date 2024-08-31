package com.itranswarp.exchange.assets;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;

import com.itranswarp.exchange.enums.AssetEnum;
import com.itranswarp.exchange.support.LoggerSupport;

@Component
public class AssetService extends LoggerSupport {

    // UserId -> Map(AssetEnum -> Assets[available/frozen])
    final ConcurrentMap<Long, ConcurrentMap<AssetEnum, Asset>> userAssets = new ConcurrentHashMap<>();

    public Asset getAsset(Long userId, AssetEnum assetId) {
        ConcurrentMap<AssetEnum, Asset> assets = userAssets.get(userId);
        if (assets == null) {
            return null;
        }
        return assets.get(assetId);
    }

    public Map<AssetEnum, Asset> getAssets(Long userId) {
        Map<AssetEnum, Asset> assets = userAssets.get(userId);
        if (assets == null) {
            return Map.of();
        }
        return assets;
    }

    public ConcurrentMap<Long, ConcurrentMap<AssetEnum, Asset>> getUserAssets() {
        return this.userAssets;
    }

    public boolean tryFreeze(Long userId, AssetEnum assetId, BigDecimal amount) {
        boolean ok = tryTransfer(Transfer.AVAILABLE_TO_FROZEN, userId, userId, assetId, amount, true);
        if (ok && logger.isDebugEnabled()) {
            logger.debug("freezed user {}, asset {}, amount {}", userId, assetId, amount);
        }
        return ok;
    }

    public void unfreeze(Long userId, AssetEnum assetId, BigDecimal amount) {
        if (!tryTransfer(Transfer.FROZEN_TO_AVAILABLE, userId, userId, assetId, amount, true)) {
            throw new RuntimeException(
                    "Unfreeze failed for user " + userId + ", asset = " + assetId + ", amount = " + amount);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("unfreezed user {}, asset {}, amount {}", userId, assetId, amount);
        }
    }

    public void transfer(Transfer type, Long fromUser, Long toUser, AssetEnum assetId, BigDecimal amount) {
        if (!tryTransfer(type, fromUser, toUser, assetId, amount, true)) {
            throw new RuntimeException("Transfer failed for " + type + ", from user " + fromUser + " to user " + toUser
                    + ", asset = " + assetId + ", amount = " + amount);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("transfer asset {}, from {} => {}, amount {}", assetId, fromUser, toUser, amount);
        }
    }

    public boolean tryTransfer(Transfer type, Long fromUser, Long toUser, AssetEnum assetId, BigDecimal amount,
            boolean checkBalance) {
        if (amount.signum() == 0) {
            return true;
        }
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Negative amount");
        }
        Asset fromAsset = getAsset(fromUser, assetId);
        if (fromAsset == null) {
            fromAsset = initAssets(fromUser, assetId);
        }
        Asset toAsset = getAsset(toUser, assetId);
        if (toAsset == null) {
            toAsset = initAssets(toUser, assetId);
        }
        return switch (type) {
        case AVAILABLE_TO_AVAILABLE -> {
            // 需要检查余额且余额不足:
            if (checkBalance && fromAsset.available.compareTo(amount) < 0) {
                yield false;
            }
            fromAsset.available = fromAsset.available.subtract(amount);
            toAsset.available = toAsset.available.add(amount);
            yield true;
        }
        case AVAILABLE_TO_FROZEN -> {
            // 需要检查余额且余额不足:
            if (checkBalance && fromAsset.available.compareTo(amount) < 0) {
                yield false;
            }
            fromAsset.available = fromAsset.available.subtract(amount);
            toAsset.frozen = toAsset.frozen.add(amount);
            yield true;
        }
        case FROZEN_TO_AVAILABLE -> {
            // 需要检查余额且余额不足:
            if (checkBalance && fromAsset.frozen.compareTo(amount) < 0) {
                yield false;
            }
            fromAsset.frozen = fromAsset.frozen.subtract(amount);
            toAsset.available = toAsset.available.add(amount);
            yield true;
        }
        default -> {
            throw new IllegalArgumentException("invalid type: " + type);
        }
        };
    }

    private Asset initAssets(Long userId, AssetEnum assetId) {
        ConcurrentMap<AssetEnum, Asset> map = userAssets.get(userId);
        if (map == null) {
            map = new ConcurrentHashMap<>();
            userAssets.put(userId, map);
        }
        Asset zeroAsset = new Asset();
        map.put(assetId, zeroAsset);
        return zeroAsset;
    }

    public void debug() {
        System.out.println("---------- assets ----------");
        List<Long> userIds = new ArrayList<>(userAssets.keySet());
        Collections.sort(userIds);
        for (Long userId : userIds) {
            System.out.println("  user " + userId + " ----------");
            Map<AssetEnum, Asset> assets = userAssets.get(userId);
            List<AssetEnum> assetIds = new ArrayList<>(assets.keySet());
            Collections.sort(assetIds);
            for (AssetEnum assetId : assetIds) {
                System.out.println("    " + assetId + ": " + assets.get(assetId));
            }
        }
        System.out.println("---------- // assets ----------");
    }
}
