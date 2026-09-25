package com.marketplace.customers.shared;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) { super(message); }
}
