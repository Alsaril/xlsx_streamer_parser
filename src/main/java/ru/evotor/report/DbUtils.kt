package ru.evotor.report

import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement

object DbUtils {
    private val DB_DRIVER = "oracle.jdbc.driver.OracleDriver"
    private val DB_CONNECTION = "jdbc:oracle:thin:@10.12.44.10:1521/EVOTOR"
    private val DB_USER = "evotor_reports"
    private val DB_PASSWORD = "Xuod2ainBaegai1tail7Feey"

    fun useConnection(use: (Connection) -> Unit) {
        Class.forName(DB_DRIVER)
        DriverManager.getConnection(DB_CONNECTION, DB_USER, DB_PASSWORD).use(use)
    }

    fun usePreparedStatement(connection: Connection, query: String, use: (PreparedStatement) -> Unit) {
        connection.prepareStatement(query).use(use)
    }
}
