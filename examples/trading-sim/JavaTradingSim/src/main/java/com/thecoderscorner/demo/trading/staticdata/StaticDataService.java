package com.thecoderscorner.demo.trading.staticdata;

import com.thecoderscorner.lowlatency.bytestruct.Utf8View;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class StaticDataService {
    private final ConcurrentMap<Utf8View, StaticMessage> staticDataMap = new ConcurrentHashMap<>(256);

    public void addStaticData(StaticMessage message) {
        staticDataMap.put(message.getTicker(), message);
    }

    public StaticMessage getStaticData(Utf8View ticker) {
        return staticDataMap.get(ticker);
    }

    public Collection<StaticMessage> getAllStaticData() {
        return staticDataMap.values();
    }
}
