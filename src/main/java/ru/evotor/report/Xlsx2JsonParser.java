package ru.evotor.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Consumer;

public class Xlsx2JsonParser {

	private static final Logger LOGGER = LoggerFactory.getLogger(Xlsx2JsonParser.class);
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	public static void parseFile(File file, Consumer<SheetRecord> process) {
		Workbook workbook = null;
		try {
			try (InputStream inputStream = new FileInputStream(file)){
				workbook = new XSSFWorkbook(inputStream);
				Iterator<Sheet> sheets = workbook.sheetIterator();
				while (sheets.hasNext()) {
					Sheet sheet = sheets.next();
					parseSheet(file.getName(), sheet)
							.ifPresent(process);
				}
			}
			LOGGER.info("parsed file {}", file.getName());
		} catch (Exception e) {
			LOGGER.error("can't parse file", e);
		} finally {
			if (workbook != null) {
				try {
					workbook.close();
				} catch (IOException e) {
					LOGGER.error("can't close workbook", e);
				}
			}
		}
	}

	private static Optional<SheetRecord> parseSheet(String fileName, Sheet sheet) {
		Map<Integer, String> columnKeyMap = new LinkedHashMap<>();
		Map<String, String> transliteratedKeyMap = new LinkedHashMap<>();
		List<Map<String, Object>> mapRows = new ArrayList<>();
		sheet.rowIterator().forEachRemaining(row -> {
			if (row.getRowNum() > 0) {
				mapRows.add(createKeyDataMap(columnKeyMap, row));
			} else {
				parseHeaderColumns(row, cell -> {
					String value = convertCellToString(cell);
					if (value != null && !value.equals("")) {
						String transliterateValue = TransliterationUtils.transliterate(value);
						transliteratedKeyMap.put(transliterateValue, value);
						columnKeyMap.put(cell.getColumnIndex(), transliterateValue);
					}
				});
			}
		});
		return createRecord(fileName, sheet, transliteratedKeyMap, mapRows);
	}

	private static void parseHeaderColumns(Row row, Consumer<Cell> processCell) {
		Iterator<Cell> cells = row.cellIterator();
		while (cells.hasNext()) {
			Cell cell = cells.next();
			processCell.accept(cell);
		}
	}

	private static Map<String, Object> createKeyDataMap(Map<Integer, String> columnKeyMap, Row row) {
		Map<String, Object> map = new LinkedHashMap<>();
		Iterator<Cell> cells = row.cellIterator();
		while (cells.hasNext()) {
			Cell cell = cells.next();
			String key = columnKeyMap.get(cell.getColumnIndex());
			if (key != null) {
				String value = convertCellToString(cell);
				if (value != null && !value.equals("")) {
					map.put(key, value);
				}
			}
		}
		return map;
	}

	private static String convertCellToString(Cell cell) {
		CellType cellTypeEnum = cell.getCellTypeEnum();
		switch (cellTypeEnum) {
			case BLANK:
				return null;
			case NUMERIC:
				return NumberToTextConverter.toText(cell.getNumericCellValue());
			case FORMULA:
				return cell.toString();
			default:
				return cell.getStringCellValue();
		}
	}

	private static Optional<SheetRecord> createRecord(String fileName, Sheet sheet, Map<String, String> transliteratedKeyMap,
	                                                  List<Map<String, Object>> mapRows) {
		try {
			SheetRecord sheetRecord = new SheetRecord();
			sheetRecord.setFileName(fileName);
			sheetRecord.setSheetName(sheet.getSheetName());
			sheetRecord.setKeys(OBJECT_MAPPER.writeValueAsString(transliteratedKeyMap));
			sheetRecord.setData(OBJECT_MAPPER.writeValueAsString(mapRows));
			return Optional.of(sheetRecord);
		} catch (Exception e) {
			LOGGER.error("can't parse sheet", e);
			return Optional.empty();
		}
	}
}
