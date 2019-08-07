package com.txhsl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Main {

    private static int NODE_AMOUNT = 4;
    private static long DELAY = 100;
    private static long BLOCK_TIME = 2000;
    private static long WAIT_LIMIT = 2000;
    private static int CREDIT_LIMIT = 3;
    private static long DURATION = 60000;

    public static void main(String[] args) {

        if (args.length < 1) {
            return;
        }

        switch (args[0]) {
            case "test":
                Map<Integer, Long> breakPoint = new HashMap<>();
                for(int i = 1; i < args.length; i++) {
                    switch (args[i]) {
                        case "--node":
                            NODE_AMOUNT = Integer.valueOf(args[i + 1]);
                            break;
                        case "--delay":
                            DELAY = Long.valueOf(args[i + 1]);
                            break;
                        case "--blocktime":
                            BLOCK_TIME = Long.valueOf(args[i + 1]);
                            break;
                        case "--waitlimit":
                            WAIT_LIMIT = Long.valueOf(args[i + 1]);
                            break;
                        case "--creditlimit":
                            CREDIT_LIMIT = Integer.valueOf(args[i + 1]);
                            break;
                        case "--duration":
                            DURATION = Long.valueOf(args[i + 1]);
                            break;
                        case "--breakpoint":
                            breakPoint.putIfAbsent(Integer.valueOf(args[i + 1]), Long.valueOf(args[i + 2]));
                            break;
                        default:
                            if (args[i].contains("--")) {
                                System.out.println("Unknown option " + args[i]);
                            }
                    }
                }

                ArrayList<Node> nodeList = new ArrayList<>();
                for (int i = 0; i < NODE_AMOUNT; i++) {
                    Node node = new Node(i);
                    node.setBlockTime(BLOCK_TIME);
                    node.setCreditLimit(CREDIT_LIMIT);
                    node.setDelay(DELAY);
                    node.setWaitLimit(WAIT_LIMIT);
                    nodeList.add(node);
                }

                ArrayList<Node> peers = new ArrayList<Node>() {{
                    for (int i = 1; i < nodeList.size(); i++) {
                        add(nodeList.get(i));
                    }
                }};
                ArrayList<Node> initialNode = new ArrayList<Node>(){{
                    add(nodeList.get(0));
                }};

                nodeList.get(0).importPeers(peers);
                for (int i = 1; i < nodeList.size(); i++) {
                    nodeList.get(i).importPeers(initialNode);
                }

                try {
                    Thread.sleep(5000);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }

                for (Node node : nodeList) {
                    node.startConsensus();
                }

                for (Integer node : breakPoint.keySet()) {
                    nodeList.get(node).addBreakPoint(breakPoint.get(node));
                }

                try {
                    Thread.sleep(DURATION);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
                for (Node node : nodeList) {
                    node.stop();
                }
                break;
            case "run":
                for(int i = 1; i < args.length; i++) {
                    switch (args[i]) {
                        case "--node":
                            NODE_AMOUNT = Integer.valueOf(args[i + 1]);
                            break;
                        case "--delay":
                            DELAY = Long.valueOf(args[i + 1]);
                            break;
                        case "--blocktime":
                            BLOCK_TIME = Long.valueOf(args[i + 1]);
                            break;
                        case "--waitlimit":
                            WAIT_LIMIT = Long.valueOf(args[i + 1]);
                            break;
                        case "--creditlimit":
                            CREDIT_LIMIT = Integer.valueOf(args[i + 1]);
                            break;
                        default:
                            if (args[i].contains("--")) {
                                System.out.println("Unknown option " + args[i]);
                            }
                    }
                }
                ArrayList<Node> nodeList_ = new ArrayList<>();
                for (int i = 0; i < NODE_AMOUNT; i++) {
                    Node node = new Node(i);
                    node.setBlockTime(BLOCK_TIME);
                    node.setCreditLimit(CREDIT_LIMIT);
                    node.setDelay(DELAY);
                    node.setWaitLimit(WAIT_LIMIT);
                    nodeList_.add(node);
                }

                ArrayList<Node> peers_ = new ArrayList<Node>() {{
                    for (int i = 1; i < nodeList_.size(); i++) {
                        add(nodeList_.get(i));
                    }
                }};
                ArrayList<Node> initialNode_ = new ArrayList<Node>(){{
                    add(nodeList_.get(0));
                }};

                nodeList_.get(0).importPeers(peers_);
                for (int i = 1; i < nodeList_.size(); i++) {
                    nodeList_.get(i).importPeers(initialNode_);
                }

                try {
                    Thread.sleep(5000);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }

                for (Node node : nodeList_) {
                    node.startConsensus();
                }
                break;
            case "help":
                System.out.println("Available commands:");
                System.out.println("run    (--node, --delay, --blocktime, --waitlimit, --creditlimit, --duration)");
                break;
            default:
                System.out.println("Unknown cmd " + args[1]);
        }
    }
}
