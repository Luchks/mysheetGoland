package terminal

import (
	"fmt"
	"os"
	"strings"
	"syscall"
	"unsafe"

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

	// 1️⃣ Calculamos ancho máximo de cada columna según contenido
	colWidths := make([]int, m.Sheet.Cols)
	for _, row := range m.Sheet.Cells {
		for j, cell := range row {
			if len(cell.Value) > colWidths[j] {
				colWidths[j] = len(cell.Value)
			}
		}
	}

	// Ajustamos ancho de los encabezados (A, B, C...)
	for j := 0; j < m.Sheet.Cols; j++ {
		header := string('A' + j)
		if len(header) > colWidths[j] {
			colWidths[j] = len(header)
		}
	}

	// 2️⃣ Obtenemos ancho de la terminal
	termWidth, _, err := terminalSize()
	if err != nil {
		termWidth = 80 // ancho por defecto si falla
	}

	// 3️⃣ Ajustamos columnas si exceden ancho de terminal
	totalWidth := 5 // espacio para número de fila y bordes
	for _, w := range colWidths {
		totalWidth += w + 3 // ancho columna + espacio interno + borde
	}

	if totalWidth > termWidth {
		scale := float64(termWidth-5) / float64(totalWidth-5)
		for i, w := range colWidths {
			newW := int(float64(w)*scale) - 1
			if newW < 1 {
				newW = 1
			}
			colWidths[i] = newW
		}
	}

	// 4️⃣ Función para dibujar línea separadora
	drawSeparator := func() {
		sb.WriteString("+----")
		for _, w := range colWidths {
			sb.WriteString("+")
			sb.WriteString(strings.Repeat("-", w+2))
		}
		sb.WriteString("+\n")
	}

	drawSeparator()

	// 5️⃣ Encabezados de columna
	sb.WriteString("|    |")
	for j := 0; j < m.Sheet.Cols; j++ {
		header := string('A' + j)
		sb.WriteString(fmt.Sprintf(" %-*s |", colWidths[j], header))
	}
	sb.WriteString("\n")
	drawSeparator()

	// 6️⃣ Filas de la hoja
	for i, row := range m.Sheet.Cells {
		sb.WriteString(fmt.Sprintf("| %2d |", i+1))
		for j, cell := range row {
			cellContent := cell.Value
			if i == m.Sheet.CurRow && j == m.Sheet.CurCol {
				cellContent = "[" + cellContent + "]"
			}
			// Recortamos si excede ancho permitido
			if len(cellContent) > colWidths[j] {
				cellContent = cellContent[:colWidths[j]]
			}
			sb.WriteString(fmt.Sprintf(" %-*s |", colWidths[j], cellContent))
		}
		sb.WriteString("\n")
		drawSeparator()
	}

	// 7️⃣ Modo e input
	sb.WriteString(fmt.Sprintf("\nMode: %v  Input: %s", m.Mode, m.Input))
	return sb.String()
}

// terminalSize obtiene ancho y alto de la terminal
func terminalSize() (width, height int, err error) {
	type winsize struct {
		Row, Col       uint16
		Xpixel, Ypixel uint16
	}
	ws := &winsize{}
	retCode, _, errno := syscall.Syscall(syscall.SYS_IOCTL,
		uintptr(os.Stdout.Fd()),
		uintptr(syscall.TIOCGWINSZ),
		uintptr(unsafe.Pointer(ws)),
	)
	if int(retCode) == -1 {
		return 0, 0, errno
	}
	return int(ws.Col), int(ws.Row), nil
}
