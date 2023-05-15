import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import pnCounter.Delta
import pnCounter.PNCounter
import pnCounter.PNCounterImpl
import java.util.*
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicLong
import kotlin.test.assertEquals

private class NetworkReplicaWrapPN(private val counter: PNCounter) {
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

    fun dec(): Delta {
        return counter.dec()
    }
}

class PNCounterTest {
    private val manyCountersAmount = 10
    private val singleOperationAmount = 1000L

    private fun singleCounter() = PNCounterImpl(Id("0"))
    private fun manyCounters() =
        Collections.synchronizedList(MutableList(manyCountersAmount) { ind ->
            NetworkReplicaWrapPN(
                PNCounterImpl(Id(ind.toString()))
            )
        })

    @Test
    fun incrementsFromSingleReplica() {
        val counter = singleCounter()
        for (i in 0 until singleOperationAmount) {
            counter.inc()
        }
        assertEquals(singleOperationAmount, counter.get())
    }

    @Test
    fun decrementsFromSingleReplica() {
        val counter = singleCounter()
        for (i in 0 until singleOperationAmount) {
            counter.dec()
        }
        assertEquals(-singleOperationAmount, counter.get())
    }

    @Test
    fun singleIncrementDecrementZeros() {
        val counter = singleCounter()
        for (i in 0 until singleOperationAmount) {
            counter.inc()
            counter.dec()
        }
        assertEquals(0, counter.get())
    }

    @Test
    fun manyOperationsFromManyReplicasSync() {
        val counters = Array(manyCountersAmount) { ind -> PNCounterImpl(Id(ind.toString())) }
        var globalCount = 0L
        for (counter in counters) {
            for (i in 0 until singleOperationAmount) {
                val delta = if (Math.random().toInt() % 2 == 0) {
                    globalCount++
                    counter.inc()
                } else {
                    globalCount--
                    counter.dec()
                }
                for (other in counters) {
                    other.merge(delta)
                }
            }
        }

        for (counter in counters) {
            assertEquals(globalCount, counter.get())
        }
    }

    @RepeatedTest(1000)
    fun manyOperationsFromManyReplicasAsync() {
        val counters = manyCounters()
        val threads = mutableListOf<Thread>()
        val globalCount = AtomicLong(0)
        for (i in 0 until manyCountersAmount) {
            threads.add(Thread {
                for (k in 0 until singleOperationAmount) {
                    val delta = if (Math.random() > 0.5) {
                        globalCount.incrementAndGet()
                        counters[i].inc()
                    } else {
                        globalCount.decrementAndGet()
                        counters[i].dec()
                    }
                    for (other in counters) {
                        other.add(delta)
                    }
                }
            })
            threads[i].start()
        }

        for (thread in threads) {
            thread.join()
        }

        for (counter in counters) {
            counter.mergeAll()
            assertEquals(globalCount.get(), counter.get())
        }
    }

    @RepeatedTest(500)
    fun manyReplicasManyGetsAsync() {
        val counters = manyCounters()
        val threads = mutableListOf<Thread>()
        val globalCount = AtomicLong(0)
        val barrier = CyclicBarrier(manyCountersAmount) {
            for (counter in counters) {
                counter.mergeAll()
                assertEquals(globalCount.get(), counter.get())
            }
        }
        for (i in 0 until manyCountersAmount) {
            threads.add(Thread {
                for (p in 0 until 5) {
                    for (j in 0 until singleOperationAmount) {
                        val delta = if (Math.random() > 0.5) {
                            globalCount.incrementAndGet()
                            counters[i].inc()
                        } else {
                            globalCount.decrementAndGet()
                            counters[i].dec()
                        }
                        for (other in counters) {
                            other.add(delta)
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
            assertEquals(globalCount.get(), counter.get())
        }
    }
}