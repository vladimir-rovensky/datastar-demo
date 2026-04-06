package com.bookie.domain.entity;

import java.math.BigDecimal;
import java.util.Date;

public class Position {

    private String cusip;
    private String book;
    private BigDecimal currentPosition;
    private BigDecimal settledPosition;
    private Date lastActivity;

    public String getCusip() { return cusip; }
    public void setCusip(String cusip) { this.cusip = cusip; }

    public String getBook() { return book; }
    public void setBook(String book) { this.book = book; }

    public BigDecimal getCurrentPosition() { return currentPosition; }
    public void setCurrentPosition(BigDecimal currentPosition) { this.currentPosition = currentPosition; }

    public BigDecimal getSettledPosition() { return settledPosition; }
    public void setSettledPosition(BigDecimal settledPosition) { this.settledPosition = settledPosition; }

    public Date getLastActivity() { return lastActivity; }
    public void setLastActivity(Date lastActivity) { this.lastActivity = lastActivity; }
}
