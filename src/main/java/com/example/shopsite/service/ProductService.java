package com.example.shopsite.service;

import com.example.shopsite.dto.ProductCreationRequest;
import com.example.shopsite.model.Product;
import java.util.List;

public interface ProductService {
    Product createProduct(ProductCreationRequest request, String username);
    List<Product> findAllAvailableProducts();
    // TODO: updateProduct, deleteProduct
}