package com.marketplace.catalog.product;

import com.marketplace.catalog.product.api.CreateProductRequest;
import com.marketplace.catalog.product.api.MoneyResponse;
import com.marketplace.catalog.product.api.ProductResponse;
import com.marketplace.catalog.repository.Product;
import com.marketplace.catalog.repository.ProductRepository;
import com.marketplace.catalog.shared.ConflictException;
import com.marketplace.catalog.shared.NotFoundException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listActiveProducts() {
        return productRepository.findByStatusOrderByNameAsc(ProductStatus.ACTIVE)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(UUID id) {
        return productRepository.findByPublicId(id)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Product %s was not found".formatted(id)));
    }

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        String normalizedSku = request.sku().trim().toUpperCase(Locale.ROOT);
        if (productRepository.existsBySku(normalizedSku)) {
            throw new ConflictException("Product SKU %s already exists".formatted(normalizedSku));
        }

        Product product = Product.active(
                normalizedSku,
                request.name().trim(),
                request.description().trim(),
                request.priceAmount(),
                request.currency().trim().toUpperCase(Locale.ROOT)
        );

        return toResponse(productRepository.save(product));
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getPublicId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                new MoneyResponse(product.getPriceAmount(), product.getCurrency()),
                product.getStatus().name()
        );
    }
}
