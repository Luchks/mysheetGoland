package com.example.csvexcel;

import java.util.ArrayList;
import java.util.List;

public class Sheet {

    private final List<String[]> rows = new ArrayList<>();
    private int maxCols = 0;

    public void addRow(String[] row) {
        rows.add(row);
        if (row.length > maxCols) maxCols = row.length;
    }

    public Cell getCell(int row, int col) {
        if (row >= rows.size() || row < 0) return new Cell(""); // fila fuera de rango
        String[] line = rows.get(row);
        if (col >= line.length || col < 0) return new Cell(""); // columna fuera de rango
        return new Cell(line[col]);
    }

    public void setCell(int row, int col, String value) {
        if (row >= rows.size()) {
            // agrega filas vacías si es necesario
            while (rows.size() <= row) {
                rows.add(new String[maxCols]);
                for (int i = 0; i < maxCols; i++) rows.get(rows.size()-1)[i] = "";
            }
        }
        String[] line = rows.get(row);
        if (col >= line.length) {
            // expande fila si es necesario
            String[] newLine = new String[col + 1];
            System.arraycopy(line, 0, newLine, 0, line.length);
            for (int i = line.length; i < newLine.length; i++) newLine[i] = "";
            rows.set(row, newLine);
            if (col + 1 > maxCols) maxCols = col + 1;
            line = newLine;
        }
        line[col] = value;
    }

    public int getRowCount() {
        return rows.size();
    }

    public int getColCount() {
        return maxCols;
    }

    public List<String[]> getRows() {
        return rows;
    }
}
