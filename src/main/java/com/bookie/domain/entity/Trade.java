package com.bookie.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Trade {

    private Long id;
    private String cusip;
    private TradeDirection direction;
    private BigDecimal quantity;
    private LocalDate tradeDate;
    private LocalDate settleDate;
    private BigDecimal accruedInterest;
    private String book;
    private String counterparty;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCusip() { return cusip; }
    public void setCusip(String cusip) { this.cusip = cusip; }

    public TradeDirection getDirection() { return direction; }
    public void setDirection(TradeDirection direction) { this.direction = direction; }

    public LocalDate getTradeDate() { return tradeDate; }
    public void setTradeDate(LocalDate tradeDate) { this.tradeDate = tradeDate; }

    public LocalDate getSettleDate() { return settleDate; }
    public void setSettleDate(LocalDate settleDate) { this.settleDate = settleDate; }

    public BigDecimal getAccruedInterest() { return accruedInterest; }
    public void setAccruedInterest(BigDecimal accruedInterest) { this.accruedInterest = accruedInterest; }

    public String getBook() { return book; }
    public void setBook(String book) { this.book = book; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public String getCounterparty() { return counterparty; }
    public void setCounterparty(String counterparty) { this.counterparty = counterparty; }
}