package com.marketplace.customers.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketplace.customers.customer.api.CreateCustomerRequest;
import com.marketplace.customers.customer.messaging.CustomerMessagingConfiguration;
import com.marketplace.customers.customer.messaging.CustomerProfileEvent;
import com.marketplace.customers.customer.repository.Customer;
import com.marketplace.customers.customer.repository.CustomerRepository;
import com.marketplace.customers.outbox.OutboxService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private OutboxService outboxService;

    @Test
    void createsCustomerAndEventInSameServiceTransaction() {
        when(customerRepository.saveAndFlush(any(Customer.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        CustomerService service = new CustomerService(customerRepository, outboxService);

        var response = service.create(new CreateCustomerRequest("USER@Example.com", "+40123456789", true, false));

        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.version()).isEqualTo(1);
        ArgumentCaptor<CustomerProfileEvent> event = ArgumentCaptor.forClass(CustomerProfileEvent.class);
        verify(outboxService).enqueue(
            any(),
            eq("CustomerCreatedEvent.v1"),
            eq(CustomerMessagingConfiguration.EXCHANGE),
            eq(CustomerMessagingConfiguration.CUSTOMER_CREATED_EVENT_ROUTING_KEY),
            event.capture()
        );
        assertThat(event.getValue().customerId()).isEqualTo(response.id());
        assertThat(event.getValue().email()).isEqualTo("user@example.com");
        assertThat(event.getValue().version()).isEqualTo(1);
    }
}
