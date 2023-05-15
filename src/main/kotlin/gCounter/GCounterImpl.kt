package gCounter

import Id
import java.lang.Integer.max

class GCounterImpl(private val id: Id) : GCounter {
    private val increment: MutableMap<Id, Int> = mutableMapOf()

    override fun inc(): Delta {
        val before = increment[id] ?: 0
        if (before == Int.MAX_VALUE) throw IllegalStateException("Maximum integer value is reached!")

        increment[id] = before + 1
        return Delta(id, increment[id]!!)
    }

    override fun get(): Long {
        return increment.values.sum().toLong()
    }

    override fun merge(other: GCounter) {
        for ((key, value) in other.getCounters()) {
            if (increment.containsKey(key)) {
                increment[key] = max(value, increment[key]!!)
            } else {
                increment[key] = value
            }
        }
    }

    override fun getCounters(): Map<Id, Int> {
        return increment
    }

}