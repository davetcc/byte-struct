package com.thecoderscorner.demo.trading;

import com.thecoderscorner.demo.trading.price.PriceDistributor;
import com.thecoderscorner.demo.trading.staticdata.ProductType;
import com.thecoderscorner.demo.trading.staticdata.StaticDataService;
import com.thecoderscorner.demo.trading.staticdata.StaticMessage;
import com.thecoderscorner.demo.trading.staticdata.TradeableVenue;
import com.thecoderscorner.demo.trading.stats.StatisticsCollatorService;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;

@RestController
@RequestMapping("/api/trading")
@Slf4j
public class TradingController {
    private final StaticDataService staticDataService;
    private final PriceDistributor priceDistributor;
    private final StatisticsCollatorService statisticsCollatorService;

    public TradingController(StaticDataService staticDataService, PriceDistributor priceDistributor,
                             StatisticsCollatorService statisticsCollatorService) {
        this.staticDataService = staticDataService;
        this.priceDistributor = priceDistributor;
        this.statisticsCollatorService = statisticsCollatorService;
    }

    @GetMapping("/acquireStatic")
    public Flux<StaticPojo> acquireStatic() {
        log.info("Request for all static data");
        return Flux.fromIterable(staticDataService.getAllStaticData().stream()
                .map(StaticPojo::fromMessage).toList());
    }

    @GetMapping(value = "/stats", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<StatisticsCollatorService.StatisticsPojo>> statistics() {
        log.info("Request for statistics updates stream");
        var statisticsFlux = statisticsCollatorService.getStatisticsFlux()
                .map(pojo -> ServerSentEvent.builder(pojo).event("stats").build());

        var heartbeatFlux = Flux.interval(Duration.ofSeconds(15))
                .map(i -> ServerSentEvent.<StatisticsCollatorService.StatisticsPojo>builder().comment("keep-alive").build());

        return Flux.merge(statisticsFlux, heartbeatFlux);
    }

    @GetMapping(value = "/priceUpdates", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<PriceDistributor.PricePojo>> priceUpdates() {
        log.info("Request for price updates stream");
        var priceFlux = priceDistributor.getPriceFlux()
                .map(pojo -> ServerSentEvent.builder(pojo).event("price-update").build());

        var heartbeatFlux = Flux.interval(Duration.ofSeconds(15))
                .map(i -> ServerSentEvent.<PriceDistributor.PricePojo>builder().comment("keep-alive").build());

        return Flux.merge(priceFlux, heartbeatFlux);
    }

    @Value
    @AllArgsConstructor
    public static class StaticPojo {
        String ticker;
        boolean blocked;
        boolean preMarket;
        int ticksPerPoint;
        ProductType priceType;
        TradeableVenue tradingVenue;

        public static StaticPojo fromMessage(StaticMessage staticMessage) {
            return new StaticPojo(
                    staticMessage.getTicker().toString(),
                    staticMessage.isBlocked(), staticMessage.isPreMarket(),
                    staticMessage.getTicksPerPoint(), staticMessage.getProductType(),
                    staticMessage.getTradeableVenue());
        }
    }
}
