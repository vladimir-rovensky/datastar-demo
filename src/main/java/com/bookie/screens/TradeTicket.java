package com.bookie.screens;

import com.bookie.domain.entity.Trade;
import com.bookie.domain.entity.TradeDirection;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TradeTicket {

    private Long tradeId;
    private TradeDirection direction;
    private String cusip;
    private String book;
    private BigDecimal quantity = BigDecimal.ZERO;
    private BigDecimal accruedInterest = BigDecimal.ZERO;
    private LocalDate tradeDate;
    private LocalDate settleDate;
    private String counterparty;

    public static TradeTicket aBlankTrade(TradeDirection direction) {
        var ticket = new TradeTicket();
        ticket.setDirection(direction);
        ticket.setTradeDate(LocalDate.now());
        ticket.setSettleDate(LocalDate.now().plusDays(2));
        return ticket;
    }

    public static TradeTicket fromTrade(Trade trade) {
        var ticket = new TradeTicket();
        ticket.setTradeId(trade.getId());
        ticket.setDirection(trade.getDirection());
        ticket.setCusip(trade.getCusip());
        ticket.setBook(trade.getBook());
        ticket.setQuantity(trade.getQuantity());
        ticket.setAccruedInterest(trade.getAccruedInterest());
        ticket.setTradeDate(trade.getTradeDate());
        ticket.setSettleDate(trade.getSettleDate());
        ticket.setCounterparty(trade.getCounterparty());
        return ticket;
    }

    public Trade toTrade() {
        Trade trade = new Trade();
        trade.setId(tradeId);
        trade.setCusip(cusip);
        trade.setDirection(direction);
        trade.setQuantity(quantity);
        trade.setAccruedInterest(accruedInterest);
        trade.setTradeDate(tradeDate);
        trade.setSettleDate(settleDate);
        trade.setBook(book);
        trade.setCounterparty(counterparty);
        return trade;
    }

    public Long getTradeId() { return tradeId; }
    public void setTradeId(Long tradeId) { this.tradeId = tradeId; }

    public String getCusip() { return cusip; }
    public void setCusip(String cusip) { this.cusip = cusip; }

    public String getBook() { return book; }
    public void setBook(String book) { this.book = book; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getAccruedInterest() { return accruedInterest; }
    public void setAccruedInterest(BigDecimal accruedInterest) { this.accruedInterest = accruedInterest; }

    public LocalDate getTradeDate() { return tradeDate; }
    public void setTradeDate(LocalDate tradeDate) { this.tradeDate = tradeDate; }

    public LocalDate getSettleDate() { return settleDate; }
    public void setSettleDate(LocalDate settleDate) { this.settleDate = settleDate; }

    public TradeDirection getDirection() { return direction; }
    public void setDirection(TradeDirection direction) { this.direction = direction; }

    public String getCounterparty() { return counterparty; }
    public void setCounterparty(String counterparty) { this.counterparty = counterparty; }
}