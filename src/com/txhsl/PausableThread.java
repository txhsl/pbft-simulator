package com.txhsl;

public class PausableThread extends Thread {

    protected boolean suspend = false;

    public synchronized void toSuspend(){
        suspend = true;
    }

    public synchronized void toResume(){
        notify();
        suspend = false;
    }
}
