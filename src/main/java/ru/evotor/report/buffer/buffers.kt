package ru.evotor.report.buffer

interface Buffer<T> {
    fun put(t: T)
    fun put(arr: Array<T>)
    fun get(): T
    fun get(length: Int): Array<T>
    fun hasElements(): Boolean
    fun size(): Int
}

class CyclicBuffer<T>(initialSize: Int = 16, private val maxSize: Int = Int.MAX_VALUE) : Buffer<T> {
    private object EMPTY

    private var array = Array<Any>(initialSize) { EMPTY }
    private var writeIndex = 0
    private var readIndex = 0
    private var direct = true

    override fun put(t: T) {
        ensureCapacity(1)
        array[writeIndex] = t as Any
        if (writeIndex == array.size) {
            writeIndex = 0
            direct = false
        }
    }

    override fun put(arr: Array<T>) = throw UnsupportedOperationException()

    @Suppress("UNCHECKED_CAST")
    override fun get(): T {
        checkSize()
        val ret = array[readIndex]
        if (readIndex == array.size) {
            readIndex = 0
            direct = true
        }
        return ret as T
    }

    override fun get(length: Int) = throw UnsupportedOperationException()

    override fun hasElements() = size() > 0

    override fun size() = if (direct) (writeIndex - readIndex) else (array.size - readIndex + writeIndex)

    private fun checkSize() {
        if (!hasElements()) throw BufferEmptyException()
    }

    private fun ensureCapacity(add: Int) {
        if (array.size - size() >= add) return
        val newSize = Math.min(maxSize, Math.max(array.size * 2, array.size + add))
        if (newSize == array.size) throw BufferFullException()
        val newArr = Array<Any>(newSize) { EMPTY }
        if (direct) {
            System.arraycopy(array, readIndex, newArr, 0, writeIndex - readIndex)
            array = newArr
            writeIndex -= readIndex
            readIndex = 0
        } else {
            System.arraycopy(array, readIndex, newArr, 0, array.size - readIndex)
            System.arraycopy(array, 0, newArr, array.size - readIndex, writeIndex)
            array = newArr
            writeIndex += array.size - readIndex
            readIndex = 0
        }
    }
}

class WrapBuffer<T>(private val arr: Array<T>, start: Int = 0, private val length: Int = arr.size) : Buffer<T> {
    private var readIndex = start

    override fun put(t: T) = throw UnsupportedOperationException()

    override fun put(arr: Array<T>) = throw UnsupportedOperationException()

    override fun get(): T {
        if (!hasElements()) throw BufferEmptyException()
        return arr[readIndex++]
    }

    override fun get(length: Int) = throw UnsupportedOperationException()

    override fun hasElements() = readIndex < length

    override fun size() = length - readIndex
}

open class BufferException : Exception()
class BufferEmptyException : BufferException()
class BufferFullException : BufferException()
