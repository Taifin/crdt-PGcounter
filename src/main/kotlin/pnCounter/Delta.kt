package pnCounter

import gCounter.GCounter
import java.lang.IllegalStateException

class Delta(private val inc: gCounter.Delta, private val dec: gCounter.Delta) : PNCounter {
    override fun inc(): Delta {
        throw IllegalStateException("Should not call any operations on delta!")
    }

    override fun dec(): Delta {
        throw IllegalStateException("Should not call any operations on delta!")
    }

    override fun get(): Long {
        throw IllegalStateException("Should not call any operations on delta!")
    }

    override fun merge(other: PNCounter) {
        throw IllegalStateException("Should not call any operations on delta!")
    }

    override fun getCounters(): Pair<GCounter, GCounter> {
        return Pair(inc, dec)
    }
}