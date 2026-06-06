package com.thecoderscorner.demo.trading.price;

import com.thecoderscorner.demo.trading.stats.StatisticsCollection;
import com.thecoderscorner.lowlatency.bytestruct.Utf8View;
import lombok.extern.slf4j.Slf4j;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
            conflatedPrices.put(sm.getUnconflatedMsg().getTicker(), sm);
        } else {
            sm.fromExisting(msg);
        }
    }

    public boolean receiveChangesIntoList(List<PriceMessage> changedElements) {
        statisticsCollection.conflationEventStarted();
        for(var sm : conflatedPrices.values()) {
            if(sm.isChanged()) {
                sm.resetChangeFlag();
                changedElements.add(sm.getAndCopyConflated());
            }
        }
        return !changedElements.isEmpty();
    }

    /**
     * An internal component that stores the current price message along with a changed status. Whenever it
     * updates the changed flag is set, and later when published, it can be marked as not changed.
     *
     * Thread Safe for single producer and single consumer cases only. Note the use of volatile and using
     * CAS from VarHandle. We can use primatives and improve memory locality slightly.
     *
     * Although VarHandle.compareAndSet looks like it will cause boxing, it does not after JIT as it is
     * marked as instrinct, and gets compiled down to direct machine instructions in most cases.
     */
    class StoredPriceMessage {
        private final PriceMessage publishMsg = new PriceMessage();
        private final PriceMessage priceMessage = new PriceMessage();
        private volatile boolean changed = true;
        private volatile boolean copyingLock;

        private static final VarHandle COPY_LOCK_FIELD;

        static {
            try {
                COPY_LOCK_FIELD = MethodHandles.lookup()
                        .findVarHandle(StoredPriceMessage.class, "copyingLock", boolean.class);
            } catch (Exception e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        public void fromExisting(PriceMessage priceMsg) {
            waitForAccessToCopy();
            try {
                this.priceMessage.copyDataFromAnother(priceMsg);
            } finally {
              copyingLock = false;
            }
            changed = true;
        }

        private void waitForAccessToCopy() {
            int spinCount = 0;
                while ((boolean)COPY_LOCK_FIELD.compareAndSet(this, false, true)) {
                    ++spinCount;
                    if (spinCount > 10000) {
                        Thread.yield();
                        statisticsCollection.blockedLockOnDistribute();
                    }
                }
        }

        public boolean isChanged() {
            return changed;
        }

        public void resetChangeFlag() {
            changed = false;
        }

        public PriceMessage getUnconflatedMsg() {
            return priceMessage;
        }

        public PriceMessage getAndCopyConflated() {
            try {
                waitForAccessToCopy();
                publishMsg.copyDataFromAnother(priceMessage);
            } finally {
                copyingLock = false;
            }
            return publishMsg;
        }
    }
}
