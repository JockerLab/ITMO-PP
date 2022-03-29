/**
 * @author :TODO: Shaldin Vsevolod
 */
public class Solution implements AtomicCounter {
    final Node root = new Node(0, new Consensus<>());
    final ThreadLocal<Node> last = ThreadLocal.withInitial(() -> root);

    public int getAndAdd(int x) {
        int old = 0;
        Node newNode;
        do {
            old = last.get().val;
            newNode = new Node(old + x, new Consensus<>());
            last.set(last.get().next.decide(newNode));
        } while (last.get() != newNode);
        return old;
    }

    private static class Node {
        final int val;
        final Consensus<Node> next;

        Node(int val, Consensus<Node> next) {
            this.val = val;
            this.next = next;
        }
    }
}
