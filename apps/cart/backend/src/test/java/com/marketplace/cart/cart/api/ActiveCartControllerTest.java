package com.marketplace.cart.cart.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.marketplace.cart.cart.CartService;
import com.marketplace.cart.cart.repository.Cart;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ActiveCartControllerTest {
    @Mock
    CartService cartService;

    @Spy
    CartApiMapper cartApiMapper = new CartApiMapper();

    @InjectMocks
    CartController controller;

    @Test
    void looksUpTheCartUsingTheAuthenticatedSubject() {
        UUID customerId = UUID.randomUUID();
        Cart cart = new Cart(customerId);
        when(cartService.findActiveCart(customerId)).thenReturn(Optional.of(cart));

        var response = controller.getActiveCart(jwt(customerId));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().customerId()).isEqualTo(customerId);
        verify(cartService).findActiveCart(customerId);
    }

    @Test
    void returnsNoContentWhenNoActiveCartExists() {
        UUID customerId = UUID.randomUUID();
        when(cartService.findActiveCart(customerId)).thenReturn(Optional.empty());

        var response = controller.getActiveCart(jwt(customerId));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void requiresAnIdentityEvenWhenTheSecurityFilterIsDisabled() {
        assertThatThrownBy(() -> controller.getActiveCart(null))
            .isInstanceOfSatisfying(ResponseStatusException.class,
                exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
        verifyNoInteractions(cartService);
    }

    private static Jwt jwt(UUID subject) {
        return Jwt.withTokenValue("test-token")
            .header("alg", "RS256")
            .subject(subject.toString())
            .build();
    }
}
