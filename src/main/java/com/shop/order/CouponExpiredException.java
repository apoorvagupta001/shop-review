package com.shop.order;

public class CouponExpiredException extends Exception {

    public CouponExpiredException(String code) {
        super("Coupon " + code + " has expired");
    }
}
