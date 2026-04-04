package com.bookie.domain.entity;

import com.bookie.infra.MessageBus;
import com.bookie.infra.events.TradeBookedEvent;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Repository
public class TradeRepository {

    private static final int TRADE_COUNT = 1000;

    private final List<Trade> trades;
    private long nextId = 1001;
    private final MessageBus messageBus;

    public TradeRepository(BondRepository bondRepository, ReferenceDataRepository referenceDataRepository, MessageBus messageBus) {
        this.trades = new ArrayList<>(Collections.unmodifiableList(generate(bondRepository, referenceDataRepository)));
        this.messageBus = messageBus;
    }

    public List<Trade> getAllTrades() { return Collections.unmodifiableList(trades); }

    public Trade findById(Long id) {
        return trades.stream().filter(t -> t.getId().equals(id)).findFirst().orElse(null);
    }

    public Trade bookTrade(Trade trade) {
        if (trade.getId() == null) {
            trade.setId(nextId++);
        }

        trade.setExecutionTime(new Date());

        trades.add(trade);

        messageBus.publish(new TradeBookedEvent(trade));

        return trade;
    }

    private static List<Trade> generate(BondRepository bondRepo, ReferenceDataRepository refData) {
        List<Bond> bonds = bondRepo.getAllBonds();
        List<String> books = refData.getAllBooks();
        List<String> counterparties = refData.getAllCounterparties();
        List<Trade> list = new ArrayList<>();
        Random rng = new Random(42);
        LocalDate today = LocalDate.of(2026, 4, 3);

        for (int i = 0; i < TRADE_COUNT; i++) {
            Bond bond = bonds.get(rng.nextInt(bonds.size()));
            TradeDirection dir = rng.nextInt(10) < 7 ? TradeDirection.BUY : TradeDirection.SELL;
            LocalDate tradeDate = today.minusDays(rng.nextInt(730));
            BigDecimal quantity = BigDecimal.valueOf((rng.nextInt(200) + 1) * 100_000L);
            BigDecimal accrued  = bond.getCoupon()
                    .divide(BigDecimal.valueOf(200), 6, RoundingMode.HALF_UP)
                    .multiply(quantity)
                    .setScale(2, RoundingMode.HALF_UP);

            Trade t = new Trade();
            t.setId((long) (i + 1));
            t.setCusip(bond.getCusip());
            t.setDirection(dir);
            t.setTradeDate(tradeDate);
            t.setSettleDate(tradeDate.plusDays(2));
            t.setQuantity(quantity);
            t.setAccruedInterest(accrued);
            t.setBook(books.get(rng.nextInt(books.size())));
            t.setCounterparty(counterparties.get(rng.nextInt(counterparties.size())));
            list.add(t);
        }
        return list;
    }
}