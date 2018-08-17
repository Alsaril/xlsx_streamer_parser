package ru.evotor.report

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

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

        val queue: Queue<B> = LinkedList()
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

    private fun <C> reshape(callback: Pipeline<A, C>.(B) -> Unit): Pipeline<A, C> {
        val newPipeline = object : Pipeline<A, C>() {
            override fun consume(a: A) {
                this@Pipeline.consume(a)
            }
        }

        setCallback {
            newPipeline.callback(it)
        }
        return newPipeline
    }

    fun <C> map(transform: (B) -> C) = reshape<C> { fire(transform(it)) }

    fun filter(predicate: (B) -> Boolean) = reshape<B> { if (predicate(it)) fire(it) }

    fun dropWhile(predicate: (B) -> Boolean): Pipeline<A, B> {
        val newPipeline = object : Pipeline<A, B>() {
            override fun consume(a: A) {
                this@Pipeline.consume(a)
            }
        }

        var drop = true

        setCallback {
            if (!predicate(it)) {
                drop = false
            }
            if (!drop) {
                newPipeline.fire(it)
            }
        }
        return newPipeline
    }

    fun take(count: Int): Pipeline<A, B> {
        val newPipeline = object : Pipeline<A, B>() {
            override fun consume(a: A) {
                this@Pipeline.consume(a)
            }
        }

        var passed = 0

        setCallback {
            if (passed < count) {
                newPipeline.fire(it)
                passed++
            }
        }
        return newPipeline
    }

    fun drop(count: Int): Pipeline<A, B> {
        val newPipeline = object : Pipeline<A, B>() {
            override fun consume(a: A) {
                this@Pipeline.consume(a)
            }
        }

        var dropped = 0

        setCallback {
            if (dropped >= count) {
                newPipeline.fire(it)
            } else {
                dropped++
            }
        }
        return newPipeline
    }

    fun head(c: (B) -> Unit): Pipeline<A, B> {
        val newPipeline = object : Pipeline<A, B>() {
            override fun consume(a: A) {
                this@Pipeline.consume(a)
            }
        }

        var skip = false

        setCallback {
            if (skip) {
                newPipeline.fire(it)
            } else {
                skip = true
                c(it)
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

abstract class TagToken(name: String) : Token() {
    val name: String
    val attrs: Map<String, String>

    init {
        val s = name.split(" ")
        if (s.isEmpty()) {
            throw IllegalArgumentException("Empty tag")
        }
        this.name = s[0]
        val attrs = mutableMapOf<String, String>()
        s.asSequence().drop(1).forEach {
            val nv = it.split("=")
            attrs[nv[0]] = nv[1].substring(1, nv[1].length - 1)
        }
        this.attrs = Collections.unmodifiableMap(attrs)
    }

    fun attribs() = attrs.map { e -> "${e.key}=\"${e.value}\"" }.joinToString(" ") { it }.let {
        if (!it.isEmpty()) {
            " $it"
        } else {
            ""
        }
    }

    override fun toString() = "$prefix$name${attribs()}$postfix"

    protected abstract val prefix: String
    protected abstract val postfix: String
}

class OpenTagToken(name: String) : TagToken(name) {
    override val prefix = "<"
    override val postfix = ">"
}

class CloseTagToken(name: String) : TagToken(name) {
    override val prefix = "</"
    override val postfix = ">"
}

class SingleTagToken(name: String) : TagToken(name) {
    override val prefix = "<"
    override val postfix = "/>"
}

class CharBuffer(val arr: CharArray, val length: Int)

class TokenCollector : Transformation<CharBuffer, Token> {
    private var tag = false
    private var open = false
    private var start = false
    private var single = false
    private var escaped = false
    private var text = StringBuilder()

    override fun transform(inp: Queue<CharBuffer>): List<Token> {
        val ca = inp.poll()
        val r = mutableListOf<Token>()
        for (i in 0..(ca.length - 1)) {
            val c = ca.arr[i]
            if (c == '<') {
                tag = true
                open = true
                start = true
                single = false
                escaped = false
                val ret = text.toString()
                text = StringBuilder()
                if (!ret.isEmpty()) {
                    r.add(TextToken(ret))
                }
                continue

            } else if (c == '>') {
                tag = false
                val ret = text.toString()
                text = StringBuilder()
                r.add(if (single) SingleTagToken(ret.substring(0, ret.length - 1)) else if (open) OpenTagToken(ret) else CloseTagToken(ret.substring(1)))
                continue
            }

            if (c == '"') {
                escaped = !escaped
            }

            if (start && c == '/') {
                open = false
            }

            if (c == '/' && !start && !escaped) {
                single = true
            }

            if (start) {
                start = false
            }

            text.append(c)
        }
        return r
    }
}

class RowCollector : Transformation<Token, String> {
    private var state = 0
    override fun transform(inp: Queue<Token>): List<String> {
        if (state == 5) return emptyList()
        val result = mutableListOf<String>()
        while (!inp.isEmpty()) {
            val t = inp.poll()

            when {
                state == 0 && t is OpenTagToken && t.name == "si" -> state = 1
                state == 1 && t is OpenTagToken && t.name == "t" -> state = 2
                state == 2 && t is TextToken -> {
                    state = 3
                    result.add(t.text)
                }
                state == 3 && t is CloseTagToken && t.name == "t" -> state = 4
                state == 4 && t is CloseTagToken && t.name == "si" -> state = 0
                else -> state = 5
            }
        }
        return result
    }
}

fun processSheet(zin: ZipInputStream, name: String): Array<String> {
    var ret: Array<String> = Array(0) { "" }
    var i = 0

    val pipeline = Pipeline<CharBuffer>()
            .then(TokenCollector(), 1000)
            .dropWhile { it !is OpenTagToken || it.name != "sst" }
            .head {
                ret = Array((it as? OpenTagToken)?.attrs?.get("uniqueCount")?.toIntOrNull() ?: throw IllegalStateException()) { _ -> "" }
            }
            .then(RowCollector(), 1000)
            .then {
                ret[i++] = it
            }

    val z = InputStreamReader(zin, "utf8")
    val SIZE = 512
    val arr = CharArray(SIZE)

    readLine()

    while (true) {
        val read = z.read(arr, 0, SIZE)
        if (read == -1) {
            break
        }
        pipeline.consume(CharBuffer(arr, read))
    }



    readLine()

    return ret
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