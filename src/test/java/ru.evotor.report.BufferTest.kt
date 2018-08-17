import org.junit.Test
import ru.evotor.report.buffer.BufferEmptyException
import ru.evotor.report.buffer.WrapBuffer

class WrapBufferTest {
    @Test(expected = UnsupportedOperationException::class)
    fun wrapBufferPutElementTest() {
        val array = Array(10) { it }
        val buffer = WrapBuffer(array)
        buffer.put(10)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun wrapBufferPutArrayTest() {
        val array = Array(10) { it }
        val buffer = WrapBuffer(array)
        buffer.put(Array(10) { it })
    }

    @Test
    fun wrapBufferGetTest() {
        val array = Array(10) { it }
        val buffer = WrapBuffer(array)
        assert(buffer.size() == 10)
        var i = 0
        while (buffer.hasElements()) {
            assert(buffer.get() == array[i++])
        }
        assert(buffer.size() == 0)
    }

    @Test(expected = BufferEmptyException::class)
    fun wrapBufferWrongGetTest() {
        val array = Array(10) { it }
        val buffer = WrapBuffer(array)
        while (buffer.hasElements()) {
            buffer.get()
        }
        buffer.get()
    }
}