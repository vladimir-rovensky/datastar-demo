package com.bookie.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

public class Trade implements Cloneable {

    private Long id;
    private String cusip;
    private Date executionTime;
    private TradeDirection direction;
    private BigDecimal quantity = BigDecimal.ZERO;
    private LocalDate tradeDate;
    private LocalDate settleDate;
    private BigDecimal accruedInterest = BigDecimal.ZERO;
    private String book;
    private String counterparty;

    public static Trade aBlankTrade(TradeDirection direction) {
        var trade = new Trade();
        trade.setDirection(direction);
        trade.setTradeDate(LocalDate.now());
        trade.setSettleDate(LocalDate.now().plusDays(2));
        return trade;
    }

    public Long getId() { return id; }
    public Trade setId(Long id) { this.id = id; return this; }

    public String getCusip() { return cusip; }
    public Trade setCusip(String cusip) { this.cusip = cusip; return this; }

    public TradeDirection getDirection() { return direction; }
    public Trade setDirection(TradeDirection direction) { this.direction = direction; return this; }

    public LocalDate getTradeDate() { return tradeDate; }
    public Trade setTradeDate(LocalDate tradeDate) { this.tradeDate = tradeDate; return this; }

    public LocalDate getSettleDate() { return settleDate; }
    public Trade setSettleDate(LocalDate settleDate) { this.settleDate = settleDate; return this; }

    public BigDecimal getAccruedInterest() { return accruedInterest; }
    public Trade setAccruedInterest(BigDecimal accruedInterest) { this.accruedInterest = accruedInterest; return this; }

    public String getBook() { return book; }
    public Trade setBook(String book) { this.book = book; return this; }

    public BigDecimal getQuantity() { return quantity; }
    public Trade setQuantity(BigDecimal quantity) { this.quantity = quantity; return this; }

    public String getCounterparty() { return counterparty; }
    public Trade setCounterparty(String counterparty) { this.counterparty = counterparty; return this; }

    public Date getExecutionTime() { return executionTime; }
    public Trade setExecutionTime(Date executionTime) { this.executionTime = executionTime; return this; }

    public PositionKey getPositionKey() { return new PositionKey(cusip, book); }

    @Override
    public Trade clone() {
        try {
            var clonedTrade = (Trade) super.clone();
            if (executionTime != null) {
                clonedTrade.executionTime = new Date(executionTime.getTime());
            }
            return clonedTrade;
        }
        catch (CloneNotSupportedException exception) {
            throw new AssertionError(exception);
        }
    }
}