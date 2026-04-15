package com.retailmanagement.modules.erp.imports.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ErpImportFileParserTest {

    private final ErpImportFileParser parser = new ErpImportFileParser();

    @Test
    void parseCsv_normalizesHeadersAndSkipsBlankRows() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "customers.csv",
                "text/csv",
                ("Customer Code,Full Name,Email\n" +
                        "CUST-001,Aarav Traders,aarav@example.com\n" +
                        ",,\n").getBytes(StandardCharsets.UTF_8)
        );

        List<ErpImportFileParser.ParsedRow> rows = parser.parse(file);

        assertEquals(1, rows.size());
        assertEquals(2, rows.get(0).rowNumber());
        assertEquals("CUST-001", rows.get(0).values().get("customercode"));
        assertEquals("Aarav Traders", rows.get(0).values().get("fullname"));
        assertEquals("aarav@example.com", rows.get(0).values().get("email"));
    }

    @Test
    void parseWorkbook_readsFirstSheet() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Products");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("SKU");
            header.createCell(1).setCellValue("Name");
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue("BAT-001");
            row.createCell(1).setCellValue("Battery");
            workbook.write(outputStream);
        }

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "products.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                outputStream.toByteArray()
        );

        List<ErpImportFileParser.ParsedRow> rows = parser.parse(file);

        assertEquals(1, rows.size());
        assertEquals("BAT-001", rows.get(0).values().get("sku"));
        assertEquals("Battery", rows.get(0).values().get("name"));
        assertNull(rows.get(0).values().get("description"));
    }
}
