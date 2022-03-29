package dijkstra

import kotlinx.atomicfu.AtomicInt
import kotlinx.atomicfu.atomic
import java.util.*
import java.util.concurrent.Phaser
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.Comparator
import kotlin.collections.ArrayList
import kotlin.concurrent.thread
import kotlin.random.Random

private val NODE_DISTANCE_COMPARATOR = Comparator<Node> { o1, o2 -> Integer.compare(o1!!.distance, o2!!.distance) }

// Returns `Integer.MAX_VALUE` if a path has not been found.
fun shortestPathParallel(start: Node) {
    val workers = Runtime.getRuntime().availableProcessors()
    start.distance = 0
    if (workers == 0) {
        return
    }
    val queuesCount = 2 * workers
    val q: Array<PriorityQueue<Node>> = Array(queuesCount, { i -> PriorityQueue(NODE_DISTANCE_COMPARATOR) })
    q[0].add(start)
    var nodeCount: AtomicInt = atomic(1)
    val onFinish = Phaser(workers + 1)
    repeat(workers) {
        thread {
            while (nodeCount.value > 0) {
                var firstQueue = Random.nextInt(queuesCount)
                var secondQueue = Random.nextInt(queuesCount)
                while (secondQueue == firstQueue) {
                    secondQueue = Random.nextInt(queuesCount)
                }
                if (firstQueue > secondQueue) {
                    firstQueue = secondQueue.also { secondQueue = firstQueue }
                }
                var curNode: Node = Node()
                var flag = false
                synchronized(q[firstQueue]) {
                    synchronized(q[secondQueue]) {
                        if (q[firstQueue].size > 0 && q[secondQueue].size > 0) {
                            curNode = if (q[firstQueue].peek().distance > q[secondQueue].peek().distance) {
                                q[secondQueue].poll()
                            } else {
                                q[firstQueue].poll()
                            }
                        } else {
                            if (q[firstQueue].size > 0) {
                                curNode = q[firstQueue].poll()
                            } else {
                                if (q[secondQueue].size > 0) {
                                    curNode = q[secondQueue].poll()
                                } else {
                                    flag = true
                                }
                            }
                        }
                    }
                }
                if (flag) {
                    continue
                }
                for (v in curNode.outgoingEdges) {
                    while(true) {
                        var len = curNode.distance + v.weight
                        var curDist = v.to.distance
                        if (len < curDist) {
                            if (v.to.casDistance(curDist, len)) {
                                val queue = Random.nextInt(queuesCount)
                                synchronized(q[queue]) {
                                    q[queue].add(v.to)
                                }

                                nodeCount.incrementAndGet()
                                break
                            }
                        } else {
                            break
                        }
                    }
                }
                nodeCount.decrementAndGet()
            }
            onFinish.arrive()
        }
    }
    onFinish.arriveAndAwaitAdvance()
}