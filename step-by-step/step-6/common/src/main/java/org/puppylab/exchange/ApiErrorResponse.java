package org.puppylab.exchange;

public record ApiErrorResponse(ApiError error, String data, String message) {

}
