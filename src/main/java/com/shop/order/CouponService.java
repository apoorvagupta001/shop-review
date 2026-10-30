package com.shop.order;

import org.springframework.stereotype.Service;

@Service
public class CouponService {

    /** Throws if the coupon can no longer be used. */
    public void validate(String code) throws CouponExpiredException {
        if (code.startsWith("EXPIRED")) {
            throw new CouponExpiredException(code);
        }
    }

    /** Returns the total after the coupon's discount. */
    public long apply(long totalPaise, String code) {
        return code.equals("SAVE10") ? totalPaise * 90 / 100 : totalPaise;
    }
}
