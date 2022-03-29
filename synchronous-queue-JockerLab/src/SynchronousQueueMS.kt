import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import java.util.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SynchronousQueueMS<E> : SynchronousQueue<E> {
    private val RETRY = Any()
    private val dummy = Node<E>()
    private val head = atomic(dummy)
    private val tail = atomic(dummy)

    override suspend fun send(element: E) {
        while (true) {
            val curHead = head.value
            val curTail = tail.value
            if (curHead == curTail || curTail.v != null) {
                val res = suspendCoroutine<Any> sc@{ cont ->
                    val newTail = Node<E>(element, cont)
                    if (curTail.next.compareAndSet(null, newTail)) {
                        tail.compareAndSet(curTail, newTail)
                    } else {
                        tail.compareAndSet(curTail, curTail.next.value!!)
                        cont.resume(RETRY)
                        return@sc
                    }
                }
                if (res != RETRY) break
            } else {
                val nextHead = curHead.next.value
                if (curHead == curTail) {
                    if (nextHead != null) {
                        tail.compareAndSet(curTail, curTail.next.value!!)
                    }
                } else {
                    if (nextHead == null || nextHead.state == null) {
                        continue
                    }
                    val res = nextHead.state!!
                    if (head.compareAndSet(curHead, nextHead)) {
                        res.resume(element!!)
                        break
                    }
                }
            }
        }
    }

    override suspend fun receive(): E {
        while (true) {
            val curHead = head.value
            val curTail = tail.value
            if (curHead == curTail || curTail.v == null) {
                val res = suspendCoroutine<Any> sc@{ cont ->
                    val newTail = Node<E>(null, cont)
                    if (curTail.next.compareAndSet(null, newTail)) {
                        tail.compareAndSet(curTail, newTail)
                    } else {
                        tail.compareAndSet(curTail, curTail.next.value!!)
                        cont.resume(RETRY)
                        return@sc
                    }
                }
                if (res != RETRY) return res as E
            } else {
                val nextHead = curHead.next.value
                if (curHead == curTail) {
                    if (nextHead != null) {
                        tail.compareAndSet(curTail, curTail.next.value!!)
                    }
                } else {
                    if (nextHead == null || nextHead.state == null || nextHead.v == null) {
                        continue
                    }
                    val res = nextHead.state!!
                    val elem = nextHead.v!!
                    if (head.compareAndSet(curHead, nextHead)) {
                        res.resume(Unit)
                        return elem
                    }
                }
            }
        }
    }

    private class Node<E> {
        val next: AtomicRef<Node<E>?> = atomic(null)
        var state: Continuation<Any>? = null
        var v: E? = null

        constructor()

        constructor(v: E?, state: Continuation<Any>?) {
            this.v = v
            this.state = state
        }
    }
}
