package com.yiling.stocktake.service;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Lightweight XLSX generator with freeze panes, merged cells and number formats. */
@Service
public class XlsxExportService {
    public byte[] create(String sheetName, List<List<Object>> rows) {
        return create(sheetName, rows, Collections.<String>emptyList(), 0, 0, Collections.<Integer, String>emptyMap());
    }

    public byte[] create(String sheetName, List<List<Object>> rows, List<String> merges,
                         int freezeRows, int freezeCols, Map<Integer, String> numberFormats) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(output)) {
                put(zip, "[Content_Types].xml", contentTypes());
                put(zip, "_rels/.rels", rootRels());
                put(zip, "xl/workbook.xml", workbook(sheetName));
                put(zip, "xl/_rels/workbook.xml.rels", workbookRels());
                put(zip, "xl/styles.xml", styles());
                put(zip, "xl/worksheets/sheet1.xml", worksheet(rows, merges, freezeRows, freezeCols, numberFormats));
            }
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("生成 XLSX 文件失败", ex);
        }
    }

    private void put(ZipOutputStream zip, String name, String value) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(value.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private String contentTypes() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"
                + "</Types>";
    }

    private String rootRels() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                + "</Relationships>";
    }

    private String workbook(String sheetName) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                + "<sheets><sheet name=\"" + xml(sheetName) + "\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>";
    }

    private String workbookRels() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
                + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>"
                + "</Relationships>";
    }

    private String styles() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                + "<numFmts count=3><numFmt numFmtId=\"164\" formatCode=\"0.###\"/><numFmt numFmtId=\"165\" formatCode=\"0.00\"/><numFmt numFmtId=\"166\" formatCode=\"0.######\"/></numFmts>"
                + "<fonts count=3><font><sz val=\"11\"/><name val=\"Microsoft YaHei\"/></font><font><b/><sz val=\"14\"/><name val=\"Microsoft YaHei\"/></font><font><b/><sz val=\"11\"/><name val=\"Microsoft YaHei\"/></font></fonts>"
                + "<fills count=5><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill><fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFDCEEFF\"/><bgColor indexed=\"64\"/></patternFill></fill><fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFF2F6FA\"/><bgColor indexed=\"64\"/></patternFill></fill><fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFEAF7EF\"/><bgColor indexed=\"64\"/></patternFill></fill></fills>"
                + "<borders count=2><border><left/><right/><top/><bottom/><diagonal/></border><border><left style=\"thin\"><color rgb=\"FFD9E2EC\"/></left><right style=\"thin\"><color rgb=\"FFD9E2EC\"/></right><top style=\"thin\"><color rgb=\"FFD9E2EC\"/></top><bottom style=\"thin\"><color rgb=\"FFD9E2EC\"/></bottom><diagonal/></border></borders>"
                + "<cellStyleXfs count=1><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs><cellXfs count=12>"
                + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/><xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyAlignment=\"1\"><alignment horizontal=\"center\"/></xf>"
                + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"3\" borderId=\"0\" xfId=\"0\"/><xf numFmtId=\"0\" fontId=\"2\" fillId=\"2\" borderId=\"1\" xfId=\"0\" applyAlignment=\"1\"><alignment horizontal=\"center\" vertical=\"center\" wrapText=\"1\"/></xf>"
                + "<xf numFmtId=\"0\" fontId=\"2\" fillId=\"3\" borderId=\"1\" xfId=\"0\" applyAlignment=\"1\"><alignment horizontal=\"center\" vertical=\"center\" wrapText=\"1\"/></xf><xf numFmtId=\"0\" fontId=\"0\" fillId=\"4\" borderId=\"1\" xfId=\"0\"/>"
                + "<xf numFmtId=\"164\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\"/><xf numFmtId=\"165\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\"/><xf numFmtId=\"166\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\"/>"
                + "<xf numFmtId=\"164\" fontId=\"0\" fillId=\"4\" borderId=\"1\" xfId=\"0\"/><xf numFmtId=\"165\" fontId=\"0\" fillId=\"4\" borderId=\"1\" xfId=\"0\"/><xf numFmtId=\"166\" fontId=\"0\" fillId=\"4\" borderId=\"1\" xfId=\"0\"/>"
                + "</cellXfs></styleSheet>";
    }

    private String worksheet(List<List<Object>> rows, List<String> merges, int freezeRows, int freezeCols,
                             Map<Integer, String> numberFormats) {
        int rowCount = rows == null ? 0 : rows.size();
        int maxColumns = 1;
        if (rows != null) for (List<Object> row : rows) if (row != null) maxColumns = Math.max(maxColumns, row.size());
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        xml.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">");
        if (freezeRows > 0 || freezeCols > 0) {
            xml.append("<sheetViews><sheetView workbookViewId=\"0\"><pane");
            if (freezeCols > 0) xml.append(" xSplit=\"").append(freezeCols).append("\"");
            if (freezeRows > 0) xml.append(" ySplit=\"").append(freezeRows).append("\"");
            xml.append(" topLeftCell=\"").append(columnName(Math.max(1, freezeCols + 1))).append(Math.max(1, freezeRows + 1)).append("\" activePane=\"bottomRight\" state=\"frozen\"/></sheetView></sheetViews>");
        }
        xml.append("<cols>");
        for (int col = 1; col <= maxColumns; col++) {
            double width = col <= 4 ? (col == 2 ? 28 : 16) : 15;
            xml.append("<col min=\"").append(col).append("\" max=\"").append(col).append("\" width=\"").append(width).append("\" customWidth=\"1\"/>");
        }
        xml.append("</cols><sheetData>");
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            List<Object> row = rows.get(rowIndex) == null ? Collections.<Object>emptyList() : rows.get(rowIndex);
            xml.append("<row r=\"").append(rowIndex + 1).append("\">");
            for (int col = 0; col < row.size(); col++) {
                Object value = row.get(col);
                if (value == null) continue;
                int style = styleFor(rowIndex, rowCount, value, col + 1, numberFormats);
                String ref = columnName(col + 1) + (rowIndex + 1);
                xml.append("<c r=\"").append(ref).append("\" s=\"").append(style).append("\"");
                if (value instanceof Number) xml.append("><v>").append(xml(number(value))).append("</v></c>");
                else xml.append(" t=\"inlineStr\"><is><t xml:space=\"preserve\">").append(xml(String.valueOf(value))).append("</t></is></c>");
            }
            xml.append("</row>");
        }
        xml.append("</sheetData>");
        if (merges != null && !merges.isEmpty()) {
            xml.append("<mergeCells count=\"").append(merges.size()).append("\">");
            for (String merge : merges) xml.append("<mergeCell ref=\"").append(xml(merge)).append("\"/>");
            xml.append("</mergeCells>");
        }
        return xml.append("</worksheet>").toString();
    }

    private int styleFor(int rowIndex, int rowCount, Object value, int column, Map<Integer, String> formats) {
        if (rowIndex == 0) return 1;
        if (rowIndex == 4) return 3;
        if (rowIndex == 5) return 4;
        boolean summary = rowIndex == rowCount - 1;
        if (!(value instanceof Number)) return summary ? 5 : 0;
        String format = formats == null ? null : formats.get(column);
        boolean amount = "amount".equalsIgnoreCase(format);
        boolean price = "price".equalsIgnoreCase(format);
        if (summary) return amount ? 10 : price ? 11 : 9;
        return amount ? 7 : price ? 8 : 6;
    }

    private String number(Object value) {
        if (value instanceof BigDecimal) return ((BigDecimal) value).toPlainString();
        if (value instanceof LocalDate || value instanceof LocalDateTime) return value.toString();
        return String.valueOf(value);
    }

    private String columnName(int column) {
        StringBuilder result = new StringBuilder();
        int value = column;
        while (value > 0) { int remainder = (value - 1) % 26; result.insert(0, (char) ('A' + remainder)); value = (value - 1) / 26; }
        return result.toString();
    }

    private String xml(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }
}
