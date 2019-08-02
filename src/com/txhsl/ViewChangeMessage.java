package com.txhsl;

import java.util.Map;

public class ViewChangeMessage extends PrepareRequestMessage {

    public ViewChangeMessage(int from, int height, int view) {
        super(from, height, view);
    }

    @Override
    public Map<String, Integer> getMessage() {
        return super.getMessage();
    }
}
