package ru.evotor.report;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;

public class DbUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

	private static final String DB_DRIVER = "oracle.jdbc.driver.OracleDriver";
	private static final String DB_CONNECTION = "jdbc:oracle:thin:@10.12.44.10:1521/EVOTOR";
	private static final String DB_USER = "evotor_reports";
	private static final String DB_PASSWORD = "Xuod2ainBaegai1tail7Feey";

	public static void useConnection(Consumer<Connection> use) {
		Connection connection = null;
		try {
			Class.forName(DB_DRIVER);
			connection = DriverManager.getConnection(DB_CONNECTION, DB_USER, DB_PASSWORD);
			use.accept(connection);
		} catch (Exception e) {
			LOGGER.error("can't use connection", e);
		} finally {
			close(connection);
		}
	}

	public static void usePreparedStatement(Connection connection, String query, Consumer<PreparedStatement> use) {
		PreparedStatement statement = null;
		try {
			statement = connection.prepareStatement(query);
			use.accept(statement);
		} catch (Exception e) {
			LOGGER.error("can't use statement", e);
		} finally {
			close(statement);
		}
	}

	private static void close(Statement statement) {
		try {
			if (statement != null) {
				statement.close();
			}
		} catch (Exception e) {
			LOGGER.error("can't close connection", e);
		}
	}

	private static void close(Connection connection) {
		try {
			if (connection != null) {
				connection.close();
			}
		} catch (Exception e) {
			LOGGER.error("can't close connection", e);
		}
	}

	public interface Consumer<T> {
		void accept(T t) throws Exception;
	}

}
