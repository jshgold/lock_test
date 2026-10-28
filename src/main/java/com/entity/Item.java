package com.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Item {
    @Id
    private Long id;
    private int stock;

    protected Item() {}

    public Item(Long id, int stock) {
        this.id = id;
        this.stock = stock;
    }

    public void decreaseStock() {
        if (this.stock <= 0) {
            throw new RuntimeException("재고 부족");
        }
        this.stock--;
    }

    public int getStock() { return stock; }
}
