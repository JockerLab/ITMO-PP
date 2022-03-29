import java.util.concurrent.atomic.*

class Solution(val env: Environment) : Lock<Solution.Node> {
    val tail = AtomicReference<Node>()

    override fun lock(): Node {
        val my = Node() // сделали узел
        val prev = tail.getAndSet(my)
        if (prev != null) {
            prev.next.value = my
            while (my.locked.value) env.park()
        }
        return my // вернули узел
    }

    override fun unlock(node: Node) {
        if (node.next.value == null) {
            if (tail.compareAndSet(node, null)) {
                return
            } else {
                while (node.next.value == null) {
                    continue
                }
            }
        }
        node.next.value!!.locked.value = false
        env.unpark(node.next.value!!.thread)
    }

    class Node {
        val thread = Thread.currentThread() // запоминаем поток, которые создал узел
        val locked = AtomicReference<Boolean>(true)
        val next: AtomicReference<Node?> = AtomicReference(null)
    }
}