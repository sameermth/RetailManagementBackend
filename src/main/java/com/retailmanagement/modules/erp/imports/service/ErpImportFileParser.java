package com.retailmanagement.modules.erp.imports.service;

import com.retailmanagement.common.exceptions.BusinessException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ErpImportFileParser {

    public List<ParsedRow> parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Import file is required");
        }
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        try {
            if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
                return parseWorkbook(file);
            }
            return parseCsv(file);
        } catch (IOException exception) {
            throw new BusinessException("Unable to read import file: " + exception.getMessage());
        }
    }

    private List<ParsedRow> parseWorkbook(MultipartFile file) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                throw new BusinessException("Import file does not contain any rows");
            }
            DataFormatter formatter = new DataFormatter();
            List<String> headers = extractHeaders(sheet.getRow(sheet.getFirstRowNum()), formatter);
            List<ParsedRow> rows = new ArrayList<>();
            for (int index = sheet.getFirstRowNum() + 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (row == null) {
                    continue;
                }
                Map<String, String> values = new LinkedHashMap<>();
                boolean hasValue = false;
                for (int column = 0; column < headers.size(); column++) {
                    String header = headers.get(column);
                    String value = formatter.formatCellValue(row.getCell(column));
                    String normalizedValue = trimToNull(value);
                    values.put(header, normalizedValue);
                    hasValue = hasValue || normalizedValue != null;
                }
                if (hasValue) {
                    rows.add(new ParsedRow(index + 1, values));
                }
            }
            return rows;
        }
    }

    private List<String> extractHeaders(Row headerRow, DataFormatter formatter) {
        if (headerRow == null) {
            throw new BusinessException("Import file header row is missing");
        }
        List<String> headers = new ArrayList<>();
        short lastCellNum = headerRow.getLastCellNum();
        for (int index = 0; index < lastCellNum; index++) {
            Cell cell = headerRow.getCell(index);
            headers.add(normalizeHeader(formatter.formatCellValue(cell)));
        }
        if (headers.isEmpty() || headers.stream().allMatch(String::isBlank)) {
            throw new BusinessException("Import file headers are missing");
        }
        return headers;
    }

    private List<ParsedRow> parseCsv(MultipartFile file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new BusinessException("Import file does not contain any rows");
            }
            List<String> headers = parseCsvLine(stripBom(headerLine)).stream()
                    .map(this::normalizeHeader)
                    .toList();
            List<ParsedRow> rows = new ArrayList<>();
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                List<String> cells = parseCsvLine(line);
                Map<String, String> values = new LinkedHashMap<>();
                boolean hasValue = false;
                for (int index = 0; index < headers.size(); index++) {
                    String header = headers.get(index);
                    String value = index < cells.size() ? trimToNull(cells.get(index)) : null;
                    values.put(header, value);
                    hasValue = hasValue || value != null;
                }
                if (hasValue) {
                    rows.add(new ParsedRow(lineNumber, values));
                }
            }
            return rows;
        }
    }

    private List<String> parseCsvLine(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int index = 0; index < line.length(); index++) {
            char currentChar = line.charAt(index);
            if (currentChar == '"') {
                if (inQuotes && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }
            if (currentChar == ',' && !inQuotes) {
                cells.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(currentChar);
        }
        cells.add(current.toString());
        return cells;
    }

    private String stripBom(String value) {
        return value != null && !value.isEmpty() && value.charAt(0) == '\uFEFF' ? value.substring(1) : value;
    }

    private String normalizeHeader(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return "";
        }
        return trimmed.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record ParsedRow(int rowNumber, Map<String, String> values) {}
}
