import kotlinx.atomicfu.*
import java.lang.IndexOutOfBoundsException
import java.lang.NullPointerException

class DynamicArrayImpl<E> : DynamicArray<E> {
    private val core = atomic(Core<E?>(INITIAL_CAPACITY))
    private val curSize = atomic(0)

    override fun get(index: Int): E {
        if (index >= size) {
            throw IllegalArgumentException()
        }
        return core.value.getById(index)!!.getV()!!
    }

    override fun put(index: Int, element: E) {
        if (index >= size) {
            throw IllegalArgumentException()
        }
        while (true) {
            // val curCore = core.value
            val curObj = core.value.getById(index)!!
            if (!curObj.isMoved() && core.value.cas(index, curObj, Wrapper<E?>(element, false))) {
                break
            }
        }
    }

    override fun pushBack(element: E) {
        while (true) {
            val stateSize = size
            if (stateSize >= core.value.getCapacity()) {
                expand()
            }
            if (core.value.cas(stateSize, null, Wrapper<E?>(element, false))) {
                curSize.incrementAndGet()
                break
            }
        }
    }

    override val size: Int
        get() {
            return curSize.value
        }

    fun expand() {
        val curCore = core.value
        val capacity = curCore.getCapacity()
        if (size < capacity) {
            return
        }
        val newCore = Core<E?>(capacity * 2)

        for (i in 0 until capacity) {
            if (curCore.getById(i) == null) {
                throw RuntimeException()
            }
            while (true) {
                val curObj = curCore.getById(i)!!
                val curV = curObj.getV()
                if (curCore.cas(i, curObj, Wrapper<E?>(curV, true))) {
                    newCore.setById(i, Wrapper<E?>(curV, false))
                    break
                }
            }
        }

        core.compareAndSet(curCore, newCore)
    }
}


private class Core<E>(
        private val capacity: Int,
) {
    private val array = atomicArrayOfNulls<Wrapper<E>?>(capacity)

    fun getCapacity(): Int {
        return capacity
    }

    fun getArray(): AtomicArray<Wrapper<E>?> {
        return array
    }

    fun cas(id: Int, from: Wrapper<E>?, elem: Wrapper<E>?): Boolean {
        return id < capacity && array[id].compareAndSet(from, elem)
    }

    fun setById(id: Int, elem: Wrapper<E>) {
        array[id].getAndSet(elem)
    }

    fun getById(id: Int): Wrapper<E>? {
        return array[id].value
    }
}

private class Wrapper<E>(
        private var v: E?,
        private var isMoved: Boolean
) {
    fun getV(): E? {
        return v
    }

    fun isMoved(): Boolean {
        return isMoved
    }
}

private const val INITIAL_CAPACITY = 1 // DO NOT CHANGE ME