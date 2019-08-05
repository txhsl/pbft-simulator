package com.txhsl;

public class ChangeConfirmMessage extends PrimaryChangeMessage {
    public ChangeConfirmMessage(int from, int height, int primary) {
        super(from, height, primary);
    }
    public ChangeConfirmMessage(PrimaryChangeMessage msg) {
        super(msg.from, msg.height, msg.primary);
    }
}
