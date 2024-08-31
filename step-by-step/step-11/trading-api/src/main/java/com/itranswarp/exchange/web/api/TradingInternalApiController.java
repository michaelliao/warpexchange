package com.itranswarp.exchange.web.api;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.itranswarp.exchange.bean.TransferRequestBean;
import com.itranswarp.exchange.enums.UserType;
import com.itranswarp.exchange.message.event.TransferEvent;
import com.itranswarp.exchange.service.SendEventService;
import com.itranswarp.exchange.support.AbstractApiController;

@RestController
@RequestMapping("/internal")
public class TradingInternalApiController extends AbstractApiController {

    @Autowired
    SendEventService sendEventService;

    /**
     * 处理一个转账请求，可重复调用，重复发送消息，根据uniqueId去重，仅定序一次。
     */
    @PostMapping("/transfer")
    public Map<String, Boolean> transferIn(@RequestBody TransferRequestBean transferRequest) {
        logger.info("transfer request: transferId={}, fromUserId={}, toUserId={}, asset={}, amount={}",
                transferRequest.transferId, transferRequest.fromUserId, transferRequest.toUserId, transferRequest.asset,
                transferRequest.amount);
        transferRequest.validate();

        var message = new TransferEvent();
        // IMPORTANT: set uniqueId to make sure the message will be sequenced only once:
        message.uniqueId = transferRequest.transferId;
        message.fromUserId = transferRequest.fromUserId;
        message.toUserId = transferRequest.toUserId;
        message.asset = transferRequest.asset;
        message.amount = transferRequest.amount;
        message.sufficient = transferRequest.fromUserId.longValue() != UserType.DEBT.getInternalUserId();
        this.sendEventService.sendMessage(message);
        logger.info("transfer event sent: {}", message);
        return Map.of("result", Boolean.TRUE);
    }
}
