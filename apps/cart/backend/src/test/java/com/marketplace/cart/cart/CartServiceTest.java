package com.marketplace.cart.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketplace.cart.catalog.CatalogClient;
import com.marketplace.cart.cart.repository.Cart;
import com.marketplace.cart.cart.repository.CartRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CatalogClient catalogClient;

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private CartService cartService;

    @Test
    void addsAnAuthoritativeCatalogSnapshotToTheCart() {
        UUID cartId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Cart cart = new Cart(UUID.randomUUID());
        var product = new CatalogClient.ProductSnapshot(
                productId,
                "BOOK-001",
                "Distributed Systems",
                "A practical guide",
                new CatalogClient.MoneySnapshot(new BigDecimal("29.90"), "USD"),
                "ACTIVE"
        );

        when(cartRepository.findByPublicId(cartId)).thenReturn(Optional.of(cart));
        when(catalogClient.getProduct(productId)).thenReturn(product);
        when(cartRepository.save(cart)).thenReturn(cart);

        Cart result = cartService.addItem(cartId, productId, 2);

        assertThat(result.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getProductId()).isEqualTo(productId);
            assertThat(item.getProductName()).isEqualTo("Distributed Systems");
            assertThat(item.getUnitPriceAmount()).isEqualByComparingTo("29.90");
            assertThat(item.getQuantity()).isEqualTo(2);
        });
        verify(cartRepository).save(cart);
    }

    @Test
    void reusesAnExistingActiveCartForTheCustomer() {
        UUID customerId = UUID.randomUUID();
        Cart existing = new Cart(customerId);
        when(cartRepository.findFirstByCustomerIdAndStatus(customerId, CartStatus.ACTIVE))
                .thenReturn(Optional.of(existing));

        assertThat(cartService.createOrGetActiveCart(customerId)).isSameAs(existing);
    }
}
