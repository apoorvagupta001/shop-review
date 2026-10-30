package com.shop.inventory;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shop.catalog.Product;
import com.shop.catalog.ProductRepository;

// PART A (check 1)
@Service
public class InventoryService {

    private final ProductRepository productRepository;

    public InventoryService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Reserves one unit of every SKU in the cart before checkout.
     * All or nothing: if any SKU is out of stock, nothing may stay reserved.
     */
    // REVIEW : [Blocker] - reserveOne is called internally and this will not result
    // in transaction, reserveOne should directly be called from service layer.
    @Transactional
    public void reserveCart(List<String> skus) {
        for (String sku : skus) {
            if (productRepository.reduceStock(sku, 1) == 0) { // 0 rows changed: not enough stock
                throw new OutOfStockException(sku, 1); // runtime exception: rolls back the whole cart
            }
            Product product = productRepository.findBySku(sku)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown SKU " + sku));
            product.setStock(product.getStock() - 1);
            productRepository.save(product);
        }
    }

}
