package com.txhsl;

import java.util.Map;

public class PrimaryChangeMessage extends Message {

    public int height;
    public Node primary;

    public PrimaryChangeMessage(int from, int height, Node primary) {
        super(from);
        this.primary = primary;
    }

    @Override
    public Map<String, Integer> getMessage() {
        Map<String, Integer> content = super.getMessage();
        content.putIfAbsent("height", height);
        content.putIfAbsent("primary", primary.name);
        return content;
    }
}
