package com.itranswarp.exchange;

public record ApiErrorResponse(ApiError error, String data, String message) {

}
