package msqueue;

import kotlinx.atomicfu.AtomicRef;

public class MSQueue implements Queue {
    private AtomicRef<Node> head;
    private AtomicRef<Node> tail;

    public MSQueue() {
        Node dummy = new Node(0);
        this.head = new AtomicRef<Node>(dummy);
        this.tail = new AtomicRef<Node>(dummy);
    }

    @Override
    public void enqueue(int x) {
        Node newTail = new Node(x);
        while (true) {
            Node curTail = tail.getValue();
            Node nextTail = curTail.next.getValue();
            if (nextTail == null) {
                if (curTail.next.compareAndSet(null, newTail)) {
                    tail.compareAndSet(curTail, newTail);
                    return;
                }
            } else {
                tail.compareAndSet(curTail, nextTail);
            }
        }
    }

    @Override
    public int dequeue() {
        while (true) {
            Node curHead = head.getValue();
            Node nextHead = curHead.next.getValue();
            Node curTail = tail.getValue();
            if (curTail == curHead) {
                if (nextHead == null) {
                    return Integer.MIN_VALUE;
                } else {
                    tail.compareAndSet(curTail, nextHead);
                }
            } else {
                nextHead = curHead.next.getValue();
                if (head.compareAndSet(curHead, nextHead)) {
                    return nextHead.x;
                }
            }
        }
    }

    @Override
    public int peek() {
        Node curHead = head.getValue();
        Node nextHead = curHead.next.getValue();
        Node curTail = tail.getValue();
        if (nextHead == null) {
            return Integer.MIN_VALUE;
        } else {
            if (curHead == curTail)
                return Integer.MIN_VALUE;
            return nextHead.x;
        }
    }

    private class Node {
        final int x;
        AtomicRef<Node> next;

        Node(int x) {
            this.x = x;
            this.next = new AtomicRef<>(null);
        }
    }
}