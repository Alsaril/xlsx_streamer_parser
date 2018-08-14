package ru.evotor.report

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.system.measureTimeMillis

interface Transformation<A, B> {
    fun transform(inp: Queue<A>): List<B>
}

abstract class Pipeline<A, B> {
    private var callback: ((B) -> Unit)? = null

    abstract fun consume(a: A)

    protected fun fire(b: B) {
        callback?.invoke(b)
    }

    private fun setCallback(callback: (B) -> Unit) {
        this.callback = callback
    }

    fun <C> then(next: Transformation<B, C>, maxBufferSize: Int): Pipeline<A, C> {
        val newPipeline = object : Pipeline<A, C>() {
            override fun consume(a: A) {
                this@Pipeline.consume(a)
            }
        }

        val queue: Queue<B> = ArrayBlockingQueue(maxBufferSize)
        setCallback {
            queue.offer(it)
            val result = next.transform(queue) // mb has result values
            result.forEach(newPipeline::fire)
        }
        return newPipeline
    }

    fun then(next: (B) -> Unit): Pipeline<A, B> {
        setCallback(next)
        return this
    }

    fun <C> map(transform: (B) -> C): Pipeline<A, C> {
        val newPipeline = object : Pipeline<A, C>() {
            override fun consume(a: A) {
                this@Pipeline.consume(a)
            }
        }

        setCallback {
            newPipeline.fire(transform(it))
        }
        return newPipeline
    }

    fun filter(predicate: (B) -> Boolean): Pipeline<A, B> {
        val newPipeline = object : Pipeline<A, B>() {
            override fun consume(a: A) {
                this@Pipeline.consume(a)
            }
        }

        setCallback {
            if (predicate(it)) {
                newPipeline.fire(it)
            }
        }
        return newPipeline
    }

    companion object {
        operator fun <T> invoke(): Pipeline<T, T> {
            return object : Pipeline<T, T>() {
                override fun consume(a: T) {
                    fire(a)
                }
            }
        }
    }
}

sealed class Token
class TextToken(val text: String) : Token() {
    override fun toString() = "TextToken($text)"
}

open class TagToken(val name: String) : Token() {
    override fun toString() = "TagToken($name)"
}

class OpenTagToken(name: String) : TagToken(name) {
    override fun toString() = "<$name>"
}

class CloseTagToken(name: String) : TagToken(name) {
    override fun toString() = "</$name>"
}

class SingleTagToken(name: String) : TagToken(name) {
    override fun toString() = "<$name/>"
}

class TokenCollector : Transformation<Char, Token> {
    private var tag = false
    private var open = false
    private var start = false
    private var single = false
    private var text = ""

    override fun transform(inp: Queue<Char>): List<Token> {
        val c = inp.poll()
        if (c == '<') {
            tag = true
            open = true
            start = true
            single = false
            val ret = text
            text = ""
            return if (ret.isEmpty()) emptyList() else listOf(TextToken(ret))
        } else if (c == '>') {
            tag = false
            val ret = text
            text = ""
            return listOf(if (single) SingleTagToken(ret.substring(0, ret.length - 1)) else if (open) OpenTagToken(ret) else CloseTagToken(ret.substring(1)))
        }

        if (start && c == '/') {
            open = false
        }

        if (c == '/' && !start) {
            single = true
        }

        if (start) {
            start = false
        }

        text += c
        return emptyList()
    }
}

fun processSheet(zin: ZipInputStream, name: String) {
    val map = mutableMapOf<Int, AtomicInteger>()
    val length = AtomicInteger()

    val pipeline = Pipeline<Char>()
            .then(TokenCollector(), 1000)
            .filter { it is TextToken }
            .map { (it as TextToken).text }
            .then {
                map.computeIfAbsent(it.length) { _ ->
                    AtomicInteger(0)
                }.incrementAndGet()
                length.addAndGet(it.length)
            }

    val z = InputStreamReader(zin, "utf8")
    println("Took " + measureTimeMillis {
        while (true) {
            val read = z.read()
            if (read == -1) {
                break
            }
            pipeline.consume(read.toChar())
        }
    } + " ms")

    println(length)
    map.asSequence().map { e -> e.key to e.value }.sortedBy { p -> p.first }.forEach {
        println("${it.first} ${it.second}")
    }
}

fun unzip(file: File) {
    val prefix = "xl/sharedStrings.xml"
    ZipInputStream(BufferedInputStream(FileInputStream(file))).use {
        var entry: ZipEntry? = it.nextEntry
        while (entry != null) {
            if (entry.name.startsWith(prefix) && entry.name.endsWith(".xml") && !entry.isDirectory) {
                processSheet(it, entry.name.substring(prefix.length))
            }
            entry = it.nextEntry
        }
    }
}

fun main(args: Array<String>) {
    unzip(File("/home/iyakovlev/Зарегистрированные_ККТ_за_июнь_1.xlsx"))
}