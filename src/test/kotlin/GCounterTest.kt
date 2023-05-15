import gCounter.Delta
import gCounter.GCounter
import gCounter.GCounterImpl
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.util.*
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.LinkedBlockingQueue
import kotlin.test.assertEquals

private class NetworkReplicaWrapG(private val counter: GCounter) {
    private val queue = LinkedBlockingQueue<Delta>()

    fun add(delta: Delta) {
        queue.put(delta)
    }

    fun mergeAll() {
        while (queue.isNotEmpty()) {
            counter.merge(queue.take())
        }
    }

    fun get(): Long {
        return counter.get()
    }

    fun inc(): Delta {
        return counter.inc()
    }
}

class GCounterTest {
    private val manyCountersAmount = 10
    private val singleOperationAmount = 1000L

    private fun singleCounter() = GCounterImpl(Id("0"))
    private fun manyCounters() =
        Collections.synchronizedList(MutableList(manyCountersAmount) { ind ->
            NetworkReplicaWrapG(
                GCounterImpl(Id(ind.toString()))
            )
        })

    @Test
    fun singleIncrement() {
        val counter = singleCounter()
        counter.inc()
        assertEquals(1, counter.get())
    }

    @Test
    fun multipleIncrementsFromSingleReplica() {
        val counter = singleCounter()
        counter.inc()
        counter.inc()
        counter.inc()
        assertEquals(3, counter.get())
    }

    @Test
    fun cannotIncrementOverIntMax() {
        val counter = singleCounter()
        for (i in 0 until Int.MAX_VALUE) {
            counter.inc()
        }
        assertThrows<IllegalStateException> { counter.inc() }
    }

    @Test
    fun noOperationsOnDeltaExceptGetCounters() {
        val delta = Delta(Id("0"), 0)
        assertThrows<IllegalStateException> { delta.inc() }
        assertThrows<IllegalStateException> { delta.get() }
        assertThrows<IllegalStateException> { delta.merge(singleCounter()) }
        assertDoesNotThrow { delta.getCounters() }
    }

    @Test
    fun manyIncrementsFromManyReplicasSync() {
        val counters = Array(manyCountersAmount) { ind -> GCounterImpl(Id(ind.toString())) }
        for (counter in counters) {
            counter.inc()
            counter.inc()
        }
        for (c1 in counters) {
            for (c2 in counters) {
                c1.merge(c2)
                c2.merge(c1)
            }
        }
        for (counter in counters) {
            assertEquals(2L * manyCountersAmount, counter.get())
        }
    }

    @RepeatedTest(1000)
    fun manyIncrementsFromManyReplicasAsync() {
        val counters = manyCounters()
        val threads = mutableListOf<Thread>()
        for (i in 0 until manyCountersAmount) {
            threads.add(Thread {
                for (j in 0 until singleOperationAmount) {
                    val delta = counters[i].inc()

                    for (k in 0 until manyCountersAmount) {
                        counters[k].add(delta)
                    }
                }
                counters[i].mergeAll()
            })
            threads[i].start()
        }

        for (thread in threads) {
            thread.join()
        }

        for (counter in counters) {
            counter.mergeAll()
            assertEquals(singleOperationAmount * manyCountersAmount, counter.get())
        }
    }

    @RepeatedTest(500)
    fun manyReplicasManyGetsAsync() {
        val counters = manyCounters()
        val threads = mutableListOf<Thread>()
        var runs = 0
        val barrier = CyclicBarrier(manyCountersAmount) {
            runs++
            for (counter in counters) {
                counter.mergeAll()
                assertEquals(singleOperationAmount * runs * manyCountersAmount, counter.get())
            }
        }
        for (i in 0 until manyCountersAmount) {
            threads.add(Thread {
                for (p in 0 until 5) {
                    for (j in 0 until singleOperationAmount) {
                        val delta = counters[i].inc()

                        for (k in 0 until manyCountersAmount) {
                            counters[k].add(delta)
                        }
                    }
                    counters[i].mergeAll()
                    barrier.await()
                }
            })
            threads[i].start()
        }

        for (thread in threads) {
            thread.join()
        }

        for (counter in counters) {
            counter.mergeAll()
            assertEquals(singleOperationAmount * runs * manyCountersAmount, counter.get())
        }
    }
}
