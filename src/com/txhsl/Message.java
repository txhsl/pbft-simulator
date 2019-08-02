package com.txhsl;

import java.util.HashMap;
import java.util.Map;

public class Message {

    public int from;

    public Message(int from) {
        this.from = from;
    }

    public Map<String, Integer> getMessage() {
        Map<String, Integer> content = new HashMap<>();
        content.putIfAbsent("from", from);
        return content;
    }
}
