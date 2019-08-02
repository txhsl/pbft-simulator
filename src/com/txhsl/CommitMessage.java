package com.txhsl;

public class CommitMessage extends PrepareResponseMessage {

    public CommitMessage(int from, PrepareResponseMessage msg) {
        super(from, msg.height, msg.view, msg.primary);
    }
}
