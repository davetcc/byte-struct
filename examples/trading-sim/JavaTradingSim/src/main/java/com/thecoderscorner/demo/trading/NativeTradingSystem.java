package com.thecoderscorner.demo.trading;

import com.thecoderscorner.demo.trading.price.PriceConflationService;
import com.thecoderscorner.demo.trading.price.PriceMessage;
import com.thecoderscorner.demo.trading.staticdata.StaticDataService;
import com.thecoderscorner.demo.trading.staticdata.StaticMessage;
import lombok.extern.slf4j.Slf4j;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.file.Path;
import java.time.Instant;

@Slf4j
public class NativeTradingSystem {
    private final MethodHandle initFunction;
    private final MethodHandle acquireNextPriceFunction;
    private final MethodHandle startStaticIteratorFuntion;
    private final MethodHandle nextStaticItemFunction;
    private final PriceConflationService conflationService;
    private final StaticDataService staticDataService;
    private volatile Thread priceThread;

    NativeTradingSystem(Path libPath, PriceConflationService conflationService, StaticDataService staticDataService) {
        this.conflationService = conflationService;
        this.staticDataService = staticDataService;
        System.load(libPath.toAbsolutePath().toString());
        Linker linker = Linker.nativeLinker();
        SymbolLookup libLookup = SymbolLookup.loaderLookup();

        log.info("Loaded native trading library from {}", libPath);

        // bool tradingInit()
        initFunction = linker.downcallHandle(
                libLookup.find("tradingInit").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN)
        );

        //bool startStaticIterator()
        startStaticIteratorFuntion = linker.downcallHandle(
                libLookup.find("startStaticIterator").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN)
        );

        //bool nextStaticItem(StaticMessage* toCopyInto)
        nextStaticItemFunction = linker.downcallHandle(
                libLookup.find("nextStaticItem").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN, ValueLayout.ADDRESS)
        );

        // bool acquireNextPrice(PriceMessage* toCopyInto)
        acquireNextPriceFunction = linker.downcallHandle(
                libLookup.find("acquireNextPrice").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN, ValueLayout.ADDRESS)
        );

        log.info("Found methods in library, now starting native component");

        initialiseLibrary();
        acquireStaticData();
        startPriceAcquisition();
    }

    private void acquireStaticData() {
        try(var arena = Arena.ofConfined()) {
            var data = arena.allocate(256);
            var received = (boolean)startStaticIteratorFuntion.invokeExact();
            while(received) {
                received = (boolean) nextStaticItemFunction.invokeExact(data);
                if(received) {
                    StaticMessage message = new StaticMessage();
                    byte[] msgBytes = message.getUnderlyingData();
                    MemorySegment.copy(data, ValueLayout.JAVA_BYTE, 0, msgBytes, 0, msgBytes.length);
                    message.existingArrayChanged();
                    staticDataService.addStaticData(message);
                }
            }
        } catch(Throwable ex) {
            log.error("Failed to acquire static data from native library", ex);
        }
    }

    private void startPriceAcquisition() {
        priceThread = new Thread(() -> {
           try(var arena = Arena.ofConfined()) {
               PriceMessage priceMessage = new PriceMessage();
               var data = arena.allocate(256);
               while(!Thread.currentThread().isInterrupted()) {
                   boolean result = false;
                   try {
                       result = (boolean)acquireNextPriceFunction.invokeExact(data);
                       if(result) {
                           byte[] dest = priceMessage.getUnderlyingData();
                           MemorySegment.copy(data, ValueLayout.JAVA_BYTE, 0, dest, 0, dest.length);
                           priceMessage.existingArrayChanged();
                           conflationService.conflatePrice(priceMessage);
                       } else {
                           log.error("Didn't Acquire price: {} with {}", result, data);
                       }
                   } catch (Throwable e) {
                       throw new RuntimeException(e);
                   }
                   if(!result) break;
               }
           }
        });
        priceThread.start();
    }

    public void stopLibrary() {
        if(priceThread != null) {
            priceThread.interrupt();
            priceThread = null;
        }
    }

    private void initialiseLibrary() {
        // start the native library
        try(var _ = Arena.ofConfined()) {
            boolean result = (boolean)initFunction.invokeExact();
            if(!result) throw new RuntimeException("Failed to initialize native trading system");
            log.info("Native component started successfully");
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

}
