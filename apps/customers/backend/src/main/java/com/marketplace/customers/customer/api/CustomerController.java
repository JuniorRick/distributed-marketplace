package com.marketplace.customers.customer.api;

import com.marketplace.customers.customer.CustomerService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    ResponseEntity<CustomerResponse> create(@Valid @RequestBody CreateCustomerRequest request) {
        CustomerResponse response = customerService.create(request);
        return ResponseEntity.created(URI.create("/api/customers/" + response.id())).body(response);
    }

    @PostMapping("/me")
    CustomerResponse provisionCurrentCustomer(@AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("The authenticated identity does not contain an email claim");
        }
        return customerService.provision(
            UUID.fromString(jwt.getSubject()),
            email,
            jwt.getClaimAsString("phone_number")
        );
    }

    @GetMapping("/me")
    CustomerResponse getCurrentCustomer(@AuthenticationPrincipal Jwt jwt) {
        return customerService.get(UUID.fromString(jwt.getSubject()));
    }

    @GetMapping("/{customerId}")
    CustomerResponse get(@PathVariable UUID customerId) {
        return customerService.get(customerId);
    }

    @PutMapping("/{customerId}/contact")
    CustomerResponse updateContact(
        @PathVariable UUID customerId,
        @Valid @RequestBody UpdateContactRequest request
    ) {
        return customerService.updateContact(customerId, request);
    }

    @PutMapping("/{customerId}/notification-preferences")
    CustomerResponse updateNotificationPreferences(
        @PathVariable UUID customerId,
        @Valid @RequestBody UpdateNotificationPreferencesRequest request
    ) {
        return customerService.updateNotificationPreferences(customerId, request);
    }
}
