package com.shop.inventory;

public class OutOfStockException extends RuntimeException {

    public OutOfStockException(String sku, int requestedQty) {
        super("SKU " + sku + " cannot supply " + requestedQty + " units");
    }
}
