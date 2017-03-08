package de.axxepta.converterservices;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.extractor.XSSFExportToXml;
import org.apache.poi.xssf.usermodel.XSSFMap;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.*;
import java.util.*;

class ExcelUtils {

    private static Logger logger = LoggerFactory.getLogger(ExcelUtils.class);

    private static final String XML_PROLOGUE    = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>"
            + System.lineSeparator();
    private static final String SHEET_INDENT    = "  ";
    private static final String ROW_INDENT      = "    ";
    private static final String COL_INDENT      = "      ";
    private static final String CONTENT_INDENT  = "        ";
    private static final String FILE_EL         = "workbook";
    static final String SHEET_EL                = "sheet";
    static final String ROW_EL                  = "row";
    static final String COL_EL                  = "column";
    static final String DEF_SHEET_NAME          = "sheet0";
    static final String DEF_ATT_SHEET           = "id";
    static final String DEF_SEPARATOR           = ";";

    private ExcelUtils() {}


    static List<String> fromExcel(String fileName, FileType type, boolean customXMLMapping,String sheetName,
                          String separator, boolean indent, boolean columnFirst, boolean firstColName, boolean firstRowName,
                          String fileEl, String sheetEl, String rowEl, String colEl) {
        List<String> outputFiles = new ArrayList<>();
        try (FileInputStream file = new FileInputStream(App.TEMP_FILE_PATH + "/" + fileName)) {
            Workbook workbook = new XSSFWorkbook(file);
            if (type.equals(FileType.CSV)) {
                outputFiles.addAll(excelToCSV(fileName, workbook, sheetName, separator));
            }
            if (type.equals(FileType.XML)) {
                if (customXMLMapping)
                    outputFiles.addAll(excelCustomXMLMapping(fileName));
                else
                    outputFiles.add(excelToXML(fileName, workbook, sheetName, columnFirst, firstColName, firstRowName,
                        fileEl, sheetEl, rowEl, colEl, indent));
            }
        } catch (IOException ie) {
            logger.error("Exception reading Excel file: " + ie.getMessage());
        }
        return outputFiles;
    }

    private static List<String> excelToCSV(String fileName, Workbook workbook, String sheetName, String separator) {
        List<String> outputFiles = new ArrayList<>();
        List<Sheet> sheets = getSheets(workbook, sheetName);
        DataFormatter formatter = new DataFormatter(true);
        for (Sheet sheet : sheets) {
            String convertedFileName = csvFileName(fileName, sheet.getSheetName());
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(App.TEMP_FILE_PATH + "/" + convertedFileName))) {
                int firstRow = sheet.getFirstRowNum();
                int lastRow = sheet.getLastRowNum();
                int firstColumn = Math.min(sheet.getRow(firstRow).getFirstCellNum(), sheet.getRow(firstRow + 1).getFirstCellNum());
                int lastColumn = Math.max(sheet.getRow(firstRow).getLastCellNum(), sheet.getRow(firstRow + 1).getLastCellNum());
                for (int rowNumber = firstRow; rowNumber < lastRow + 1; rowNumber++) {
                    Row row = sheet.getRow(rowNumber);
                    StringJoiner joiner = new StringJoiner(separator);
                    for (int colNumber = firstColumn; colNumber < lastColumn; colNumber++) {
                        Cell cell = row.getCell(colNumber);
                        joiner.add(formatter.formatCellValue(cell));
                    }
                    writer.write(joiner.toString() + System.lineSeparator());
                }
            } catch (IOException ie) {
                logger.error("Exception writing to CSV file: " + ie.getMessage());
            }
            outputFiles.add(convertedFileName);
        }
        return outputFiles;
    }

    private static List<String> excelCustomXMLMapping(String fileName) throws IOException {
        List<String> customMappingFiles = new ArrayList<>();
        try {
            OPCPackage pkg = OPCPackage.open(fileName);
            XSSFWorkbook wb = new XSSFWorkbook(pkg);
            for (XSSFMap map : wb.getCustomXMLMappings()) {
                XSSFExportToXml exporter = new XSSFExportToXml(map);
                String outputFile = xmlMappingFileName(fileName, map.hashCode());
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    exporter.exportToXML(baos, true);
                    try (OutputStream outputStream = new FileOutputStream(App.TEMP_FILE_PATH + "/" + outputFile)) {
                        baos.writeTo(outputStream);
                        customMappingFiles.add(outputFile);
                    }
                }
            }
            pkg.close();
        } catch (InvalidFormatException | SAXException | ParserConfigurationException | TransformerException ife) {
            logger.error("Exception writing to CSV file: " + ife.getMessage());
        }
        return customMappingFiles;
    }

    static String csvToExcel(String fileName, String sheetName, String separator) {
        String outputFile = xlsxFileName(fileName);
        XSSFWorkbook workbook = new XSSFWorkbook();
        try {
            FileOutputStream out = new FileOutputStream(new File(App.TEMP_FILE_PATH + "/" + outputFile));
            XSSFSheet sheet = workbook.createSheet(sheetName);
            try (FileInputStream fis = new FileInputStream(App.TEMP_FILE_PATH + "/" + fileName)) {
                Scanner scanner = new Scanner(fis);
                int rowId = 0;
                while (scanner.hasNext()) {
                    String line = scanner.nextLine();
                    String[] cellContents = line.split(separator);
                    XSSFRow row = sheet.createRow(rowId++);
                    int cellId = 0;
                    for (String el : cellContents)
                    {
                        Cell cell = row.createCell(cellId++);
                        cell.setCellValue(el);
                    }
                }
            }
            workbook.write(out);
            out.close();
        } catch (IOException ie) {
            logger.error("Exception writing to XLSX file: " + ie.getMessage());
        }
        return outputFile;
    }

    static String xmlToExcel(String path) {
        return "";
    }

    private static String excelToXML(String fileName, Workbook workbook, String sheetName, boolean columnFirst,
                                   boolean firstColName, boolean firstRowName,
                                   String fileEl, String sheetEl, String rowEl, String colEl,
                                   boolean indent) {
        List<Sheet> sheets = getSheets(workbook, sheetName);
        DataFormatter formatter = new DataFormatter(true);

        String outputFile = xmlFileName(fileName);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(App.TEMP_FILE_PATH + "/" + outputFile))) {
            writer.write(XML_PROLOGUE);
            writeTag(writer, TagType.open, fileEl.equals("") ? FILE_EL : fileEl, indent, "");
            for (Sheet sheet : sheets) {
                writeTag(writer, TagType.open, sheetEl.equals("") ? SHEET_EL : sheetEl, indent, SHEET_INDENT);
                int firstRow = sheet.getFirstRowNum();
                int lastRow = sheet.getLastRowNum();
                int firstColumn = Math.min(sheet.getRow(firstRow).getFirstCellNum(), sheet.getRow(firstRow + 1).getFirstCellNum());
                int lastColumn = Math.max(sheet.getRow(firstRow).getLastCellNum(), sheet.getRow(firstRow + 1).getLastCellNum());
                if (columnFirst) {
                    //
                } else {
                    for (int rowNumber = firstRow; rowNumber < lastRow + 1; rowNumber++) {
                        Row row = sheet.getRow(rowNumber);
                        writeTag(writer, TagType.open, rowEl.equals("") ? ROW_EL : rowEl, indent, ROW_INDENT,
                                "RowNumber", Integer.toString(row.getRowNum()));
                        for (int colNumber = firstColumn; colNumber < lastColumn; colNumber++) {
                            Cell cell = row.getCell(colNumber);
                            writeElement(writer, colEl.equals("") ? COL_EL : colEl,
                                    formatter.formatCellValue(cell), indent, CONTENT_INDENT, COL_INDENT,
                                    "Ref", (cell != null) ? cell.getAddress().toString() : "_",
                                    "ColumnNumber", (cell != null) ? Integer.toString(cell.getColumnIndex()) : "_");
                        }
                        writeTag(writer, TagType.close, rowEl.equals("") ? ROW_EL : rowEl, indent, ROW_INDENT);
                    }
                }
                writeTag(writer, TagType.close, sheetEl.equals("") ? SHEET_EL : sheetEl, indent, SHEET_INDENT);
            }
            writeTag(writer, TagType.close, fileEl.equals("") ? FILE_EL : fileEl, indent, "");
        } catch (IOException ie){
            logger.error("Exception writing to XML file: " + ie.getMessage());
        }
        return outputFile;
    }

    private static List<Sheet> getSheets(Workbook workbook, String sheetName) {
        List<Sheet> sheets = new ArrayList<>();
        if (!sheetName.equals("") && (workbook.getSheet(sheetName) != null)) {
            sheets.add(workbook.getSheet(sheetName));
        } else {
            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                sheets.add(workbook.getSheetAt(s));
            }
        }
        return sheets;
    }

    private static void writeTag(BufferedWriter writer, TagType tag, String name,
                                 boolean indent, String indentString, String ... attributes) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append(indent ? indentString : "");
        if (tag.equals(TagType.open)) {
            builder.append("<").append(name);
            for (int i = 0; i < attributes.length / 2; i++) {
                builder.append(" ").append(attributes[2 * i]).append("=\"").append(attributes[2 * i + 1]).append("\"");
            }
            builder.append(">");
        } else {
            builder.append("</").append(name).append(">");
        }
        builder.append(indent ? System.lineSeparator() : "");
        writer.write(builder.toString());
    }

    private static void writeElement(BufferedWriter writer, String name, String content,
                                 boolean indent, String indentContentString, String indentTagString,
                                     String ... attributes) throws IOException {
        writeTag(writer, TagType.open, name, indent, indentTagString, attributes);
        String s = (indent ? indentContentString : "").concat(content).concat(indent ? System.lineSeparator() : "");
        writer.write(s);
        writeTag(writer, TagType.close, name, indent, indentTagString);
    }

    private static String xlsxFileName(String name) {
        return name.substring(0, name.lastIndexOf(".")) + ".xlsx";
    }

    private static String xmlFileName(String xlsName) {
        return xlsName.substring(0, xlsName.lastIndexOf(".")) + ".xml";
    }

    private static String xmlMappingFileName(String xlsName, int hash) {
        return xlsName.substring(0, xlsName.lastIndexOf(".")) + "_" + Integer.toString(hash) + ".xml";
    }

    private static String csvFileName(String xlsName, String sheetName) {
        return xlsName.substring(0, xlsName.lastIndexOf(".")) + "_" + sheetName + ".csv";
    }


    public enum FileType {
        XLSX,
        CSV,
        XML
    }

    private enum TagType {
        open,
        close
    }
}
