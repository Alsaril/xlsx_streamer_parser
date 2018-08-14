package ru.evotor.report

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.coroutines.experimental.buildSequence

abstract class Pipeline<A, B>(protected val input: Sequence<A>) {
    abstract fun asSequence(): Sequence<B>

    fun <C> addTransformation(t: (Sequence<B>) -> C): Pipeline<A, C> = object : Pipeline<A, C>(input) {
        override fun asSequence() = buildSequence {
            while (true) {
                yield(t(this@Pipeline.asSequence()))
            }
        }
    }
}

class InitialPipeline<A>(input: Sequence<A>) : Pipeline<A, A>(input) {
    override fun asSequence() = input
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

/*class TokenCollector : Transformation<Char, Token> {
    private var tag = false
    private var open = false
    private var start = false
    private var single = false
    private var text = ""

    override fun transform(inp: Queue<Char>): List<Token> {
        buildSequence {

        }

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
}*/

fun processSheet(zin: ZipInputStream, name: String) {
    val map = mutableMapOf<Int, AtomicInteger>()
    val length = AtomicInteger()

    val z = InputStreamReader(zin, "utf8")

    val inp = generateSequence { }
            .map { z.read() }
            .takeWhile { it > 0 }
            .map { it.toChar() }

    val pipeline = InitialPipeline(inp)
    pipeline.asSequence()
            .map { it.toUpperCase() }
            .forEach { print(it) }

    println()
    println(length)
    map.asSequence()
            .map { e -> e.key to e.value }
            .sortedBy { p -> p.first }
            .forEach {
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