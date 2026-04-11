package com.bookie.domain.entity;

import com.bookie.infra.EventBus;
import com.bookie.infra.events.TradeBookedEvent;
import com.bookie.infra.events.TradeDeletedEvent;
import com.bookie.infra.events.TradeModifiedEvent;
import com.bookie.infra.events.TradesLoadedEvent;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static com.bookie.infra.Util.sleep;

@Repository
public class TradeRepository {

    private static final int TRADE_COUNT = 1000;

    private List<Trade> trades;
    private long nextId = 1001;
    private final EventBus eventBus;
    private final BondRepository bondRepository;
    private final ReferenceDataRepository referenceDataRepository;

    public TradeRepository(BondRepository bondRepository, ReferenceDataRepository referenceDataRepository, EventBus eventBus) {
        this.eventBus = eventBus;
        this.bondRepository = bondRepository;
        this.referenceDataRepository = referenceDataRepository;
    }

    public synchronized List<Trade> getAllTrades() {
        if(trades == null) {
            Thread.startVirtualThread(this::loadTradesFromDB);
            return Collections.emptyList();
        }

        return new ArrayList<>(trades);
    }

    public synchronized Trade findById(Long id) {
        return trades.stream().filter(t -> t.getId().equals(id)).findFirst().orElse(null);
    }

    public synchronized void deleteTrade(Long id) {
        trades.removeIf(t -> t.getId().equals(id));
        eventBus.publish(new TradeDeletedEvent(id));
    }

    public synchronized Trade modifyTrade(Trade trade) {
        trades.removeIf(t -> t.getId().equals(trade.getId()));
        trade.setExecutionTime(new Date());
        trades.add(trade);
        eventBus.publish(new TradeModifiedEvent(trade));
        return trade;
    }

    public synchronized Trade bookTrade(Trade trade) {
        if (trade.getId() == null) {
            trade.setId(nextId++);
        }

        trade.setExecutionTime(new Date());

        trades.add(trade);

        eventBus.publish(new TradeBookedEvent(trade));

        return trade;
    }

    public boolean isValid(Trade trade) {
        return validateCusip(trade.getCusip()) == null
                && validateQuantity(trade.getQuantity()) == null
                && validateBook(trade.getBook()) == null
                && validateCounterparty(trade.getCounterparty()) == null;
    }

    public String validateCusip(String cusip) {
        if (cusip == null || cusip.isBlank()) {
            return "This field is required";
        }
        if (!bondRepository.isValidCusip(cusip)) {
            return "The CUSIP is invalid - please specify a known CUSIP";
        }
        return null;
    }

    public String validateBook(String book) {
        if (book == null || book.isBlank()) {
            return "This field is required";
        }
        if (!referenceDataRepository.getAllBooks().contains(book)) {
            return "Invalid book";
        }
        return null;
    }

    public String validateCounterparty(String counterparty) {
        if (counterparty == null || counterparty.isBlank()) {
            return "This field is required";
        }
        if (!referenceDataRepository.getAllCounterparties().contains(counterparty)) {
            return "Invalid counterparty";
        }
        return null;
    }

    public String validateQuantity(BigDecimal quantity) {
        if (quantity == null) {
            return "This field is required";
        }
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return "Quantity has to be > 0";
        }
        return null;
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
            BigDecimal accrued = bond.getCoupon() != null ? bond.getCoupon()
                                                            .divide(BigDecimal.valueOf(200), 6, RoundingMode.HALF_UP)
                                                            .multiply(quantity)
                                                            .setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            Trade t = new Trade();
            t.setId((long) (i + 1));
            t.setCusip(bond.getCusip());
            t.setDirection(dir);
            t.setTradeDate(tradeDate);
            t.setSettleDate(tradeDate.plusDays(2));
            t.setExecutionTime(Date.from(tradeDate.atStartOfDay(ZoneOffset.UTC).plusSeconds(rng.nextInt(86400)).toInstant()));
            t.setQuantity(quantity);
            t.setAccruedInterest(accrued);
            t.setBook(books.get(rng.nextInt(books.size())));
            t.setCounterparty(counterparties.get(rng.nextInt(counterparties.size())));
            list.add(t);
        }
        return list;
    }

    private void loadTradesFromDB() {
        //Slow DB here.
        sleep(1000L);
        this.trades = new ArrayList<>(Collections.unmodifiableList(generate(bondRepository, referenceDataRepository)));
        eventBus.publish(new TradesLoadedEvent(this.trades));
    }
}