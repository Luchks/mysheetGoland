package com.example.csvexcel;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.*;

public class Sheet {

    private int totalOriginalRows = 0;
    private List<String[]> originalRows = null;
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
        totalOriginalRows++;
    }
    public int getTotalOriginalRows() {
        return totalOriginalRows;
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
        // Asegura que la fila exista
        while (rows.size() <= row) {
            rows.add(new String[col + 1]);
        }

        // Asegura que la columna exista en esta fila
        String[] rowArray = rows.get(row);
        if (rowArray.length <= col) {
            String[] newRow = new String[col + 1];
            System.arraycopy(rowArray, 0, newRow, 0, rowArray.length);
            rows.set(row, newRow);
            rowArray = newRow;
        }

        // Asigna el valor
        rows.get(row)[col] = value;

        // Si es una fórmula (empieza con =), aplica a toda la columna automáticamente
        if (value.startsWith("=")) {
            applyFormulaToColumn(col,row, value);
        }
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
            expr = replaceCellReferences(expr, row);

            // Evalúa expresión aritmética (sólo + - * /)
            return String.valueOf(evalMath(expr));
        } catch (Exception e) {
            return "#ERR";
        }
    }
//`````````````````````````````````````````````````````````````````

    private String replaceCellReferences(String formula, int rowIndex) {
        String result = formula;
        String[] row = rows.get(rowIndex);

        for (int colIndex = 0; colIndex < row.length; colIndex++) {
            char colLetter = (char) ('A' + colIndex);
            String cellRef = "" + colLetter + (rowIndex + 1);

            String cellValue = row[colIndex];
            if (cellValue == null || cellValue.isEmpty()) cellValue = "0";

            result = result.replaceAll(cellRef, cellValue);
        }

        return result;
    }
 
       
    public String getColumnName(int index) {
        return String.valueOf((char) ('A' + index));
    }

    private String evaluateFormula(String expr) {
        try {
            if (expr.startsWith("=")) expr = expr.substring(1);
            expr = expr.replaceAll("\\s+", ""); // limpia espacios

            // ✅ Reemplaza todas las referencias antes
            // (si ya vienen reemplazadas no pasa nada)
            double result = evalMath(expr);
            return String.valueOf(result);

        } catch (Exception e) {
            System.out.println("⚠️ Error evaluando fórmula: " + expr);
            return "#ERR";
        }
    }
    private double simpleEval(String expr) {
        expr = expr.replaceAll("[^0-9+\\-*/.]", ""); // limpia caracteres extraños
        String[] nums = expr.split("[+]");
        double sum = 0;
        for (String n : nums) sum += Double.parseDouble(n);
        return sum;
    }
   //`````````````````````````````````````````````````````````````````
    private int columnToIndex(String col) {
        int index = 0;
        for (int i = 0; i < col.length(); i++) {
            index = index * 26 + (col.charAt(i) - 'A' + 1);
        }
        return index - 1;
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

    public void fillColumnWithFormula(int columnIndex, String formula) {
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            String replaced = replaceCellReferences(formula, rowIndex);
            String result = evaluateFormula(replaced);
            setCell(rowIndex, columnIndex, result);
        }
    }


    // ✅ Filtrar acumulativamente sobre el resultado actual
    public void filterByColumn(int columnIndex, String condition) {
        if (originalRows == null) {
            originalRows = new ArrayList<>(rows); // guarda la primera vez
        }

        List<String[]> filtered = new ArrayList<>();

        String operator = "";
        String valueStr = condition.replaceAll("^[><=!]+", "").trim();
        double valueNum = 0;
        boolean isNumeric = valueStr.matches("-?\\d+(\\.\\d+)?");

        if (isNumeric) valueNum = Double.parseDouble(valueStr);

        if (condition.startsWith(">=")) operator = ">=";
        else if (condition.startsWith("<=")) operator = "<=";
        else if (condition.startsWith(">")) operator = ">";
        else if (condition.startsWith("<")) operator = "<";
        else if (condition.startsWith("==")) operator = "==";
        else if (condition.startsWith("!=")) operator = "!=";
        else {
            System.out.println("❌ Condición inválida: " + condition);
            return;
        }

        for (String[] row : rows) {
            if (columnIndex >= row.length) continue;
            String cell = row[columnIndex];
            boolean matches = false;

            if (isNumeric) {
                try {
                    double cellVal = Double.parseDouble(cell);
                    matches = switch (operator) {
                        case ">" -> cellVal > valueNum;
                        case "<" -> cellVal < valueNum;
                        case ">=" -> cellVal >= valueNum;
                        case "<=" -> cellVal <= valueNum;
                        case "==" -> cellVal == valueNum;
                        case "!=" -> cellVal != valueNum;
                        default -> false;
                    };
                } catch (NumberFormatException e) {
                    matches = false;
                }
            } else {
                matches = switch (operator) {
                    case "==" -> cell.equalsIgnoreCase(valueStr);
                    case "!=" -> !cell.equalsIgnoreCase(valueStr);
                    default -> false;
                };
            }

            if (matches) filtered.add(row);
        }

        // Sustituye filas actuales por las filtradas
        rows.clear();
        rows.addAll(filtered);
    }

    // ✅ Restaurar todo el dataset original
    public void clearFilter() {
        if (originalRows != null) {
            rows.clear();
            rows.addAll(originalRows);
            originalRows = null;
        }
    }

    public boolean isColumnEmpty(int colIndex) {
        for (String[] row : rows) {
            if (colIndex < row.length && !row[colIndex].trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`````

    // Nuevo applyFormulaToColumn: recibe la fila de origen (sourceRow)
    public void applyFormulaToColumn(int columnIndex, int sourceRow, String formula) {
        if (formula == null || !formula.startsWith("=")) return;

        for (int targetRow = 0; targetRow < rows.size(); targetRow++) {
            try {
                // 1) Ajusta los números de fila de las referencias según la diferencia de filas
                String shifted = shiftRowNumbersInFormula(formula, sourceRow, targetRow);

                // 2) Reemplaza referencias por valores (evalúa celdas referenciadas)
                String replaced = replaceCellReferencesForEvaluation(shifted, targetRow, columnIndex);

                // 3) Evalúa la fórmula resultante (evaluateFormula ya maneja el '=').
                String result = evaluateFormula(replaced);

                // 4) Guarda el resultado en la columna objetivo
                // Asegúrate de que la fila tenga suficientes columnas (normalizar si es necesario)
                String[] rowArr = rows.get(targetRow);
                if (columnIndex >= rowArr.length) {
                    String[] newRow = new String[columnIndex + 1];
                    System.arraycopy(rowArr, 0, newRow, 0, rowArr.length);
                    for (int k = rowArr.length; k < newRow.length; k++) newRow[k] = "";
                    rows.set(targetRow, newRow);
                }
                rows.get(targetRow)[columnIndex] = result;
            } catch (Exception e) {
                rows.get(targetRow)[columnIndex] = "#ERR";
            }
        }
    }

    // Ajusta los números de fila en todas las referencias tipo A1, B12, AA3, etc.
    // por el delta = targetRow - sourceRow
    private String shiftRowNumbersInFormula(String formula, int sourceRow, int targetRow) {
        Matcher m = Pattern.compile("([A-Za-z]+)(\\d+)").matcher(formula);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String col = m.group(1);
            int refRow = Integer.parseInt(m.group(2)); // 1-based
            int newRow = refRow + (targetRow - sourceRow);
            if (newRow < 1) newRow = 1; // evitar filas < 1
            m.appendReplacement(sb, col + newRow);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    // Reemplaza referencias (A2, B3, ...) por el valor evaluado de esa celda.
    // targetRow = fila donde se está evaluando la fórmula (0-based).
    // fillingColumn = columna que estamos llenando (usada para evitar circularidad simple).
    private String replaceCellReferencesForEvaluation(String formula, int targetRow, int fillingColumn) {
        Matcher m = Pattern.compile("([A-Za-z]+)(\\d+)").matcher(formula);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String colLetters = m.group(1);
            int rowNum = Integer.parseInt(m.group(2)); // 1-based
            int refRowIndex = rowNum - 1;
            int refColIndex = columnToIndex(colLetters);

            String val = "0";
            if (refRowIndex >= 0 && refRowIndex < rows.size() && refColIndex >= 0 && refColIndex < getColCount()) {
                // Evitar referencia circular simple (misma celda que estamos llenando)
                if (refRowIndex == targetRow && refColIndex == fillingColumn) {
                    // política simple: tratar como 0 para evitar recursión infinita
                    val = "0";
                } else {
                    // Usamos evaluateCell para permitir referencias anidadas (si la celda referenciada es otra fórmula).
                    String ev = evaluateCell(refRowIndex, refColIndex);
                    if (ev == null || ev.isEmpty()) ev = "0";
                    // Si es numérico lo dejamos tal cual; si no, lo ponemos entre comillas para que JS pueda hacer comparaciones
                    if (ev.matches("-?\\d+(\\.\\d+)?")) {
                        val = ev;
                    } else {
                        val = "\"" + ev.replace("\"", "\\\"") + "\"";
                    }
                }
            } else {
                val = "0";
            }

            m.appendReplacement(sb, Matcher.quoteReplacement(val));
        }
        m.appendTail(sb);
        return sb.toString();
    }
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`````

}
