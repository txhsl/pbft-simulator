package com.txhsl;

import java.util.ArrayList;

public class Test {

    public static void main(String[] args) {

        for (String option : args) {

        }
        Node node0 = new Node(0);
        Node node1 = new Node(1);
        Node node2 = new Node(2);
        Node node3 = new Node(3);

        ArrayList<Node> nodes = new ArrayList<Node>(){{add(node1); add(node2); add(node3);}};
        ArrayList<Node> initial = new ArrayList<Node>(){{add(node0);}};

        node0.importPeers(nodes);
        node1.importPeers(initial);
        node2.importPeers(initial);
        node3.importPeers(initial);

        try {
            Thread.sleep(5000);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }

        node0.startConsensus();
        node1.startConsensus();
        node2.startConsensus();
        node3.startConsensus();

        try {
            Thread.sleep(10000);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        node0.pause();

        while(true) {
            ;
        }
    }
}
