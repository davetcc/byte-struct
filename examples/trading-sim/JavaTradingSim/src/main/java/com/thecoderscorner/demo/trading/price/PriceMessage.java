package com.thecoderscorner.demo.trading.price;

import com.thecoderscorner.lowlatency.bytestruct.*;
import lombok.Getter;

@Getter
public class PriceMessage extends BaseMessage {
    private final Utf8View ticker = DataViews.ofUtf8View(0, 32);
    private final Utf8View source = DataViews.ofUtf8View(32, 16);
    private final LongView millisEpoch = DataViews.ofLongView(48);
    private final LongView tickPrice = DataViews.ofLongView(56);

    public PriceMessage() {
        super(64);
        addByteViewListeners(ticker, source, millisEpoch, tickPrice);
    }
}
