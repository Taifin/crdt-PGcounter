package pnCounter

import gCounter.Delta
import gCounter.GCounter
import gCounter.GCounterImpl
import Id

class PNCounterImpl(private val id: Id) : PNCounter {
    private val increment = GCounterImpl(id)
    private val decrement = GCounterImpl(id)

    override fun inc(): pnCounter.Delta {
        return Delta(increment.inc(), Delta(id, 0))
    }

    override fun dec(): pnCounter.Delta {
        return Delta(Delta(id, 0), decrement.inc())
    }

    override fun get(): Long {
        return increment.get() - decrement.get()
    }

    override fun merge(other: PNCounter) {
        val (oInc, oDec) = other.getCounters()
        increment.merge(oInc)
        decrement.merge(oDec)
    }

    override fun getCounters(): Pair<GCounter, GCounter> {
        return Pair(increment, decrement)
    }
}