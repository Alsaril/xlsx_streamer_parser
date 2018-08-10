package ru.evotor.report

import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.sql.Connection
import java.util.*

object App {
    private val LOGGER = LoggerFactory.getLogger(App::class.java)
    private const val QUERY = "insert into" +
            " evotor_reports.import_excel2json" +
            " (file_name, sheet_name, column_names_map, file_json)" +
            " values (?, ?, ?, ?)"

    @JvmStatic
    fun main(args: Array<String>) {
        Locale.setDefault(Locale.US)
        if (args.isEmpty()) {
            LOGGER.error("parse directory is empty")
            return
        }
        val dir = args[0]
        LOGGER.info("start parsing directory: {}", dir)
        try {
            DbUtils.useConnection { conn ->
                Files.walk(Paths.get(dir), 1)
                        .filter { Files.isRegularFile(it) }
                        .filter { !it.toFile().isHidden }
                        .filter { !it.fileName.toString().startsWith("~") }
                        .filter { it.toString().endsWith(".xlsx") }
                        .forEach { processFile(conn, it) }
            }
            LOGGER.info("finish parsing")
        } catch (e: Exception) {
            LOGGER.error("Unexpected behavior", e)
        }
    }

    private fun processFile(connection: Connection, path: Path) {
        Xlsx2JsonParser.parseFile(path.toFile()) {
            saveSheet(connection, it)
        }
    }

    private fun saveSheet(connection: Connection, sheetRecord: SheetRecord) {
        DbUtils.usePreparedStatement(connection, QUERY) {
            it.setString(1, sheetRecord.fileName)
            it.setString(2, sheetRecord.sheetName)
            it.setString(3, sheetRecord.keys)
            it.setString(4, sheetRecord.data)
            it.executeUpdate()
        }
    }
}
