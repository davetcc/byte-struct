package com.thecoderscorner.demo.trading.staticdata;

import com.thecoderscorner.lowlatency.bytestruct.*;
import lombok.Getter;

public class StaticMessage extends BaseMessage {
    @Getter
    private final Utf8View ticker = DataViews.ofUtf8View(0, 32);
    private final IntegerView tradeInfo = DataViews.ofIntView(32);

    public StaticMessage() {
        super(36);
        addByteViewListeners(ticker, tradeInfo);
    }

    public boolean isTradeable() { return tradeInfo.booleanPartial(0); }
    public boolean isPreMarket() { return tradeInfo.booleanPartial(1); }
    public boolean isBlocked() { return tradeInfo.booleanPartial(2); }
    public TradeableVenue getTradeableVenue() { return tradeInfo.enumPartial(3, 6, TradeableVenue.class); }
    public ProductType getProductType() { return tradeInfo.enumPartial(9, 6, ProductType.class); }
    public int getTicksPerPoint() { return tradeInfo.intPartial(15, 16); }
}
