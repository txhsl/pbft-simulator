package com.txhsl;

import java.util.Map;

public class PrepareRequestMessage extends Message {

    public int height;
    public int view;

    public PrepareRequestMessage(int from, int height, int view) {
        super(from);
        this.height = height;
        this.view = view;
    }

    @Override
    public Map<String, Integer> getMessage() {
        Map<String, Integer> content = super.getMessage();
        content.putIfAbsent("height", height);
        content.putIfAbsent("view", view);
        return content;
    }
}
