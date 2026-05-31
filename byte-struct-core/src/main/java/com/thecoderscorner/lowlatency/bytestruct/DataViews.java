package com.thecoderscorner.lowlatency.bytestruct;

public class DataViews {
    public static IntegerView ofIntView(int loc) {
        return new IntegerView(loc);
    }

    public static LongView ofLongView(int loc) {
        return new LongView(loc);
    }

    public static Utf8View ofUtf8View(int loc, int len) {
        return new Utf8View(loc, len);
    }
}
