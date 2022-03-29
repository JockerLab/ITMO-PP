import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.atomicArrayOfNulls
import kotlinx.atomicfu.locks.ReentrantLock
import java.util.*
import kotlin.random.Random

class FCPriorityQueue<E : Comparable<E>> {
    private val ARRAY_SIZE = 8
    private val STEPS_COUNT = 100
    private val q = PriorityQueue<E>()
    private val ADD = Any()
    private val DONE = Any()
    private val PEEK = Any()
    private val REMOVE = Any()
    private val lock = atomic<Boolean>(false)
    private val array = atomicArrayOfNulls<Element<E>>(ARRAY_SIZE)

    private class Element<E>(val operation: Any, val value: E?) {}

    private fun makeOperations() {
        for (i in 0 until ARRAY_SIZE) {
            if (array[i].compareAndSet(null, null)) {
                continue
            }
            if (array[i].value?.operation == ADD) {
                q.add(array[i].value?.value)
                array[i].value = Element(DONE, null)
                continue
            }
            if (array[i].value?.operation == PEEK) {
                array[i].value = Element(DONE, q.peek())
                continue
            }
            if (array[i].value?.operation == REMOVE) {
                array[i].value = Element(DONE, q.poll())
                continue
            }
        }
    }

    private fun makeOp(func: String, element: E?): E? {
        var result: E? = null

        if (lock.compareAndSet(false, true)) {
            result = if (func == "poll") {
                q.poll()
            } else {
                if (func == "peek") {
                    q.peek()
                } else {
                    q.add(element)
                    null
                }
            }
            makeOperations()
            lock.compareAndSet(true, false)
        } else {
            var pos: Int = Random.nextInt(ARRAY_SIZE)
            var steps = 0
            var curPos: Int = -1
            val state: Element<E> = if (func == "poll") {
                Element(REMOVE, null)
            } else {
                if (func == "peek") {
                    Element(PEEK, null)
                } else {
                    Element(ADD, element)
                }
            }
            while (true) {
                pos %= ARRAY_SIZE
                if (array[pos].compareAndSet(null, state)) {
                    curPos = pos
                    break
                }

                steps++
                pos++
            }
            if (curPos == -1 || array[curPos].value?.operation != DONE) {
                while (true) {
                    if (lock.compareAndSet(false, true)) {
                        if (curPos != -1 && array[curPos].value?.operation == DONE) {
                            result = array[curPos].value?.value
                            array[curPos].compareAndSet(array[curPos].value, null)
                        } else {
                            array[curPos].compareAndSet(array[curPos].value, null)
                            result = if (func == "poll") {
                                q.poll()
                            } else {
                                if (func == "peek") {
                                    q.peek()
                                } else {
                                    q.add(element)
                                    null
                                }
                            }
                            makeOperations()
                        }
                        lock.compareAndSet(true, false)
                        break
                    } else {
                        if (curPos != -1 && array[curPos].value?.operation == DONE) {
                            result = array[curPos].value?.value
                            array[curPos].compareAndSet(array[curPos].value, null)
                            break
                        }
                    }
                }
            } else {
                result = array[curPos].value?.value
                array[curPos].compareAndSet(array[curPos].value, null)
            }
        }

        return result
    }

    /**
     * Retrieves the element with the highest priority
     * and returns it as the result of this function;
     * returns `null` if the queue is empty.
     */
    fun poll(): E? {
        return makeOp("poll", null)
    }

    /**
     * Adds the specified element to the queue.
     */
    fun add(element: E) {
        makeOp("add", element)
    }

    /**
     * Returns the element with the highest priority
     * or `null` if the queue is empty.
     */
    fun peek(): E? {
        return makeOp("peek", null)
    }
}