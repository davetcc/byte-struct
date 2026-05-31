package com.thecoderscorner.demo.trading;

import com.thecoderscorner.demo.trading.price.PriceConflationService;
import com.thecoderscorner.demo.trading.price.PriceDistributor;
import com.thecoderscorner.demo.trading.staticdata.StaticDataService;
import com.thecoderscorner.demo.trading.stats.StatisticsCollatorService;
import com.thecoderscorner.demo.trading.stats.StatisticsCollection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.Executors.*;

@Configuration
public class TradingConfig {
    @Bean
    public NativeTradingSystem nativeTradingSystem(@Value("${trading.lib.path}") Path libPath,
                                                   PriceConflationService priceConflationService,
                                                   StaticDataService staticDataService) {
        return new NativeTradingSystem(libPath, priceConflationService, staticDataService);
    }

    @Bean
    public StaticDataService staticDataService() {
        return new StaticDataService();
    }

    @Bean
    public PriceConflationService priceConflationService(StatisticsCollection statisticsCollection) {
        return new PriceConflationService(statisticsCollection);
    }

    @Bean
    public ScheduledExecutorService scheduledExecutorService() {
        return newSingleThreadScheduledExecutor();
    }

    @Bean
    public PriceDistributor priceDistributor(ScheduledExecutorService scheduledExecutorService,
                                             PriceConflationService priceConflationService,
                                             StaticDataService staticDataService) {
        return new PriceDistributor(scheduledExecutorService, priceConflationService, staticDataService);
    }

    @Bean
    public StatisticsCollatorService statisticsCollator(ScheduledExecutorService scheduledExecutorService) {
        return new StatisticsCollatorService(scheduledExecutorService);
    }
}
