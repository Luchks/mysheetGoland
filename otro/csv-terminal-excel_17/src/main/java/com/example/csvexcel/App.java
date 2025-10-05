package com.example.csvexcel;
import java.util.HashMap;
import java.util.Map;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;

import java.util.Scanner;

public class App {

    public static void main(String[] args) throws Exception { // <-- throws Exception


        Sheet sheet = CsvReader.readCsv("sheet.csv");

        Terminal terminal = TerminalBuilder.builder()
                .system(true)
                .jna(true)
                .build();
        terminal.enterRawMode(); // modo raw: teclas leídas al instante

        Scanner sc = new Scanner(System.in);

        int curRow = 0;
        int curCol = 0;
        int topRow = 0;
        int leftCol = 0;
        int viewHeight = 29;
        int viewWidth = 9;
        int key;

        int prevKey = -1; // fuera del while principal, para detectar combinaciones
        Map<Character, int[]> marks = new HashMap<>();
                            
        while (true) {

            // Limpiar pantalla
            terminal.puts(InfoCmp.Capability.clear_screen);
            terminal.flush();

            // Ajustar scroll automático
            if (curRow < topRow) topRow = curRow;
            if (curRow >= topRow + viewHeight) topRow = curRow - viewHeight + 1;
            if (curCol < leftCol) leftCol = curCol;
            if (curCol >= leftCol + viewWidth) leftCol = curCol - viewWidth + 1;


            // 1. Calcular ancho máximo de cada columna visible
            int[] colWidths = new int[Math.min(viewWidth, sheet.getColCount() - leftCol)];
            for (int j = leftCol; j < Math.min(leftCol + viewWidth, sheet.getColCount()); j++) {
                int maxWidth = sheet.getColumnName(j).length();
                for (int i = topRow; i < Math.min(topRow + viewHeight, sheet.getRowCount()); i++) {
                    String val = sheet.evaluateCell(i, j);
                    if (val.length() > maxWidth) maxWidth = val.length();
                }
                colWidths[j - leftCol] = maxWidth + 2; // pequeño margen para separar columnas
            }

            // 2. Imprimir filas con colores y ancho fijo
            for (int i = topRow; i < Math.min(topRow + viewHeight, sheet.getRowCount()); i++) {

                String colorGris = "\033[90m";
                String colorReset = "\033[0m";

                System.out.printf(colorGris + "%-4s" + colorReset, (i + 1)); // número de fila alineado
                for (int j = leftCol; j < Math.min(leftCol + viewWidth, sheet.getColCount()); j++) {
                    String val = sheet.evaluateCell(i, j);

                    String reset = "\033[0m";
                    String bgBlue = "\033[44m";
                    String bgGray = "\033[100m";
                    String bgLight = "\033[47m";
                    String bgYellow = "\033[43m";

                    boolean colVacia = sheet.isColumnEmpty(j);
                    Character markLabel = null;
                    for (Map.Entry<Character, int[]> entry : marks.entrySet()) {
                        int[] pos = entry.getValue();
                        if (pos[0] == i && pos[1] == j) {
                            markLabel = entry.getKey();
                            break;
                        }
                    }

                    String cellText;
                    if (i == curRow && j == curCol) {
                        cellText = bgBlue + String.format("%-" + colWidths[j - leftCol] + "s", val) + reset;
                    } else if (markLabel != null) {
                        cellText = bgYellow + "[" + markLabel + "]" + val + reset;
                    } else if (colVacia) {
                        cellText = bgGray + String.format("%-" + colWidths[j - leftCol] + "s", val) + reset;
                    } else if (val.trim().isEmpty()) {
                        cellText = bgLight + String.format("%-" + colWidths[j - leftCol] + "s", " ") + reset;
                    } else {
                        cellText = String.format("%-" + colWidths[j - leftCol] + "s", val);
                    }

                    System.out.print(cellText);
                }
                System.out.println();
            }

            // 3. Imprimir encabezado de columnas alineado
            String AcolorReset = "\033[0m";
            String AcolorVerde = "\033[32m";
            String AcolorGris = "\033[90m";

            System.out.print("    "); // espacio para numeración de filas
            for (int j = leftCol; j < Math.min(leftCol + viewWidth, sheet.getColCount()); j++) {
                System.out.printf(AcolorGris + "%-" + colWidths[j - leftCol] + "s" + AcolorReset, sheet.getColumnName(j));
            }
            System.out.println();
       
            // Mostrar ventana visible

            int visibles = sheet.getRows().size();
            int totales = sheet.getTotalOriginalRows();

            String colorReset = "\033[0m";
            String colorVerde = "\033[32m";
            String colorGris = "\033[90m";

            String color = (visibles < totales) ? colorVerde : colorGris;

            System.out.println(color + "Filas visibles: " + visibles + " / " + totales + colorReset);
            //===============================================================================================================

            System.out.println("\nTeclas: h/j/k/l=Mover | e=Editar | s=Guardar | c/C=Agregar Columna | d=Eliminar Columna | f=Filtrar | r=Restaurar | q=Salir");

                key = terminal.reader().read();

                    // ----- Manejo de marcas tipo Vim -----
                    if (key == 'm') {
                        int markKey = terminal.reader().read();
                        if (Character.isLetter(markKey)) {
                            marks.put((char) markKey, new int[]{curRow, curCol});
                            terminal.writer().println("Marcada posición '" + (char) markKey + "'");
                            terminal.flush();
                            Thread.sleep(200);
                        }
                        continue;
                    }

                    if (key == '\'') {
                        int markKey = terminal.reader().read();
                        if (marks.containsKey((char) markKey)) {
                            int[] pos = marks.get((char) markKey);
                            curRow = pos[0];
                            curCol = pos[1];
                            terminal.writer().println("Saltando a marca '" + (char) markKey + "'");
                            terminal.flush();
                            Thread.sleep(200);
                        } else {
                            terminal.writer().println("No existe marca '" + (char) markKey + "'");
                            terminal.flush();
                            Thread.sleep(200);
                        }
                        continue;
                    }
            // Detectar combinaciones tipo gg
            if (prevKey == 'g' && key == 'g') {
                curRow = 0;
                prevKey = -1;
                continue;
            }

            switch (key) {
                case 'h': curCol = Math.max(0, curCol - 1); break;
                case 'l': curCol = Math.min(sheet.getColCount() - 1, curCol + 1); break;
                case 'k': curRow = Math.max(0, curRow - 1); break;
                case 'j': curRow = Math.min(sheet.getRowCount() - 1, curRow + 1); break;
                case '0': // Ir a la primera columna
                    curCol = 0;
                    break;
                case '$': // Ir a la última columna
                    curCol = sheet.getColCount() - 1;
                    break;
                case 'G': // Ir a la última fila
                    curRow = sheet.getRowCount() - 1;
                    break;
                case 21: // Ctrl+u → subir media pantalla
                    curRow = Math.max(0, curRow - viewHeight / 2);
                    break;
                case 4: // Ctrl+d → bajar media pantalla
                    curRow = Math.min(sheet.getRowCount() - 1, curRow + viewHeight / 2);
                    break;
                case 'e':
                    terminal.writer().print("Editar: ");
                    terminal.writer().flush();

                    StringBuilder inputVal = new StringBuilder();
                    while (true) {
                        key = terminal.reader().read();
                        
                        if (key == 10 || key == 13) break; // Enter
                        if (key == 127 && inputVal.length() > 0) { // Backspace
                            inputVal.deleteCharAt(inputVal.length() - 1);
                            terminal.writer().print("\b \b"); // borrar en pantalla
                        } else if (key >= 32 && key <= 126) { // caracteres imprimibles
                            inputVal.append((char) key);
                            terminal.writer().print((char) key);
                        }
                        terminal.flush();
                    }

                    sheet.setCell(curRow, curCol, inputVal.toString());
                    terminal.writer().println();
                    terminal.flush();
                    break;
                case 's':
                    CsvReader.writeCsv(sheet, "sheet.csv");
                    terminal.writer().println("Guardado! Presiona Enter...");
                    terminal.writer().flush();
                    break;

                case 'C': // agregar columna al final
                    sheet.addColumn();
                    curCol = sheet.getColCount() - 1; // mover cursor a la nueva columna
                    terminal.writer().println("Columna agregada!");
                    terminal.flush();
                    Thread.sleep(300);
                    break;

                case 'c': // agregar columna antes de la actual
                    sheet.addColumnAt(curCol);
                    terminal.writer().println("Columna agregada al final!");
                    terminal.flush();
                    Thread.sleep(300);
                    break;
                case 'y': // duplicar columna actual (como "yank" en Vim)
                    sheet.duplicateColumnAt(curCol);
                    curCol++; // mover cursor a la nueva columna duplicada
                    break;
                case 'd': // eliminar columna actual
                    sheet.removeColumnAt(curCol);
                    if (curCol >= sheet.getColCount()) curCol = sheet.getColCount() - 1; // ajustar cursor
                    if (curCol < 0) curCol = 0;
                    break;
                
                case 'f': // filtrar por columna actual
                    terminal.writer().print("Filtrar columna " + curCol + " (ej: >30, ==Peru): ");
                    terminal.writer().flush();

                    StringBuilder filterInput = new StringBuilder();
                    while (true) {
                        key = terminal.reader().read();
                        if (key == 10 || key == 13) break;
                        if (key == 127 && filterInput.length() > 0) {
                            filterInput.deleteCharAt(filterInput.length() - 1);
                            terminal.writer().print("\b \b");
                        } else if (key >= 32 && key <= 126) {
                            filterInput.append((char) key);
                            terminal.writer().print((char) key);
                        }
                        terminal.flush();
                    }
                    terminal.writer().println();
                    terminal.flush();

                    sheet.filterByColumn(curCol, filterInput.toString());
                    curRow = 0;
                    terminal.writer().println("Filtro aplicado. Presiona cualquier tecla para continuar...");
                    terminal.writer().flush();
                    terminal.reader().read();
                    break;
                case 'r': // restaurar filtro
                    sheet.clearFilter();
                    curRow = 0;
                    terminal.writer().println("Filtro eliminado.");
                    terminal.flush();
                    Thread.sleep(300);
                    break;
                case 'q':
                    terminal.close();
                    return;
            }
            prevKey = key;
        }
    }
}
