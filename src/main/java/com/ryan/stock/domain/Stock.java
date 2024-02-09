package com.ryan.stock.domain;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long productId;
    private Long quantity;
    @Version
    private Long version;

    public Stock() {
    }

    public Stock(Long productId, Long quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public void decrease(Long quantity) {
        if (this.quantity - quantity <= 0)
            throw new RuntimeException("모든 재고가 소진 되었습니다.");

        this.quantity -= quantity;
    }
}
