package linked_list_set;

import kotlinx.atomicfu.AtomicRef;

public class SetImpl implements Set {
    private abstract class NodeOrRemoved {
        AtomicRef<NodeOrRemoved> next;
        int x;

        NodeOrRemoved(int x, AtomicRef<NodeOrRemoved> next) {
            this.x = x;
            this.next = next;
        }
    }

    private class Removed extends NodeOrRemoved {
        Removed(int x, AtomicRef<NodeOrRemoved> next) {
            super(x, next);
        }
    }

    private class Node extends NodeOrRemoved {
        Node(int x, AtomicRef<NodeOrRemoved> next) {
            super(x, next);
        }
    }

    private class Window {
        NodeOrRemoved cur, next;

        Window(NodeOrRemoved cur, NodeOrRemoved next) {
            this.cur = cur;
            this.next = next;
        }
    }

    private final Node head = new Node(Integer.MIN_VALUE, new AtomicRef<NodeOrRemoved>(new Node(Integer.MAX_VALUE, new AtomicRef<NodeOrRemoved>(null))));

    /**
     * Returns the {@link Window}, where cur.x < x <= next.x
     */
    private Window findWindow(int x) {
        while (true) {
            NodeOrRemoved cur = head;
            NodeOrRemoved next = cur.next.getValue();
            boolean flag = false;
            while (next.x < x) {
                NodeOrRemoved node = next.next.getValue();
                if (node instanceof Removed) {
                    Node newNode = new Node(node.x, node.next);
                    if (!cur.next.compareAndSet(next, newNode)) {
                        flag = true;
                        break;
                    }
                    next = node;
                } else {
                    cur = next;
                    next = cur.next.getValue();
                }
            }
            if (flag) {
                continue;
            }
            NodeOrRemoved curCur = cur.next.getValue(), curNext = next.next.getValue();
            if (curCur instanceof Removed) {
                continue;
            }
            if (curNext instanceof Removed) {
                Node newNode = new Node(curNext.x, curNext.next);
                cur.next.compareAndSet(next, newNode);
            }
            return new Window(cur, next);
        }
    }

    @Override
    public boolean add(int x) {
        while (true) {
            Window w = findWindow(x);
            if (w.next.next.getValue() instanceof Removed ||
                    w.cur.next.getValue() instanceof Removed) {
                continue;
            }
            if (w.next.x == x) {
                return false;
            }
            Node node = new Node(x, new AtomicRef<NodeOrRemoved>(w.next));
            if (!(w.cur.next.getValue() instanceof Removed) &&
                    !(w.next.next.getValue() instanceof Removed) &&
                    w.cur.next.compareAndSet(w.next, node)) {
                return true;
            }
        }
    }

    @Override
    public boolean remove(int x) {
        while (true) {
            Window w = findWindow(x);
            if (w.next.next.getValue() instanceof Removed ||
                    w.cur.next.getValue() instanceof Removed) {
                continue;
            }
            if (w.next.x != x) {
                return false;
            }
            NodeOrRemoved node = w.next.next.getValue();
            Removed removedNode = new Removed(node.x, node.next);
            if (!(w.cur.next.getValue() instanceof Removed) &&
                    !(w.next.next.getValue() instanceof Removed) &&
                    w.next.next.compareAndSet(node, removedNode)) {
                Node newNode = new Node(node.x, node.next);
                w.cur.next.compareAndSet(w.next, newNode);
                return true;
            }
        }
    }

    @Override
    public boolean contains(int x) {
        Window w = findWindow(x);
        return !(w.next.next.getValue() instanceof Removed) &&
                w.next.x == x;
    }
}