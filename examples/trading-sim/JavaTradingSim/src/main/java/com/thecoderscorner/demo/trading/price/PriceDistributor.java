package com.thecoderscorner.demo.trading.price;

import com.thecoderscorner.demo.trading.staticdata.StaticDataService;
import com.thecoderscorner.demo.trading.staticdata.StaticMessage;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class PriceDistributor {
    private final ScheduledExecutorService executorService;
    private final PriceConflationService conflationService;
    private final ScheduledFuture<?> priceTask;
    List<PriceMessage> priceMessages = new ArrayList<>(256);
    private final StaticDataService staticDataService;
    private final Sinks.Many<PricePojo> priceSink = Sinks.many().multicast().directBestEffort();

    public PriceDistributor(ScheduledExecutorService executorService,
                            PriceConflationService conflationService,
                            StaticDataService staticDataService) {
        this.executorService = executorService;
        this.conflationService = conflationService;
        this.staticDataService = staticDataService;
        priceTask = this.executorService.scheduleAtFixedRate(this::receiveChanges, 0, 200, TimeUnit.MILLISECONDS);
    }

    public Flux<PricePojo> getPriceFlux() {
        return priceSink.asFlux();
    }

    public void stop() {
        priceTask.cancel(true);
        priceMessages.clear();
    }

    private void receiveChanges() {
        priceMessages.clear();
        if(conflationService.receiveChangesIntoList(priceMessages)) {
            for(var pm : priceMessages) {
                sendPrice(pm);
            }
        }
    }

    private void sendPrice(PriceMessage price) {
        var instrument = staticDataService.getStaticData(price.getTicker());
        if(instrument == null) {
            log.error("Failed to find instrument for ticker {}", price.getTicker());
            return;
        }
        priceSink.tryEmitNext(PricePojo.fromMsg(price, instrument));
    }

    @Value
    @AllArgsConstructor
    public static class PricePojo {
        String ticker;
        String price;
        String source;

        public static PricePojo fromMsg(PriceMessage price, StaticMessage instrument) {
            var ticks = price.getTickPrice().asLong();
            var tpp = instrument.getTicksPerPoint();
            var px = String.format("%d.%02d", ticks / tpp, ticks % tpp);
            //log.info("Price conversion for ticker {} to price {}", price.getTicker(), px);
            return new PricePojo(price.getTicker().toString(), px, price.getSource().toString());
        }
    }
}
