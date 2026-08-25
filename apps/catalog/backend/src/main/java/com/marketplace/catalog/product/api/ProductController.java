package com.marketplace.catalog.product.api;

import com.marketplace.catalog.product.ProductService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
class ProductController {

    private final ProductService productService;
    private final Logger log = LoggerFactory.getLogger(ProductController.class);

    ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    List<ProductResponse> listProducts() {
        log.info("REST request to get all products");
        return productService.listActiveProducts();
    }

    @GetMapping("/{id}")
    ProductResponse getProduct(@PathVariable UUID id) {
        return productService.getProduct(id);
    }

    @PostMapping
    ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse created = productService.createProduct(request);
        return ResponseEntity
                .created(URI.create("/api/products/" + created.id()))
                .body(created);
    }
}
