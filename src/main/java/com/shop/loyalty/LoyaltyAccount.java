package com.shop.loyalty;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "loyalty_accounts")
public class LoyaltyAccount {

    @Id
    private String customerId;

    private int points;

    protected LoyaltyAccount() {
    }

    public LoyaltyAccount(String customerId, int points) {
        this.customerId = customerId;
        this.points = points;
    }

    public String getCustomerId() {
        return customerId;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }
}
