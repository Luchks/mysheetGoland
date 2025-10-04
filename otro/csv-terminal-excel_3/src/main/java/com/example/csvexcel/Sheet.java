package com.example.csvexcel;

import java.util.ArrayList;
import java.util.List;

public class Sheet {

    private final List<String[]> rows = new ArrayList<>();
    private int maxCols = 0;

    public void addRow(String[] row) {
        // Asegura que todas las filas tengan la misma cantidad de columnas
        if (row.length < maxCols) {
            String[] newRow = new String[maxCols];
            System.arraycopy(row, 0, newRow, 0, row.length);
            for (int i = row.length; i < maxCols; i++) newRow[i] = "";
            row = newRow;
        }
        rows.add(row);
        if (row.length > maxCols) {
            maxCols = row.length;
            normalizeColumnCount();
        }
    }

    public void addColumn() {
        maxCols++;
        for (int i = 0; i < rows.size(); i++) {
            String[] row = rows.get(i);
            String[] newRow = new String[maxCols];
            System.arraycopy(row, 0, newRow, 0, row.length);
            newRow[maxCols - 1] = ""; // nueva celda vacía
            rows.set(i, newRow);
        }
    }

    public void addColumnAt(int index) {
        // Si el índice es mayor al número actual de columnas, lo ajustamos
        if (index > maxCols) index = maxCols;
        maxCols++;

        for (int i = 0; i < rows.size(); i++) {
            String[] row = rows.get(i);
            String[] newRow = new String[maxCols];
            // Copiar las columnas antes del índice
            System.arraycopy(row, 0, newRow, 0, index);
            // Insertar nueva columna vacía
            newRow[index] = "";
            // Copiar las columnas después del índice
            System.arraycopy(row, index, newRow, index + 1, row.length - index);
            rows.set(i, newRow);
        }
    }

    public void removeColumnAt(int index) {
        if (index < 0 || index >= maxCols) return; // índice fuera de rango
        maxCols--;

        for (int i = 0; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (row.length <= 1) {
                // si solo hay una columna, la dejamos vacía
                rows.set(i, new String[maxCols]);
                continue;
            }

            String[] newRow = new String[maxCols];
            // copia todo antes de la columna eliminada
            System.arraycopy(row, 0, newRow, 0, index);
            // copia todo después de la columna eliminada
            if (index < row.length - 1) {
                System.arraycopy(row, index + 1, newRow, index, row.length - index - 1);
            }
            rows.set(i, newRow);
        }
    }

    public void duplicateColumnAt(int index) {
        if (index < 0 || index >= maxCols) return;
        maxCols++;

        for (int i = 0; i < rows.size(); i++) {
            String[] row = rows.get(i);
            String[] newRow = new String[maxCols];

            // Copiar columnas hasta la actual
            System.arraycopy(row, 0, newRow, 0, index + 1);

            // Duplicar la columna actual
            newRow[index + 1] = index < row.length ? row[index] : "";

            // Copiar el resto
            if (index + 1 < row.length) {
                System.arraycopy(row, index + 1, newRow, index + 2, row.length - index - 1);
            }

            rows.set(i, newRow);
        }
    }
    public Cell getCell(int row, int col) {
        if (row >= rows.size() || row < 0) return new Cell("");
        String[] line = rows.get(row);
        if (col >= line.length || col < 0) return new Cell("");
        return new Cell(line[col]);
    }

    
    public void setCell(int row, int col, String value) {
        // Asegura que haya suficientes filas
        while (rows.size() <= row) {
            String[] emptyRow = new String[maxCols > 0 ? maxCols : (col + 1)];
            for (int i = 0; i < emptyRow.length; i++) emptyRow[i] = "";
            rows.add(emptyRow);
        }

        // Asegura que haya suficientes columnas
        if (col >= maxCols) {
            maxCols = col + 1;
            normalizeColumnCount(); // expande todas las filas
        }

        // Establece el valor
        rows.get(row)[col] = value;
    }

    private void normalizeColumnCount() {
        // Asegura que todas las filas tengan el mismo número de columnas
        for (int i = 0; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (row.length < maxCols) {
                String[] newRow = new String[maxCols];
                System.arraycopy(row, 0, newRow, 0, row.length);
                for (int j = row.length; j < maxCols; j++) newRow[j] = "";
                rows.set(i, newRow);
            }
        }
    }

    public int getRowCount() { return rows.size(); }

    public int getColCount() { return maxCols; }

    public List<String[]> getRows() { return rows; }
    //==================================================

    public String evaluateCell(int row, int col) {
        Cell cell = getCell(row, col);
        String val = cell.getValue();

        if (!cell.isFormula()) {
            return val; // no es fórmula
        }

        try {
            String expr = val.substring(1).replaceAll("\\s+", ""); // quita '=' y espacios

            // Soporta referencias como A1, B2, C3
            expr = replaceCellReferences(expr);

            // Evalúa expresión aritmética (sólo + - * /)
            return String.valueOf(evalMath(expr));
        } catch (Exception e) {
            return "#ERR";
        }
    }

    private String replaceCellReferences(String expr) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("([A-Z]+)([0-9]+)");
        java.util.regex.Matcher matcher = pattern.matcher(expr);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String colRef = matcher.group(1);
            String rowRef = matcher.group(2);

            int col = columnToIndex(colRef);
            int row = Integer.parseInt(rowRef) - 1;

            Cell refCell = getCell(row, col);
            String v = refCell.getValue();

            if (refCell.isFormula()) {
                v = evaluateCell(row, col);
            }

            try {
                Double.parseDouble(v);
            } catch (NumberFormatException e) {
                v = "0"; // si no es numérico, se reemplaza por 0
            }

            matcher.appendReplacement(sb, v);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
    // Convierte "A"->0, "B"->1, "AA"->26, etc.
    private int columnToIndex(String col) {
        int result = 0;
        for (int i = 0; i < col.length(); i++) {
            result = result * 26 + (col.charAt(i) - 'A' + 1);
        }
        return result - 1;
    }

    // Evaluador de expresiones matemáticas simples (+, -, *, /)
    private double evalMath(String expr) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < expr.length()) ? expr.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < expr.length()) throw new RuntimeException("Unexpected: " + (char) ch);
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if (eat('+')) x += parseTerm();
                    else if (eat('-')) x -= parseTerm();
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if (eat('*')) x *= parseFactor();
                    else if (eat('/')) x /= parseFactor();
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return parseFactor();
                if (eat('-')) return -parseFactor();

                double x;
                int startPos = this.pos;
                if (eat('(')) {
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(expr.substring(startPos, this.pos));
                } else {
                    throw new RuntimeException("Unexpected: " + (char) ch);
                }

                return x;
            }
        }.parse();
    }
    //==================================================
    //**************************************************

    public void applyFormulaToColumn(int columnIndex, String formula) {
        // Aplicar la fórmula a todas las filas existentes en esa columna
        for (int rowIndex = 0; rowIndex < data.size(); rowIndex++) {
            String replaced = replaceCellReferences(formula, rowIndex);
            String result = evaluateFormula(replaced);
            data.get(rowIndex).get(columnIndex).setValue(result);
        }
    }
    //**************************************************
}
