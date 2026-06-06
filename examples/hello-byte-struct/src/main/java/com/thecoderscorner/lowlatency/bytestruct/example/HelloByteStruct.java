package com.thecoderscorner.lowlatency.bytestruct.example;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * The simplest byte struct example possible, we just full up a byte array using a buffer, and
 * then read it back using a byte struct message.
 */
public class HelloByteStruct {
    private final byte[] someData = new byte[128];

    public HelloByteStruct() {
        // struct HelloStruct {
        //   int intVal;
        //   long longVal;
        //   int bitField;
        //   char str[32];
        // }

        // first we fill up the buffer, normally this would be received from another system
        // but here we simulate it using a byte buffer.
        ByteBuffer buffer = ByteBuffer.allocate(128);
        buffer.order(ByteOrder.nativeOrder());
        buffer.putInt(0x80238321); // 0..3
        buffer.putLong(0xabcdef0123456789L); // 4..11

        buffer.put((byte) HelloStructMsg.Foods.PIZZA.ordinal()); // 12..15
        buffer.put((byte) HelloStructMsg.Foods.SALAD.ordinal());
        buffer.put((byte) 0b00000010);
        buffer.put((byte) 0);

        buffer.put("Hello world this is byte struct".getBytes(StandardCharsets.UTF_8));
        buffer.flip();
        buffer.get(someData, 0, buffer.remaining());
    }

    public void doSomeStructs() {
        // we create our Java struct representation
        var structMsg = new HelloStructMsg();
        structMsg.copyDataFromRawData(someData);

        // then we access the fields within it.
        System.out.printf("Int value: 0x%04x%n", structMsg.getIntVal().asInt());
        System.out.printf("Long value: 0x%08x%n", structMsg.getLongView().asLong());
        System.out.println("Foods value: " + structMsg.getFoods0());
        System.out.println("Foods value: " + structMsg.getFoods1());
        System.out.println("Bool0 value: " + structMsg.getBoolean0());
        System.out.println("Bool1 value: " + structMsg.getBoolean1());

        // Although we're calling toString on the UTF8 view here, if you're populating
        // a container with them, they properly implement Comparable, hash and equality
        System.out.println("UTF8 view: " + structMsg.getUtf8View());
    }

    public static void main(String[] args) {
        var hello = new HelloByteStruct();
        hello.doSomeStructs();
    }
}