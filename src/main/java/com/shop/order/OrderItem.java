package com.shop.order;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


// REVIEW [Major] - by default mapping of fetch type is EAGER, this will lead to threads in waiting, use lazy
    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    private String sku;

    private int qty;

    private long pricePaise;

    protected OrderItem() {
    }

    public OrderItem(String sku, int qty, long pricePaise) {
        this.sku = sku;
        this.qty = qty;
        this.pricePaise = pricePaise;
    }

    public Long getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public String getSku() {
        return sku;
    }

    public int getQty() {
        return qty;
    }

    public long getPricePaise() {
        return pricePaise;
    }
}
