package ru.evotor.report

import com.fasterxml.jackson.databind.ObjectMapper
import com.monitorjbl.xlsx.StreamingReader
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType.*
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.util.NumberToTextConverter
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*

object Xlsx2JsonParser {
    private val LOGGER = LoggerFactory.getLogger(Xlsx2JsonParser::class.java)
    private val OBJECT_MAPPER = ObjectMapper()

    fun parseFile(file: File, process: (SheetRecord) -> Unit) {
        StreamingReader.builder()
                .rowCacheSize(100)
                .bufferSize(4096)
                .open(file).use { workbook ->
            workbook.forEach {
                process(parseSheet(file.name, it))
            }
        }
        LOGGER.info("parsed file {}", file.name)
    }

    private fun parseSheet(fileName: String, sheet: Sheet): SheetRecord {
        val columnKeyMap = LinkedHashMap<Int, String>()
        val transliteratedKeyMap = LinkedHashMap<String, String>()
        val mapRows = ArrayList<Map<String, Any>>()
        sheet.forEach { row ->
            if (row.rowNum > 0) {
                mapRows.add(createKeyDataMap(columnKeyMap, row))
            } else {
                row.forEach { cell ->
                    convertCellToString(cell)?.let {
                        val transliterateValue = TransliterationUtils.transliterate(it)
                        transliteratedKeyMap[transliterateValue] = it
                        columnKeyMap[cell.columnIndex] = transliterateValue
                    }
                }
            }
        }
        return SheetRecord(fileName, sheet.sheetName, OBJECT_MAPPER.writeValueAsString(transliteratedKeyMap), OBJECT_MAPPER.writeValueAsString(mapRows))
    }

    private fun createKeyDataMap(columnKeyMap: Map<Int, String>, row: Row) = row
            .asSequence()
            .map { columnKeyMap[it.columnIndex] to convertCellToString(it) }
            .filter { it.first != null && it.second != null }
            .unsafeToMap()

    private fun convertCellToString(cell: Cell) = when (cell.cellTypeEnum) {
        BLANK -> null
        NUMERIC -> NumberToTextConverter.toText(cell.numericCellValue)
        FORMULA -> cell.toString()
        else -> cell.stringCellValue
    }.isEmptyThenNull()
}

fun String?.isEmptyThenNull(): String? = if (this == "") null else this
fun <A, B> Sequence<Pair<A?, B?>>.unsafeToMap() = this.map { it.first as A to it.second as B }.toMap()
