package com.txhsl;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class Node {

    public enum State {Waiting, Initial, Primary, BackUp, Prepared, Committed, ViewChanging, PrimaryChanging}

    public int name;
    public int height = 0;
    public int view = 0;
    public int voteCounter = 0;
    public int viewChangeCounter = 0;
    public State state = State.Waiting;
    public boolean signal = false;

    private static long DELAY = 1000;
    private static long BLOCKTIME = 5000;
    private static long WAITLIMIT = 5000;

    public ArrayList<Node> peers = new ArrayList<>();
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
                view = 0;
                voteCounter = 0;
                state = Node.State.Initial;

                if ((height + 1) % peers.size() == name){
                    state = Node.State.Primary;
                    voteCounter += 1;
                    broadcast(new PrepareRequestMessage(name, height, view));
                }
                else {
                    state = Node.State.BackUp;
                }
                signal = true;

                while(state != Node.State.Committed && state != Node.State.Waiting) {
                    synchronized (this) {
                        if (signal && state != Node.State.Waiting) {
                            Node.State temp = state;
                            signal = false;

                            long startPoint = System.currentTimeMillis();

                            while (System.currentTimeMillis() - startPoint < WAITLIMIT && state == temp) {
                                try {
                                    Thread.sleep(DELAY);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            if (state == temp) {
                                broadcast(new ViewChangeMessage(name, height, view + 1));
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

                try {
                    Thread.sleep(BLOCKTIME);
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
                signal = true;
            }
        }
        else if (msg.getClass() == PrepareResponseMessage.class) {
            if (state == State.Primary || state == State.BackUp) {
                if (msg.getMessage().get("height") != height || msg.getMessage().get("view") != view) {
                    return;
                }
                voteCounter += 1;
                if (voteCounter >= 2 * peers.size() / 3 + 1) {
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
                if (voteCounter >= 2 * peers.size() / 3 + 1) {
                    state = State.Committed;

                    //commit something
                    height += 1;
                    System.out.println("[Node " + name + "] Commits. Height: " + height);
                    state = State.Waiting;
                }
            }
        }
        else if (msg.getClass() == ViewChangeMessage.class) {
            if (state != State.Committed && state != State.Initial) {
                if (msg.getMessage().get("height") != height || msg.getMessage().get("view") != view + 1) {
                    return;
                }
                viewChangeCounter += 1;
                if (viewChangeCounter >= 2 * peers.size() / 3 + 1) {
                    state = State.ViewChanging;
                    view += 1;
                    state = State.Initial;
                }
            }
        }
        else if (msg.getClass() == PrimaryChangeMessage.class) {
            if (state == State.Committed){

            }
        }
    }
}
