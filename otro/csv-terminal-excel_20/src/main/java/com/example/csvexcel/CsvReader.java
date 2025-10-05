package com.example.csvexcel;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.FileReader;
import java.io.FileWriter;
import java.util.List;

public class CsvReader {

    public static Sheet readCsv(String filename) throws Exception {
        Sheet sheet = new Sheet();
        try (CSVReader reader = new CSVReader(new FileReader(filename))) {
            List<String[]> allRows = reader.readAll();
            int maxCols = allRows.stream().mapToInt(arr -> arr.length).max().orElse(0);

            for (String[] row : allRows) {
                if (row.length < maxCols) {
                    // normaliza filas cortas
                    String[] newRow = new String[maxCols];
                    System.arraycopy(row, 0, newRow, 0, row.length);
                    for (int i = row.length; i < maxCols; i++) newRow[i] = "";
                    sheet.addRow(newRow);
                } else {
                    sheet.addRow(row);
                }
            }
        }
        return sheet;
    }

    public static void writeCsv(Sheet sheet, String filename) throws Exception {
        try (CSVWriter writer = new CSVWriter(new FileWriter(filename))) {
            for (String[] row : sheet.getRows()) {
                writer.writeNext(row);
            }
        }
    }
}
