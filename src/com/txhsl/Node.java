package com.txhsl;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class Node {

    public enum State {Waiting, Initial, Primary, BackUp, Prepared, Committed, ViewChanging, PrimaryChanging}

    private int name;
    private int height = 0;
    private int view = 0;
    private int targetView = 0;
    private int voteCounter = 0;
    private int viewChangeCounter = 0;
    private int primaryChangeCounter = 0;
    private int comfirmChangeCounter = 0;
    private State state = State.Waiting;
    public Map<Integer, boolean[]> msgRecord = new HashMap<>();
    private boolean signal = false;

    private static long DELAY = 100;
    private static long BLOCK_TIME = 2000;
    private static long WAIT_LIMIT = 2000;
    private static int INITIAL_CREDIT = 5;
    private static int CREDIT_LIMIT = 3;

    private ArrayList<Node> peers = new ArrayList<>();
    private Map<Integer, Integer> credits = new HashMap<>();
    private Queue<Message> messages = new LinkedBlockingQueue<>();

    private PausableThread discoverThread = new PausableThread() {

        @Override
        public void run() {
            while(true) {

                synchronized (this) {
                    while(suspend){
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                for (Node peer : peers) {
                    ArrayList<Node> copy = new ArrayList<>(peer.peers);
                    for (Node node : copy) {
                        if (!peers.contains(node) && node.name != name) {
                            peers.add(node);
                            credits.putIfAbsent(node.name, INITIAL_CREDIT);
                        }
                    }
                }

                try {
                    Thread.sleep(DELAY);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private PausableThread handlerThread = new PausableThread() {
        @Override
        public void run() {
            while(true) {

                synchronized (this) {
                    while(suspend){
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                //handle all
                while (!messages.isEmpty()) {
                    handle(messages.poll());
                }

                try {
                    Thread.sleep(DELAY);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private PausableThread consensusThread = new PausableThread() {
        @Override
        public void run() {
            while(true) {
                synchronized (this) {
                    while(suspend){
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                //judge and start
                voteCounter = 0;
                primaryChangeCounter = 0;
                msgRecord = new HashMap<>();
                for (int name : credits.keySet()) {
                    msgRecord.putIfAbsent(name, new boolean[2]);
                }
                state = Node.State.Initial;

                if (isPrimary(height, view)){
                    state = Node.State.Primary;
                    try {
                        Thread.sleep(DELAY);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    voteCounter += 1;
                    broadcast(new PrepareRequestMessage(name, height, view));
                }
                else {
                    state = Node.State.BackUp;
                }
                signal = true;

                int stage = 0;
                while(state != Node.State.Committed && state != Node.State.ViewChanging) {
                    synchronized (this) {
                        if (signal && state != Node.State.Waiting) {
                            Node.State temp = state;
                            signal = false;

                            long startPoint = System.currentTimeMillis();

                            while (System.currentTimeMillis() - startPoint < WAIT_LIMIT && state == temp) {
                                try {
                                    Thread.sleep(DELAY);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            if (state == temp) {
                                switch (state) {
                                    case Primary:
                                    case BackUp:
                                        stage = 1;
                                        break;
                                    case Prepared:
                                        stage = 2;
                                }

                                broadcast(new ViewChangeMessage(name, height, view + 1));
                                viewChangeCounter += 1;
                            }
                        } else {
                            try {
                                Thread.sleep(DELAY);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

                if (state == Node.State.Committed) {
                    String log = "[Node " + name + "] Consensus success. Height: " + height + ", view: " + view + ". Credits:";
                    for (int name : credits.keySet()) {
                        log = log + " " + name + "-" + credits.get(name);
                    }
                    System.out.println(log);
                    view = 0;
                    targetView = 0;
                    comfirmChangeCounter = 0;
                    height += 1;
                    state = Node.State.Waiting;

                    //optional primaryChange
                    try {
                        Thread.sleep(DELAY);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    for (int name : credits.keySet()) {
                        updateCredit(name, msgRecord.get(name), 3);
                    }
                    if (!isPrimary(height, 0) && credits.get(getPrimary(height, 0)) < CREDIT_LIMIT) {
                        primaryChangeCounter += 1;
                        broadcast(new PrimaryChangeMessage(name, height, getPrimary(height, 1)));
                    }
                }
                else {
                    String log = "[Node " + name + "] Consensus failed, view changing. Height: " + height + ", view: " + view + ". Credits:";
                    for (int name : credits.keySet()) {
                        log = log + " " + name + "-" + credits.get(name);
                    }
                    System.out.println(log);
                    view = targetView;
                    comfirmChangeCounter = 0;
                    state = Node.State.Waiting;
                    System.out.println("[Node " + name + "] View changed, number: " + view);

                    for (int name : credits.keySet()) {
                        updateCredit(name, msgRecord.get(name), stage);
                    }
                    if (!isPrimary(height, 0) && credits.get(getPrimary(height, 0)) < CREDIT_LIMIT) {
                        targetView = view + 1;
                        primaryChangeCounter += 1;
                        broadcast(new PrimaryChangeMessage(name, height, getPrimary(height, targetView)));
                    }
                }

                try {
                    Thread.sleep(BLOCK_TIME);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    public Node(int name) {
        this.name = name;
        handlerThread.start();
        discoverThread.start();
    }
    public void setBlockTime(long mills) {
        BLOCK_TIME = mills;
    }

    public void setWaitLimit(long mills) {
        WAIT_LIMIT = mills;
    }

    public void setDelay(long mills) {
        DELAY = mills;
    }

    public void setCreditLimit(int limit) {
        CREDIT_LIMIT = limit;
    }

    public void startConsensus() {
        consensusThread.start();
    }

    public void pause() {
        discoverThread.toSuspend();
        handlerThread.toSuspend();
        consensusThread.toSuspend();
    }

    public void resume() {
        discoverThread.toResume();
        handlerThread.toResume();
        consensusThread.toResume();
    }

    public void stop() {
        discoverThread.stop();
        handlerThread.stop();
        consensusThread.stop();
    }

    public void addBreakPoint(long mills) {
        Thread breakPoint = new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(mills);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                pause();
            }
        };
        breakPoint.start();
    }

    public void importPeers(List<Node> nodes) {
        this.peers.addAll(nodes);
        for (Node node : nodes) {
            credits.putIfAbsent(node.name, INITIAL_CREDIT);
        }
    }

    private void deliver(Message msg, Node peer) {
        peer.messages.add(msg);
    }

    private void broadcast(Message msg) {
        for (Node peer : peers) {
            peer.addMsg(msg);
        }
    }

    private void addMsg(Message msg) {
        synchronized (this) {
            messages.add(msg);
        }
    }

    private void handle(Message msg) {

        Map<String, Integer> content = msg.getMessage();
        String log = "[Node " + name + "] " + msg.getClass().getName() + " received: {";

        for (String key : content.keySet()) {
            log = log + key + ": " + content.get(key) + " ";
        }
        System.out.println(log + "}");

        if (msg.getClass() == PrepareRequestMessage.class) {
            if (msg.getMessage().get("height") != height || msg.getMessage().get("view") != view) {
                return;
            }
            if (state == State.BackUp) {
                voteCounter += 1;
                broadcast(new PrepareResponseMessage(name, (PrepareRequestMessage) msg));
                voteCounter += 1;
                signal = true;

                msgRecord.get(msg.from)[0] = true;
            }
        }
        else if (msg.getClass() == PrepareResponseMessage.class) {
            if (msg.getMessage().get("height") != height || msg.getMessage().get("view") != view) {
                return;
            }
            if (state == State.Primary || state == State.BackUp) {
                voteCounter += 1;
                if (hasEnoughVote(voteCounter)) {
                    state = State.Prepared;
                    broadcast(new CommitMessage(name, (PrepareResponseMessage) msg));
                    voteCounter = 1;
                    signal = true;
                }
                msgRecord.get(msg.from)[0] = true;
            }
        }
        else if (msg.getClass() == CommitMessage.class) {
            if (msg.getMessage().get("height") != height || msg.getMessage().get("view") != view) {
                return;
            }
            if (state == State.Prepared) {
                voteCounter += 1;
                if (hasEnoughVote(voteCounter)) {
                    state = State.Committed;

                    //commit something
                    System.out.println("[Node " + name + "] Commits. But nothing is done here");
                }
                msgRecord.get(msg.from)[1] = true;
            }
        }
        else if (msg.getClass() == ViewChangeMessage.class) {
            if (msg.getMessage().get("height") != height || msg.getMessage().get("view") < view + 1) {
                return;
            }
            if (state != State.Committed && state != State.Initial) {
                viewChangeCounter += 1;
                if (msg.getMessage().get("view") > targetView) {
                    targetView = msg.getMessage().get("view");
                }
                if (hasEnoughVote(viewChangeCounter)) {
                    state = State.ViewChanging;
                }
            }
        }
        else if (msg.getClass() == PrimaryChangeMessage.class) {
            if (msg.getMessage().get("height") != height) {
                return;
            }
            if (state == State.Waiting) {
                primaryChangeCounter += 1;
                if (hasEnoughPChangeVote(primaryChangeCounter)) {
                    state = State.PrimaryChanging;
                    comfirmChangeCounter += 1;
                    broadcast(new ChangeConfirmMessage((PrimaryChangeMessage) msg));
                }
            }
        }
        else if (msg.getClass() == ChangeConfirmMessage.class) {
            if (msg.getMessage().get("height") != height) {
                return;
            }
            if (state == State.Waiting || state == State.PrimaryChanging) {
                comfirmChangeCounter += 1;
                if (hasEnoughVote(comfirmChangeCounter)) {
                    state = State.Waiting;
                    primaryChangeCounter = 0;
                    view += 1;
                    System.out.println("[Node " + name + "] Primary changed, now is Node " + getPrimary(height, view));
                }
            }
        }
    }

    private boolean hasEnoughVote(int voteCount) {
        return voteCount >= 2 * (peers.size() + 1) / 3 + 1;
    }

    private boolean hasEnoughPChangeVote(int voteCount) {
        return voteCount >= 2 * peers.size() / 3;
    }

    private boolean isPrimary(int height, int view) {
        return isPrimary(name, height, view);
    }

    private boolean isPrimary(int name, int height, int view) {
        return (height + view) % (peers.size() + 1) == name;
    }

    private int getPrimary(int height, int view) {
        return (height + view) % (peers.size() + 1);
    }

    private int calculateCredit(int old, boolean success) {
        return success ? old / 2 + 5 : old / 2;
    }

    private void updateCredit(int name, boolean[] record, int stage) {
        int credit = credits.get(name);
        if (stage > 0) {
            credit = calculateCredit(credit, record[0]);
        }
        if (stage > 1) {
            credit = calculateCredit(credit, record[1]);
        }
        credits.put(name, credit);
    }
}
