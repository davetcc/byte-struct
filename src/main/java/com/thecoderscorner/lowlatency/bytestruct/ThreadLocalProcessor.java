package com.thecoderscorner.lowlatency.bytestruct;


import com.thecoderscorner.lowlatency.bytestruct.utf8.CapturingHandler;
import com.thecoderscorner.lowlatency.bytestruct.utf8.Utf8TextProcessor;

public class ThreadLocalProcessor {
    private static final ThreadLocal<Utf8TextProcessor> processor = ThreadLocal.withInitial(() -> new Utf8TextProcessor(null, null));
    private static final ThreadLocal<CapturingHandler> capturingHandlers = ThreadLocal.withInitial(CapturingHandler::new);

    public static Utf8TextProcessor getProcessorForThread() {
        return processor.get();
    }

    public static CapturingHandler getCapturingHandlerForThread() {
        return capturingHandlers.get();
    }
}
