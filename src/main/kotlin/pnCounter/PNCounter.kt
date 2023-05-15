package pnCounter

import gCounter.GCounter

interface PNCounter {
    fun inc(): Delta
    fun dec(): Delta
    fun get(): Long
    fun merge(other: PNCounter)
    fun getCounters(): Pair<GCounter, GCounter>
}