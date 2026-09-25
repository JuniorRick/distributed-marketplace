package com.marketplace.customers.customer;

import com.marketplace.customers.customer.api.CreateCustomerRequest;
import com.marketplace.customers.customer.api.CustomerResponse;
import com.marketplace.customers.customer.api.UpdateContactRequest;
import com.marketplace.customers.customer.api.UpdateNotificationPreferencesRequest;
import com.marketplace.customers.customer.messaging.CustomerMessagingConfiguration;
import com.marketplace.customers.customer.messaging.CustomerProfileEvent;
import com.marketplace.customers.customer.repository.Customer;
import com.marketplace.customers.customer.repository.CustomerRepository;
import com.marketplace.customers.outbox.OutboxService;
import com.marketplace.customers.shared.ConflictException;
import com.marketplace.customers.shared.NotFoundException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final OutboxService outboxService;

    public CustomerService(CustomerRepository customerRepository, OutboxService outboxService) {
        this.customerRepository = customerRepository;
        this.outboxService = outboxService;
    }

    @Transactional
    public CustomerResponse create(CreateCustomerRequest request) {
        if (customerRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("A customer with this email already exists");
        }
        Customer customer = Customer.create(
            request.email(), request.phone(), request.emailNotificationsEnabled(), request.smsNotificationsEnabled()
        );
        try {
            customerRepository.saveAndFlush(customer);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("A customer with this email already exists");
        }
        enqueue(customer, "CustomerCreatedEvent.v1", CustomerMessagingConfiguration.CUSTOMER_CREATED_EVENT_ROUTING_KEY);
        return CustomerResponse.from(customer);
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(UUID customerId) {
        return CustomerResponse.from(find(customerId));
    }

    @Transactional
    public CustomerResponse updateContact(UUID customerId, UpdateContactRequest request) {
        Customer customer = find(customerId);
        if (customerRepository.existsByEmailIgnoreCaseAndPublicIdNot(request.email(), customerId)) {
            throw new ConflictException("A customer with this email already exists");
        }
        customer.updateContact(request.email(), request.phone());
        customerRepository.flush();
        enqueue(customer, "CustomerContactUpdatedEvent.v1", CustomerMessagingConfiguration.CUSTOMER_CONTACT_UPDATED_EVENT_ROUTING_KEY);
        return CustomerResponse.from(customer);
    }

    @Transactional
    public CustomerResponse updateNotificationPreferences(
        UUID customerId,
        UpdateNotificationPreferencesRequest request
    ) {
        Customer customer = find(customerId);
        customer.updateNotificationPreferences(request.emailEnabled(), request.smsEnabled());
        customerRepository.flush();
        enqueue(
            customer,
            "CustomerNotificationPreferencesChangedEvent.v1",
            CustomerMessagingConfiguration.CUSTOMER_PREFERENCES_UPDATED_EVENT_ROUTING_KEY
        );
        return CustomerResponse.from(customer);
    }

    private Customer find(UUID customerId) {
        return customerRepository.findByPublicId(customerId)
            .orElseThrow(() -> new NotFoundException("Customer %s was not found".formatted(customerId)));
    }

    private void enqueue(Customer customer, String eventType, String routingKey) {
        UUID eventId = UUID.randomUUID();
        CustomerProfileEvent event = new CustomerProfileEvent(
            eventId,
            customer.getPublicId(),
            customer.getEmail(),
            customer.getPhone(),
            customer.isEmailNotificationsEnabled(),
            customer.isSmsNotificationsEnabled(),
            customer.getEventVersion(),
            Instant.now()
        );
        outboxService.enqueue(eventId, eventType, CustomerMessagingConfiguration.EXCHANGE, routingKey, event);
    }
}
