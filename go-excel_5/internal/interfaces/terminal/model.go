package terminal

import (
	"fmt"
	"strings"

	"github.com/luchks/go-excel/internal/domain"
	"github.com/luchks/go-excel/internal/usecases"

	tea "github.com/charmbracelet/bubbletea"
)

type Mode int

const (
	Normal Mode = iota
	Insert
)

type Model struct {
	Sheet *domain.Sheet
	Mode  Mode
	Input string
}

func NewModel(sheet *domain.Sheet) Model {
	return Model{Sheet: sheet, Mode: Normal}
}

func (m Model) Init() tea.Cmd {
	return nil
}

func (m Model) Update(msg tea.Msg) (tea.Model, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.KeyMsg:
		switch m.Mode {
		case Normal:
			switch msg.String() {
			case "h":
				usecases.MoveLeft(m.Sheet)
			case "j":
				usecases.MoveDown(m.Sheet)
			case "k":
				usecases.MoveUp(m.Sheet)
			case "l":
				usecases.MoveRight(m.Sheet)
			case "i":
				m.Mode = Insert
			case "x":
				usecases.DeleteCell(m.Sheet, m.Sheet.CurRow, m.Sheet.CurCol)
			case "s":
				usecases.SaveCSV(m.Sheet, "sheet.csv")
			case "q":
				return m, tea.Quit
			}
		case Insert:
			switch msg.String() {
			case "esc":
				usecases.InsertValue(m.Sheet, m.Sheet.CurRow, m.Sheet.CurCol, m.Input)
				m.Input = ""
				m.Mode = Normal
			default:
				m.Input += msg.String()
			}
		}
	}
	return m, nil
}

func (m Model) View() string {
	var sb strings.Builder

	// 1️⃣ Calculamos ancho máximo de cada columna
	colWidths := make([]int, m.Sheet.Cols)
	for _, row := range m.Sheet.Cells {
		for j, cell := range row {
			if len(cell.Value) > colWidths[j] {
				colWidths[j] = len(cell.Value)
			}
		}
	}

	// Aseguramos que los encabezados tengan al menos 2 caracteres de ancho
	for j := 0; j < m.Sheet.Cols; j++ {
		header := string('A' + j)
		if len(header) > colWidths[j] {
			colWidths[j] = len(header)
		}
	}

	// 2️⃣ Función para dibujar línea separadora
	drawSeparator := func() {
		sb.WriteString("+----") // espacio para números de fila
		for _, w := range colWidths {
			sb.WriteString("+")
			sb.WriteString(strings.Repeat("-", w+2)) // +2 para espacio interno
		}
		sb.WriteString("+\n")
	}

	drawSeparator()

	// 3️⃣ Dibujar encabezados de columnas
	sb.WriteString("|    |") // espacio para números de fila
	for j := 0; j < m.Sheet.Cols; j++ {
		header := string('A' + j)
		sb.WriteString(fmt.Sprintf(" %-*s |", colWidths[j], header))
	}
	sb.WriteString("\n")
	drawSeparator()

	// 4️⃣ Dibujar filas con números y celdas
	for i, row := range m.Sheet.Cells {
		// Número de fila
		sb.WriteString(fmt.Sprintf("| %2d |", i+1))

		// Contenido de celdas
		for j, cell := range row {
			cellContent := cell.Value
			if i == m.Sheet.CurRow && j == m.Sheet.CurCol {
				cellContent = "[" + cellContent + "]"
			}
			sb.WriteString(fmt.Sprintf(" %-*s |", colWidths[j], cellContent))
		}
		sb.WriteString("\n")
		drawSeparator()
	}

	// 5️⃣ Mostrar modo e input
	sb.WriteString(fmt.Sprintf("\nMode: %v  Input: %s", m.Mode, m.Input))
	return sb.String()
}
