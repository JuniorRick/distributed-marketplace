package com.marketplace.customers.customer.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateContactRequest(
    @NotBlank @Email @Size(max = 320) String email,
    @Size(max = 32) String phone
) {
}
