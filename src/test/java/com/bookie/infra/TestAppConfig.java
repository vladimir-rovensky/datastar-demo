package com.bookie.infra;

import com.bookie.Router;
import com.bookie.domain.entity.BondRepository;
import com.bookie.domain.entity.FakeBondDAO;
import com.bookie.domain.entity.FakeTradeDAO;
import com.bookie.domain.entity.ReferenceDataRepository;
import com.bookie.domain.entity.TradeRepository;
import com.bookie.domain.service.PricingService;
import com.bookie.screens.TradeTicketPopup;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import java.util.Map;

@Configuration
@Import(Router.class)
public class TestAppConfig {

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        MutablePropertySources sources = new MutablePropertySources();
        sources.addFirst(new MapPropertySource("testProperties", Map.of(
            "bookie.version", "test",
            "bookie.cache.enabled", "false",
            "bookie.session.timeout-seconds", "60"
        )));
        configurer.setPropertySources(sources);
        return configurer;
    }

    @Bean
    public EventBus eventBus() {
        return new EventBus();
    }

    @Bean
    public SessionRegistry sessionRegistry(AutowireCapableBeanFactory beanFactory) {
        return new SessionRegistry(beanFactory, 60L);
    }

    @Bean
    public FakeBondDAO fakeBondDAO() {
        return new FakeBondDAO();
    }

    @Bean
    public BondRepository bondRepository(FakeBondDAO fakeBondDAO, EventBus eventBus) {
        BondRepository bondRepository = new BondRepository(fakeBondDAO, eventBus);
        bondRepository.setGenerateFakeData(false);
        return bondRepository;
    }

    @Bean
    public FakeTradeDAO fakeTradeDAO() {
        return new FakeTradeDAO();
    }

    @Bean
    public ReferenceDataRepository referenceDataRepository() {
        return new ReferenceDataRepository();
    }

    @Bean
    public PricingService pricingService() {
        return new PricingService();
    }

    @Bean
    public TradeRepository tradeRepository(FakeTradeDAO fakeTradeDAO, BondRepository bondRepository,
                                           ReferenceDataRepository referenceDataRepository, EventBus eventBus) {
        TradeRepository tradeRepository = new TradeRepository(fakeTradeDAO, bondRepository, referenceDataRepository, eventBus);
        tradeRepository.setGenerateFakeData(false);
        return tradeRepository;
    }

    @Bean
    public TradeTicketPopup tradeTicketPopup(ReferenceDataRepository referenceDataRepository,
                                             PricingService pricingService, TradeRepository tradeRepository) {
        return new TradeTicketPopup(referenceDataRepository, pricingService, tradeRepository);
    }
}
