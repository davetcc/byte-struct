package com.thecoderscorner.lowlatency.bytestruct.example;

import com.thecoderscorner.lowlatency.bytestruct.*;

/**
 * This is the message class used to receive the byte[] data. You can consider this
 * similar to a POJO without allocation. It both receives and lazily parses the byte
 * array, while also suitable to keep the data in storage for as long as needed.
 */
public class HelloStructMsg extends BaseMessage {
    public enum Foods {
        PIZZA, PASTA, SALAD, BURGER, SANDWICH, CHIPS
    }

    // C++ representation of the message.
    // struct HelloStruct {
    //   int intVal;
    //   long longVal;
    //   int bitField;
    //   char str[32];
    // }

    private final IntegerView intVal = DataViews.ofIntView(0);
    private final LongView longView = DataViews.ofLongView(4);
    private final IntegerView partView = DataViews.ofIntView(12);
    private final Utf8View utf8View = DataViews.ofUtf8View(16, 32);

    public HelloStructMsg() {
        super(128);
        addByteViewListeners(intVal, longView, partView, utf8View);
    }

    public IntegerView getIntVal() {
        return intVal;
    }

    public LongView getLongView() {
        return longView;
    }

    public boolean getBoolean0() {
        return partView.booleanPartial(16);
    }

    public boolean getBoolean1() {
        return partView.booleanPartial(17);
    }

    public Foods getFoods0() {
        return partView.enumPartial(0, 8, Foods.class);
    }

    public Foods getFoods1() {
        return partView.enumPartial(8, 8, Foods.class);
    }

    public Utf8View getUtf8View() {
        return utf8View;
    }
}
