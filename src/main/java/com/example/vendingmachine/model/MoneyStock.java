package com.example.vendingmachine.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "money_stock")
public class MoneyStock {

    @Id
    @Column(name = "money_type")
    private Integer moneyType;

    @Column(name = "stock_count")
    private Integer stockCount;

    @Column(name = "available")
    private Boolean available;

    public MoneyStock() {
    }

    public Integer getMoneyType() {
        return moneyType;
    }

    public void setMoneyType(Integer moneyType) {
        this.moneyType = moneyType;
    }

    public Integer getStockCount() {
        return stockCount;
    }

    public void setStockCount(Integer stockCount) {
        this.stockCount = stockCount;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }
}