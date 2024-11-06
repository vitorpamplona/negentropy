package com.vitorpamplona.negentropy.storage

import com.vitorpamplona.negentropy.ID_SIZE

class StorageVector: IStorage {
    private val items = mutableListOf<StorageUnit>()
    private var sealed = false

    override fun insert(timestamp: Long, idHex: String) = insert(timestamp, Id(idHex))

    override fun insert(timestamp: Long, id: Id) {
        check(!sealed) { throw Error("already sealed") }
        check(id.bytes.size == ID_SIZE) { throw Error("bad id size for added item") }

        items.add(StorageUnit(timestamp, id))
    }

    override fun seal() {
        check(!sealed) { throw Error("already sealed") }
        sealed = true

        items.sortWith(this::itemCompare)

        // checks if there are no duplicates
        for (i in 1 until items.size) {
            check(itemCompare(items[i - 1], items[i]) != 0) { throw Error("duplicate item inserted") }
        }
    }

    override fun unseal() {
        sealed = false
    }

    override fun size(): Int {
        checkSealed()
        return items.size
    }

    override fun getItem(i: Int): StorageUnit {
        checkSealed()
        check(i < items.size) { throw Error("out of range") }
        return items[i]
    }


    override fun <T> map(begin: Int, end: Int, run: (StorageUnit) -> T): List<T> {
        checkSealed()
        checkBounds(begin, end)

        val list = mutableListOf<T>()

        for (i in begin until end) {
            list.add(run(items[i]))
        }

        return list
    }

    override fun forEach(begin: Int, end: Int, run: (StorageUnit) -> Unit) {
        checkSealed()
        checkBounds(begin, end)

        for (i in begin until end) {
            run(items[i])
        }
    }

    override fun findTimestamp(id: Id): Long {
        for (i in items.indices) {
            if (items[i].id == id) {
                return items[i].timestamp
            }
        }
        return -1
    }

    override fun iterate(begin: Int, end: Int, shouldContinue: (StorageUnit, Int) -> Boolean) {
        checkSealed()
        checkBounds(begin, end)

        for (i in begin until end) {
            if (!shouldContinue(items[i], i)) break
        }
    }

    override fun findLowerBound(begin: Int, end: Int, bound: StorageUnit): Int {
        checkSealed()
        checkBounds(begin, end)

        return binarySearch(items, begin, end) { itemCompare(it, bound) < 0 }
    }

    fun itemCompare(a: StorageUnit, b: StorageUnit): Int {
        return if (a.timestamp == b.timestamp) {
            a.id.compareTo(b.id)
        } else {
            a.timestamp.compareTo(b.timestamp)
        }
    }

    private fun checkSealed() = check(sealed) { throw Error("not sealed") }

    private fun checkBounds(begin: Int, end: Int) = check(begin <= end && end <= items.size) { throw Error("bad range") }

    private fun binarySearch(arr: List<StorageUnit>, first: Int, last: Int, cmp: (StorageUnit) -> Boolean): Int {
        var low = first
        var high = last

        while (low < high) {
            val mid = low + (high - low) / 2
            if (cmp(arr[mid])) {
                low = mid + 1
            } else {
                high = mid
            }
        }

        return low
    }
}