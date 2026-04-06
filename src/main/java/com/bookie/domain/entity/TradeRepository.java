package com.bookie.domain.entity;

import com.bookie.infra.MessageBus;
import com.bookie.infra.events.TradeBookedEvent;
import com.bookie.infra.events.TradeDeletedEvent;
import com.bookie.infra.events.TradeModifiedEvent;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static com.bookie.infra.Util.sleep;

@Repository
public class TradeRepository {

    private static final int TRADE_COUNT = 1000;

    private final List<Trade> trades;
    private long nextId = 1001;
    private final MessageBus messageBus;
    private final BondRepository bondRepository;
    private final ReferenceDataRepository referenceDataRepository;

    public TradeRepository(BondRepository bondRepository, ReferenceDataRepository referenceDataRepository, MessageBus messageBus) {
        this.trades = new ArrayList<>(Collections.unmodifiableList(generate(bondRepository, referenceDataRepository)));
        this.messageBus = messageBus;
        this.bondRepository = bondRepository;
        this.referenceDataRepository = referenceDataRepository;
    }

    public List<Trade> getAllTrades() {
        sleep(1000); //This is a slow DB load
        return new ArrayList<>(trades);
    }

    public Trade findById(Long id) {
        return trades.stream().filter(t -> t.getId().equals(id)).findFirst().orElse(null);
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

    public void deleteTrade(Long id) {
        trades.removeIf(t -> t.getId().equals(id));
        messageBus.publish(new TradeDeletedEvent(id));
    }

    public Trade modifyTrade(Trade trade) {
        trades.removeIf(t -> t.getId().equals(trade.getId()));
        trade.setExecutionTime(new Date());
        trades.add(trade);
        messageBus.publish(new TradeModifiedEvent(trade));
        return trade;
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