package gCounter

import Id

interface GCounter {
    fun get(): Long
    fun inc(): Delta
    fun merge(other: GCounter)
    fun getCounters(): Map<Id, Int>
}