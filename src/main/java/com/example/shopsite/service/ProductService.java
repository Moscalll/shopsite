package com.example.shopsite.service;

import com.example.shopsite.dto.ProductCreationRequest;
import com.example.shopsite.model.Product;

public interface ProductService {
    Product createProduct(ProductCreationRequest request, String username);
    // TODO: updateProduct, deleteProduct
}