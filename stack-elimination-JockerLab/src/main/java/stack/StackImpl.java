package stack;

import kotlinx.atomicfu.AtomicArray;
import kotlinx.atomicfu.AtomicBoolean;
import kotlinx.atomicfu.AtomicInt;
import kotlinx.atomicfu.AtomicRef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class StackImpl implements Stack {
    private static class Node {
        final AtomicRef<Node> next;
        final int x;

        Node(int x, Node next) {
            this.next = new AtomicRef<>(next);
            this.x = x;
        }
    }

    private class Element {
        private boolean done;
        private int val;

        public Element(int x) {
            val = x;
            done = false;
        }

        public Element(boolean b, int x) {
            val = x;
            done = b;
        }

        public boolean isDone() {
            return done;
        }

        public int getVal() {
            return val;
        }

        public void setDone(boolean done) {
            this.done = done;
        }
    }

    // head pointer
    private AtomicRef<Node> head = new AtomicRef<>(null);

    private final int ARRAY_SIZE = 4, CHECKED_ELEMENTS = 2, SPIN_WAIT_COUNT = 4;
    private Random random = new Random();
    //elimination array
    private List<AtomicRef<Element>> array = new ArrayList<>();
    AtomicBoolean init = new AtomicBoolean(false);

    @Override
    public void push(int x) {
        if (!init.getValue()) {
            for (int i = 0 ; i < ARRAY_SIZE; i++) {
                array.add(new AtomicRef<Element>(null));
            }
            init.compareAndSet(false, true);
        }
        int pos = random.nextInt(ARRAY_SIZE);

        for (int i = 0; i < CHECKED_ELEMENTS; i++) {
            if (i + pos == ARRAY_SIZE) {
                pos -= ARRAY_SIZE;
            }
            AtomicRef<Element> curRef = array.get(pos + i);
            Element curElement = curRef.getValue();
            Element newElement = new Element(x);
            Element newElementDone = new Element(true, x);
            if (curElement == null && curRef.compareAndSet(null, newElement)) {
                for (int j = 0; j < SPIN_WAIT_COUNT; j++) {
                    if (curRef.compareAndSet(newElementDone, null)) {
                        return;
                    }
                }
                if (curRef.compareAndSet(newElement, null)) {
                    basicPush(x);
                } else {
                    curRef.setValue(null);
                }
                return;
            }
        }

        basicPush(x);
    }

    public void basicPush(int x) {
        while (true) {
            Node curHead = head.getValue();
            Node newHead = new Node(x, curHead);
            if (head.compareAndSet(curHead, newHead)) {
                return;
            }
        }
    }

    @Override
    public int pop() {
        if (!init.getValue()) {
            for (int i = 0 ; i < ARRAY_SIZE; i++) {
                array.add(new AtomicRef<Element>(null));
            }
            init.compareAndSet(false, true);
        }
        int pos = random.nextInt(ARRAY_SIZE);

        for (int i = 0; i < CHECKED_ELEMENTS; i++) {
            if (i + pos == ARRAY_SIZE) {
                pos -= ARRAY_SIZE;
            }
            AtomicRef<Element> curRef = array.get(pos + i);
            Element curElement = curRef.getValue();
            if (curElement != null && !curElement.isDone()) {
                int x = curElement.getVal();
                Element newElement = new Element(true, x);
                if (curRef.compareAndSet(curElement, newElement)) {
                    return x;
                }
            }
        }

        return basicPop();
    }

    public int basicPop() {
        while (true) {
            Node curHead = head.getValue();
            if (curHead == null) return Integer.MIN_VALUE;
            if (head.compareAndSet(curHead, curHead.next.getValue())) {
                return curHead.x;
            }
        }
    }
}

