package com.thecoderscorner.demo.trading.stats;

import lombok.AllArgsConstructor;
import lombok.Value;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class StatisticsCollatorService implements StatisticsCollection {
    private final AtomicLong messagesReceivedFromCpp = new AtomicLong(0);
    private final AtomicLong numberOfConflations = new AtomicLong(0);
    private final AtomicLong currentJavaHeapTotal = new AtomicLong(0);
    private final AtomicLong totalJvmRuntimeSoFar = new AtomicLong(0);
    private final AtomicLong numberOfBlockedLocks = new AtomicLong(0);
    private final long whenStarted = System.currentTimeMillis();
    private final Sinks.Many<StatisticsPojo> statisticsSink = Sinks.many().multicast().directBestEffort();

    public StatisticsCollatorService(ScheduledExecutorService executorService) {
        executorService.scheduleAtFixedRate(this::refreshStats, 0, 200, TimeUnit.MILLISECONDS);
    }

    private void refreshStats() {
        currentJavaHeapTotal.set(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        totalJvmRuntimeSoFar.set(System.currentTimeMillis() - whenStarted);
        statisticsSink.emitNext(new StatisticsPojo(
                messagesReceivedFromCpp.get(),
                numberOfConflations.get(),
                currentJavaHeapTotal.get(),
                totalJvmRuntimeSoFar.get(),
                numberOfBlockedLocks.get()
        ), Sinks.EmitFailureHandler.FAIL_FAST);
    }

    @Override
    public void messageReceivedFromCpp() {
        messagesReceivedFromCpp.incrementAndGet();
    }

    @Override
    public void conflationEventStarted() {
        numberOfConflations.incrementAndGet();
    }

    @Override
    public void blockedLockOnDistribute() {
        numberOfBlockedLocks.incrementAndGet();
    }

    public StatisticsPojo getLatest() {
        return new StatisticsPojo(
                messagesReceivedFromCpp.get(),
                numberOfConflations.get(),
                currentJavaHeapTotal.get(),
                totalJvmRuntimeSoFar.get(),
                numberOfBlockedLocks.get()
        );
    }

    public Flux<StatisticsPojo> getStatisticsFlux() {
        return statisticsSink.asFlux();
    }

    @AllArgsConstructor
    @Value
    public static class StatisticsPojo {
        long messagesReceivedFromCpp;
        long numberOfConflations;
        long currentJavaHeapTotal;
        long totalJvmRuntimeSoFar;
        long distributionBlocking;
    }
}
