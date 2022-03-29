import kotlinx.atomicfu.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class BlockingStackImpl<E> : BlockingStack<E> {

    // ==========================
    // Segment Queue Synchronizer
    // ==========================

    private class FAAQueue<T> {
        private val head: AtomicRef<Segment>
        private val tail: AtomicRef<Segment>

        init {
            val firstNode = Segment()
            head = atomic(firstNode)
            tail = atomic(firstNode)
        }

        fun enqueue(x: T) {
            while (true) {
                val curTail = tail.value
                val enqIdx = curTail.enqIdx.getAndIncrement()
                if (enqIdx >= SEGMENT_SIZE) {
                    val newTail = Segment(x)
                    val nextTail = curTail.next.value
                    if (nextTail == null) {
                        if (curTail.next.compareAndSet(null, newTail)) {
                            tail.compareAndSet(curTail, newTail)
                            return
                        }
                    } else {
                        tail.compareAndSet(curTail, nextTail)
                    }
                } else {
                    if (curTail.elements[enqIdx.toInt()].compareAndSet(null, x)) {
                        return
                    }
                }
            }
        }

        fun dequeue(): T? {
            while (true) {
                val curHead = head.value
                val deqIdx = curHead.deqIdx.getAndIncrement()
                if (deqIdx >= SEGMENT_SIZE) {
                    val headNext = curHead.next.value ?: return null
                    head.compareAndSet(curHead, headNext)
                    continue
                }
                val res = curHead.elements[deqIdx.toInt()].getAndSet(DONE) ?: continue
                return res as T?
            }
        }
    }

    private class Segment {
        val next: AtomicRef<Segment?> = atomic(null)
        val enqIdx = atomic(0L)
        val deqIdx = atomic(0L)
        val elements = atomicArrayOfNulls<Any>(SEGMENT_SIZE)

        constructor()

        constructor(x: Any?) {
            enqIdx.getAndIncrement()
            elements[0].compareAndSet(null, x)
        }

        val isEmpty: Boolean get() {
            val deqId = deqIdx.value
            val enqId = enqIdx.value

            return deqId >= enqId || deqId >= SEGMENT_SIZE
        }
    }

    private val queue =  FAAQueue<Continuation<E>>()

    private suspend fun suspend(): E {
        return suspendCoroutine { cont ->
            queue.enqueue(cont)
        }
    }

    private fun resume(element: E) {
        while (true) {
            val cur = queue.dequeue() ?: continue
            cur.resume(element)
            break
        }
        //queue.dequeue()!!.resume(element)
    }

    // ==============
    // Blocking Stack
    // ==============


    private val head = atomic<Node<E>?>(null)
    private val elements = atomic(0)
    private val RETRY = Any()

    override fun push(element: E) {
        val elements = this.elements.getAndIncrement()
        if (elements >= 0) {
            // push the element to the top of the stack
            while (true) {
                val curHead = head.value
                if (curHead != null && curHead.element == SUSPENDED) {
                    val nextHead = curHead.next.get()
                    if (head.compareAndSet(curHead, nextHead)) {
                        curHead.cont!!.resume(element as Any)
                        break
                    }
                }
                val newHead = Node<E>(element, AtomicReference<Node<E>?>(curHead), null)
                if (head.compareAndSet(curHead, newHead)) {
                    break
                }
            }
        } else {
            // resume the next waiting receiver
            resume(element)
        }
    }

    override suspend fun pop(): E {
        val elements = this.elements.getAndDecrement()
        if (elements > 0) {
            // remove the top element from the stack
            while (true) {
                val curHead = head.value
                if (curHead == null || curHead.element == SUSPENDED) {
                    val res = suspendCoroutine<Any> sc@{ cont ->
                        val newHead = Node<E>(SUSPENDED, AtomicReference<Node<E>?>(curHead), cont)
                        if (!head.compareAndSet(curHead, newHead)) {
                            cont.resume(RETRY)
                            return@sc
                        }
                    }

                    if (res != RETRY) return res as E
                    continue
                }
                if (head.compareAndSet(curHead, curHead.next.get())) {
                    return curHead.element as E
                }
            }
        } else {
            return suspend()
        }
    }
}

private class Node<E>(val element: Any?, val next: AtomicReference<Node<E>?>, val cont: Continuation<Any>?)
const val SEGMENT_SIZE = 2
private val DONE = Any()
private val SUSPENDED = Any() //