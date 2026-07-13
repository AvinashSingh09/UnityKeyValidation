package com.company.keyvault.service;

import com.company.keyvault.dto.request.ProductRequest;
import com.company.keyvault.exception.DuplicateResourceException;
import com.company.keyvault.exception.ResourceNotFoundException;
import com.company.keyvault.model.Product;
import com.company.keyvault.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product getProductById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + id));
    }

    public Product getProductByCode(String productCode) {
        return productRepository.findByProductCode(productCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with code: " + productCode));
    }

    public Product createProduct(ProductRequest request, String createdBy) {
        if (productRepository.existsByProductCode(request.getProductCode())) {
            throw new DuplicateResourceException(
                    "Product already exists with code: " + request.getProductCode());
        }

        Product product = Product.builder()
                .name(request.getName())
                .productCode(request.getProductCode())
                .description(request.getDescription())
                .version(request.getVersion())
                .active(true)
                .createdBy(createdBy)
                .build();

        return productRepository.save(product);
    }

    public Product updateProduct(String id, ProductRequest request) {
        Product product = getProductById(id);

        // Check if product code is changing and if new code already exists
        if (!product.getProductCode().equals(request.getProductCode())
                && productRepository.existsByProductCode(request.getProductCode())) {
            throw new DuplicateResourceException(
                    "Product already exists with code: " + request.getProductCode());
        }

        product.setName(request.getName());
        product.setProductCode(request.getProductCode());
        product.setDescription(request.getDescription());
        product.setVersion(request.getVersion());

        return productRepository.save(product);
    }

    public void deleteProduct(String id) {
        Product product = getProductById(id);
        product.setActive(false);
        productRepository.save(product);
    }
}
