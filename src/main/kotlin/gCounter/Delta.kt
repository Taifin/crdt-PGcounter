package gCounter

import Id
import java.lang.IllegalStateException

class Delta(private val id: Id, private val count: Int) : GCounter {
    override fun inc(): Delta {
        throw IllegalStateException("Should not call any operations on delta!")
    }

    override fun get(): Long {
        throw IllegalStateException("Should not call any operations on delta!")
    }

    override fun merge(other: GCounter) {
        throw IllegalStateException("Should not call any operations on delta!")
    }

    override fun getCounters(): Map<Id, Int> {
        return mapOf(id to count)
    }

}