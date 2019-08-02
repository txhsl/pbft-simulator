package com.txhsl;

import java.util.Map;

public class PrepareResponseMessage extends PrepareRequestMessage {

    public int primary;

    public PrepareResponseMessage(int from, PrepareRequestMessage request) {
        super(from, request.height, request.view);
        this.primary = request.from;
    }

    public PrepareResponseMessage(int from, int height, int view ,int primary) {
        super(from, height, view);
        this.primary = primary;
    }

    @Override
    public Map<String, Integer> getMessage() {
        Map<String, Integer> content = super.getMessage();
        content.putIfAbsent("primary", primary);
        return content;
    }
}
