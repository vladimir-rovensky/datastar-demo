package com.bookie.domain.entity;

import com.bookie.infra.EventBus;
import com.bookie.infra.events.TradeBookedEvent;
import com.bookie.infra.events.TradeDeletedEvent;
import com.bookie.infra.events.TradeModifiedEvent;
import com.bookie.infra.events.TradesLoadedEvent;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

import static com.bookie.infra.Util.sleep;

@Repository
public class TradeRepository {

    private static final int TRADE_COUNT = 1000;

    private long nextId = 1001;
    private final TradeDAO dao;
    private final EventBus eventBus;
    private final BondRepository bondRepository;
    private final ReferenceDataRepository referenceDataRepository;

    private boolean generateFakeData = true;

    public TradeRepository(TradeDAO dao, BondRepository bondRepository, ReferenceDataRepository referenceDataRepository, EventBus eventBus) {
        this.dao = dao;
        this.eventBus = eventBus;
        this.bondRepository = bondRepository;
        this.referenceDataRepository = referenceDataRepository;
    }

    @PostConstruct
    public void init() {
        Thread.startVirtualThread(this::loadTradesFromDB);
    }

    public synchronized List<Trade> getAllTrades() {
        return dao.findAll();
    }

    public synchronized Trade findById(Long id) {
        return dao.findById(id);
    }

    public synchronized void deleteTrade(Long id) {
        Trade deletedTrade = dao.delete(id);
        if (deletedTrade == null) {
            return;
        }

        eventBus.publish(new TradeDeletedEvent(deletedTrade));
    }

    public synchronized Trade modifyTrade(Trade trade) {
        Trade originalTrade = dao.findById(trade.getId());
        if (originalTrade == null) {
            return null;
        }

        trade.setExecutionTime(new Date());
        dao.save(trade);
        eventBus.publish(new TradeModifiedEvent(originalTrade, trade));
        return trade;
    }

    public synchronized Trade bookTrade(Trade trade) {
        if (trade.getId() == null) {
            trade.setId(nextId++);
        }

        trade.setExecutionTime(new Date());
        dao.save(trade);
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

    public void setGenerateFakeData(boolean generateFakeData) {
        this.generateFakeData = generateFakeData;
    }

    private List<Trade> generate(BondRepository bondRepo, ReferenceDataRepository refData) {
        if(!generateFakeData) {
            return Collections.emptyList();
        }

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
        var trades = generate(bondRepository, referenceDataRepository);
        this.dao.saveAll(trades);

        eventBus.publish(new TradesLoadedEvent(new ArrayList<>(trades)));
    }
}
