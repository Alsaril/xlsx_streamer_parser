package ru.evotor.report;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.Locale;

public class App {

	private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

	private static final String QUERY = "insert into" +
			" evotor_reports.import_excel2json" +
			" (file_name, sheet_name, column_names_map, file_json)" +
			" values (?, ?, ?, ?)";

	public static void main(String[] args) {
		Locale.setDefault(Locale.US);
		if (args == null || args.length == 0) {
			LOGGER.error("parse directory is empty");
			return;
		}
		String dir = args[0];
		LOGGER.info("start parsing directory: {}", dir);
		DbUtils.useConnection(connection ->
				Files.walk(Paths.get(dir), 1)
						.filter(Files::isRegularFile)
						.filter(path -> !path.toFile().isHidden())
						.filter(path -> !path.getFileName().toString().startsWith("~"))
						.filter(path -> path.toString().endsWith(".xlsx"))
						.forEach(path -> processFile(connection, path)));
		LOGGER.info("finish parsing");
	}

	private static void processFile(Connection connection, Path path) {
		Xlsx2JsonParser.parseFile(path.toFile(), sheetRecord -> saveSheet(connection, sheetRecord));
	}

	private static void saveSheet(Connection connection, SheetRecord sheetRecord) {
		LOGGER.info(sheetRecord.getData());
//		DbUtils.usePreparedStatement(connection, QUERY, statement -> {
//			statement.setString(1, sheetRecord.getFileName());
//			statement.setString(2, sheetRecord.getSheetName());
//			statement.setString(3, sheetRecord.getKeys());
//			statement.setString(4, sheetRecord.getData());
//			statement.executeUpdate();
//		});
	}

}
