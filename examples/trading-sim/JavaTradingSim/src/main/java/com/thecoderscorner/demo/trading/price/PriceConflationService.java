package com.thecoderscorner.demo.trading.price;

import com.thecoderscorner.demo.trading.stats.StatisticsCollection;
import com.thecoderscorner.lowlatency.bytestruct.Utf8View;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class PriceConflationService {
    private final ConcurrentMap<Utf8View, StoredPriceMessage> conflatedPrices = new ConcurrentHashMap<>(128);
    private final StatisticsCollection statisticsCollection;

    public PriceConflationService(StatisticsCollection statisticsCollection) {
        this.statisticsCollection = statisticsCollection;
    }

    public void conflatePrice(PriceMessage msg){
        statisticsCollection.messageReceivedFromCpp();

//        log.info("Received {} of {}", msg.getTicker().toString(), msg.getTickPrice().asLong());
        var sm = conflatedPrices.get(msg.getTicker());
        if(sm == null) {
            sm = new StoredPriceMessage();
            sm.fromExisting(msg);
            conflatedPrices.put(sm.getPriceMessage().getTicker(), sm);
        } else {
            sm.fromExisting(msg);
        }
    }

    public boolean receiveChangesIntoList(List<PriceMessage> changedElements) {
        statisticsCollection.conflationEventStarted();
        for(var sm : conflatedPrices.values()) {
            if(sm.isChanged()) {
                sm.resetChangeFlag();
                changedElements.add(sm.getPriceMessage());
            }
        }
        return !changedElements.isEmpty();
    }

    static class StoredPriceMessage {
        @Getter
        private final PriceMessage priceMessage = new PriceMessage();
        private final AtomicBoolean changed = new AtomicBoolean(true);

        public void fromExisting(PriceMessage priceMsg) {
            this.priceMessage.copyDataFromAnother(priceMsg);
            changed.set(true);
        }

        public boolean isChanged() {
            return changed.get();
        }

        public void resetChangeFlag() {
            changed.set(false);
        }
    }
}
