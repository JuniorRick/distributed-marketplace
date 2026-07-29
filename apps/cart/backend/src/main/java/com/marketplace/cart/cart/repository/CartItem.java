package com.marketplace.cart.cart.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cart_items")
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "product_sku", nullable = false, length = 64)
    private String productSku;

    @Column(name = "product_name", nullable = false, length = 160)
    private String productName;

    @Column(name = "unit_price_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPriceAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CartItem() {
    }

    public CartItem(
            Cart cart,
            UUID productId,
            String productSku,
            String productName,
            BigDecimal unitPriceAmount,
            String currency,
            Integer quantity
    ) {
        this.publicId = UUID.randomUUID();
        this.cart = cart;
        this.productId = productId;
        this.productSku = productSku;
        this.productName = productName;
        this.unitPriceAmount = unitPriceAmount;
        this.currency = currency;
        this.quantity = quantity;
    }

    public void increaseQuantity(int amount) {
        changeQuantity(quantity + amount);
    }

    public void changeQuantity(int newQuantity) {
        if (newQuantity < 1 || newQuantity > 99) {
            throw new IllegalArgumentException("Cart item quantity must be between 1 and 99");
        }
        quantity = newQuantity;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public Cart getCart() {
        return cart;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getProductSku() {
        return productSku;
    }

    public String getProductName() {
        return productName;
    }

    public BigDecimal getUnitPriceAmount() {
        return unitPriceAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}