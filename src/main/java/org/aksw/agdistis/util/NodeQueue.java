package org.aksw.agdistis.util;

import org.aksw.agdistis.graph.Node;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public class NodeQueue {
    private HashMap<String, Integer> map;
    Queue<Node> queue;

    public NodeQueue() throws Exception {
        map = new HashMap<>(10240);
        queue = new LinkedList<>();
    }

    public void add(Node node) throws Exception {
        if (map.containsKey(node.getCandidateURI()))
            return;

        map.put(node.getCandidateURI(), 1);

        queue.add(node);
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public Node poll() {
        return queue.poll();
    }

    public int size(){
        return queue.size();
    }
}
