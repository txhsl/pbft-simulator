package com.txhsl;

import java.util.Map;

public class PrimaryChangeMessage extends Message {

    public int height;
    public int primary;

    public PrimaryChangeMessage(int from, int height, int primary) {
        super(from);
        this.primary = primary;
        this.height = height;
    }

    @Override
    public Map<String, Integer> getMessage() {
        Map<String, Integer> content = super.getMessage();
        content.putIfAbsent("height", height);
        content.putIfAbsent("primary", primary);
        return content;
    }
}
