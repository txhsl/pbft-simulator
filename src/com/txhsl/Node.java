package com.txhsl;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class Node {

    public enum State {Waiting, Initial, Primary, BackUp, Prepared, Committed, ViewChanging, PrimaryChanging}

    public int name;
    public int height = 0;
    public int view = 0;
    public int targetView = 0;
    public int voteCounter = 0;
    public int viewChangeCounter = 0;
    public int primaryChangeCounter = 0;
    public State state = State.Waiting;
    public boolean signal = false;

    private static long DELAY = 1000;
    private static long BLOCK_TIME = 5000;
    private static long WAIT_LIMIT = 5000;
    private static int INITIAL_CREDIT = 5;
    private static int CREDIT_LIMIT = 5;

    public ArrayList<Node> peers = new ArrayList<>();
    public Map<Integer, Integer> credits = new HashMap<>();
    public Queue<Message> messages = new LinkedBlockingQueue<>();

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
                state = Node.State.Initial;

                if (isPrimary(height, view)){
                    state = Node.State.Primary;
                    voteCounter += 1;
                    broadcast(new PrepareRequestMessage(name, height, view));
                }
                else {
                    state = Node.State.BackUp;
                }
                signal = true;

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
                                targetView = view + 1;
                                broadcast(new ViewChangeMessage(name, height, targetView));
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
                    System.out.println("[Node " + name + "] Consensus success. Height: " + height + ", view: " + view);
                    view = 0;
                    targetView = 0;
                    height += 1;
                    state = Node.State.Waiting;

                    //optional primaryChange
                    if (!isPrimary(height + 1, 0) && credits.get(getPrimary(height + 1, 0)) < CREDIT_LIMIT) {
                        primaryChangeCounter += 1;
                        broadcast(new PrimaryChangeMessage(name, height + 1, getPrimary(height + 1, 1)));
                    }
                }
                else {
                    System.out.println("[Node " + name + "] Consensus failed, view changing. Height: " + height + ", view: " + view);
                    view = targetView;
                    System.out.println("[Node " + name + "] View changed, number: " + view);
                    continue;
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
            peer.messages.add(msg);
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
            if (state == State.BackUp) {
                if (msg.getMessage().get("height") != height || msg.getMessage().get("view") != view) {
                    return;
                }
                voteCounter += 1;
                broadcast(new PrepareResponseMessage(name, (PrepareRequestMessage) msg));
                voteCounter += 1;
                signal = true;
            }
        }
        else if (msg.getClass() == PrepareResponseMessage.class) {
            if (state == State.Primary || state == State.BackUp) {
                if (msg.getMessage().get("height") != height || msg.getMessage().get("view") != view) {
                    return;
                }
                voteCounter += 1;
                if (getEnoughVote(voteCounter)) {
                    state = State.Prepared;
                    broadcast(new CommitMessage(name, (PrepareResponseMessage) msg));
                    voteCounter = 0;
                    signal = true;
                }
            }
        }
        else if (msg.getClass() == CommitMessage.class) {
            if (state == State.Prepared) {
                if (msg.getMessage().get("height") != height || msg.getMessage().get("view") != view) {
                    return;
                }
                voteCounter += 1;
                if (getEnoughVote(voteCounter)) {
                    state = State.Committed;

                    //commit something
                    System.out.println("[Node " + name + "] Commits. But nothing is done here");
                }
            }
        }
        else if (msg.getClass() == ViewChangeMessage.class) {
            if (state != State.Committed && state != State.Initial) {
                if (msg.getMessage().get("height") != height || msg.getMessage().get("view") < view + 1) {
                    return;
                }
                viewChangeCounter += 1;
                if (msg.getMessage().get("view") > targetView) {
                    targetView = msg.getMessage().get("view");
                }
                if (getEnoughVote(viewChangeCounter)) {
                    state = State.ViewChanging;
                }
            }
        }
        else if (msg.getClass() == PrimaryChangeMessage.class) {
            if (state == State.Waiting){
                if (msg.getMessage().get("height") != height + 1 || !isPrimary(msg.getMessage().get("primary"), height + 1, 1)) {
                    return;
                }
                primaryChangeCounter += 1;
                if (getEnoughVote(primaryChangeCounter)) {
                    state = State.PrimaryChanging;
                    view = 1;
                    state = State.Waiting;
                }
            }
        }
    }

    private boolean getEnoughVote(int voteCount) {
        return voteCount >= 2 * (peers.size() + 1) / 3 + 1;
    }

    private boolean isPrimary(int height, int view) {
        return (height + view) % (peers.size() + 1) == name;
    }

    private boolean isPrimary(int name, int height, int view) {
        return (height + view) % (peers.size() + 1) == name;
    }

    private int getPrimary(int height, int view) {
        return (height + view) % (peers.size() + 1);
    }
}
