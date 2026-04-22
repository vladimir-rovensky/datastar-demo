package com.bookie.domain.entity;

import com.bookie.domain.service.PositionService;
import com.bookie.infra.EventBus;
import com.bookie.infra.events.TradeBookedEvent;
import com.bookie.infra.events.TradeDeletedEvent;
import com.bookie.infra.events.TradeModifiedEvent;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

import static com.bookie.infra.Util.sleep;

@Repository
public class TradeRepository {

    private static final int TRADE_COUNT = 1000;

    private long nextId = 1;
    private final TradeDAO dao;
    private final EventBus eventBus;
    private final BondRepository bondRepository;
    private final ReferenceDataRepository referenceDataRepository;
    private final PositionService positionService;

    private boolean generateFakeData = true;

    public TradeRepository(TradeDAO dao, BondRepository bondRepository,
                           ReferenceDataRepository referenceDataRepository, EventBus eventBus,
                           PositionService positionService) {
        this.dao = dao;
        this.eventBus = eventBus;
        this.bondRepository = bondRepository;
        this.referenceDataRepository = referenceDataRepository;
        this.positionService = positionService;
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

    public synchronized boolean bookTrade(Trade trade) {
        trade.setExecutionTime(new Date());

        if (!isValid(trade)) {
            return false;
        }

        if (trade.getId() != null) {
            modifyExistingTrade(trade);
        } else {
            bookNewTrade(trade);
        }

        return true;
    }

    private synchronized Trade bookNewTrade(Trade trade) {
        if (trade.getId() == null) {
            trade.setId(nextId++);
        }

        trade.setExecutionTime(new Date());
        dao.save(trade);
        positionService.onTradeBooked(trade);
        eventBus.publish(new TradeBookedEvent(trade));
        return trade;
    }

    private synchronized Trade modifyExistingTrade(Trade trade) {
        Trade originalTrade = dao.findById(trade.getId());
        if (originalTrade == null) {
            return null;
        }

        trade.setExecutionTime(new Date());
        dao.save(trade);
        positionService.onTradeModified(originalTrade, trade);
        eventBus.publish(new TradeModifiedEvent(originalTrade, trade));
        return trade;
    }

    public synchronized boolean deleteTrade(Long id) {

        var trade = dao.findById(id);
        var currentPosition = positionService.getPosition(trade.getPositionKey());
        var newPosition = positionService.getUpdatedPosition(currentPosition, trade, null);
        if (validatePosition(newPosition) != null) {
            return false;
        }

        dao.delete(id);

        positionService.onTradeDeleted(trade);
        eventBus.publish(new TradeDeletedEvent(trade));
        return true;
    }

    public boolean isValid(Trade trade) {
        return validateCusip(trade.getCusip()) == null
                && validateQuantity(trade) == null
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

    public String validateQuantity(Trade trade) {
        if (trade.getQuantity() == null) {
            return "This field is required";
        }

        if (trade.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            return "Quantity has to be > 0";
        }

        return validateNoShortPosition(trade);
    }

    private String validateNoShortPosition(Trade trade) {
        if (trade.getCusip() == null || trade.getBook() == null || trade.getDirection() == null
                || trade.getTradeDate() == null || trade.getSettleDate() == null) {
            return null;
        }

        var originalTrade = dao.findById(trade.getId());
        var currentPosition = positionService.getPosition(trade.getPositionKey());
        var newPosition = positionService.getUpdatedPosition(currentPosition, originalTrade, trade);

        return validatePosition(newPosition);
    }

    private static String validatePosition(Position newPosition) {
        if (newPosition.getCurrentPosition().compareTo(BigDecimal.ZERO) < 0) {
            return "Trade would result in negative Current Position";
        }

        if (newPosition.getSettledPosition().compareTo(BigDecimal.ZERO) < 0) {
            return "Trade would result in negative settled position";
        }

        return null;
    }

    public void setGenerateFakeData(boolean generateFakeData) {
        this.generateFakeData = generateFakeData;
    }

    private void generate(BondRepository bondRepo, ReferenceDataRepository refData) {
        if (!generateFakeData) {
            return;
        }

        List<Bond> bonds = bondRepo.getAllBonds();
        List<String> books = refData.getAllBooks();
        List<String> counterparties = refData.getAllCounterparties();
        Random rng = new Random(42);
        LocalDate today = LocalDate.of(2026, 4, 3);

        while (this.dao.getTotalCount() < TRADE_COUNT) {
            Bond bond = bonds.get(rng.nextInt(bonds.size()));
            TradeDirection direction = rng.nextInt(10) < 7 ? TradeDirection.BUY : TradeDirection.SELL;
            LocalDate tradeDate = today.minusDays(rng.nextInt(730));
            BigDecimal quantity = BigDecimal.valueOf((rng.nextInt(200) + 1) * 100_000L);
            BigDecimal accrued = quantity.multiply(BigDecimal.valueOf(0.1));

            Trade trade = new Trade();
            trade.setCusip(bond.getCusip());
            trade.setDirection(direction);
            trade.setTradeDate(tradeDate);
            trade.setSettleDate(tradeDate.plusDays(2));
            trade.setExecutionTime(Date.from(tradeDate.atStartOfDay(ZoneOffset.UTC).plusSeconds(rng.nextInt(86400)).toInstant()));
            trade.setQuantity(quantity);
            trade.setAccruedInterest(accrued);
            trade.setBook(books.get(rng.nextInt(books.size())));
            trade.setCounterparty(counterparties.get(rng.nextInt(counterparties.size())));

            if (this.isValid(trade)) {
                bookNewTrade(trade);
            }
        }
    }

    private void loadTradesFromDB() {
        //Slow DB here.
        sleep(1000L);
        generate(bondRepository, referenceDataRepository);
    }
}
