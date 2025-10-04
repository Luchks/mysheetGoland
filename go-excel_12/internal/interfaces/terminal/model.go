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
	Sheet     *domain.Sheet
	Mode      Mode
	Input     string
	RowOffset int
	ColOffset int
}

func NewModel(sheet *domain.Sheet) *Model {
	return &Model{Sheet: sheet, Mode: Normal}
}

// Inicialización requerida por Bubble Tea
func (m *Model) Init() tea.Cmd {
	return nil
}

// Actualización de eventos
func (m *Model) Update(msg tea.Msg) (tea.Model, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.KeyMsg:
		switch m.Mode {
		case Normal:
			switch msg.String() {
			case "h":
				usecases.MoveLeft(m.Sheet)
			case "l":
				usecases.MoveRight(m.Sheet)
			case "k":
				usecases.MoveUp(m.Sheet)
			case "j":
				usecases.MoveDown(m.Sheet)
			case "i":
				m.Mode = Insert
			case "x":
				usecases.DeleteCell(m.Sheet, m.Sheet.CurRow, m.Sheet.CurCol)
			case "s":
				usecases.SaveCSV(m.Sheet, "sheet.csv")
			case "q":
				return m, tea.Quit
			case "H": // scroll horizontal izquierda
				if m.ColOffset > 0 {
					m.ColOffset--
				}
			case "L": // scroll horizontal derecha
				if m.ColOffset < m.Sheet.Cols-1 {
					m.ColOffset++
				}
			case "pgup":
				if m.RowOffset > 0 {
					termHeight := getTermHeight()
					m.RowOffset -= termHeight - 5
					if m.RowOffset < 0 {
						m.RowOffset = 0
					}
				}
			case "pgdn":
				termHeight := getTermHeight()
				if m.RowOffset < m.Sheet.Rows-1 {
					m.RowOffset += termHeight - 5
					if m.RowOffset > m.Sheet.Rows-1 {
						m.RowOffset = m.Sheet.Rows - 1
					}
				}
			}
			adjustScrollForCursor(m)

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

// Ajusta scroll según posición del cursor
func adjustScrollForCursor(m *Model) {
	termWidth, termHeight := getTermWidth(), getTermHeight()
	colWidths := computeColWidths(m.Sheet)
	visibleCols := calculateVisibleCols(m.Sheet.Cols, m.ColOffset, termWidth, colWidths)
	visibleRows := termHeight - 5

	if m.Sheet.CurRow < m.RowOffset {
		m.RowOffset = m.Sheet.CurRow
	} else if m.Sheet.CurRow >= m.RowOffset+visibleRows {
		m.RowOffset = m.Sheet.CurRow - visibleRows + 1
	}

	if m.Sheet.CurCol < m.ColOffset {
		m.ColOffset = m.Sheet.CurCol
	} else if m.Sheet.CurCol >= m.ColOffset+visibleCols {
		m.ColOffset = m.Sheet.CurCol - visibleCols + 1
	}
}

// Renderizado
func (m *Model) View() string {
	var sb strings.Builder

	colWidths := computeColWidths(m.Sheet)
	termWidth, termHeight := getTermWidth(), getTermHeight()
	visibleCols := calculateVisibleCols(m.Sheet.Cols, m.ColOffset, termWidth, colWidths)
	visibleRows := termHeight - 5

	// Encabezado
	sb.WriteString("     ")
	for j := m.ColOffset; j < m.ColOffset+visibleCols && j < m.Sheet.Cols; j++ {
		header := string('A' + j)
		sb.WriteString(fmt.Sprintf("%-*s ", colWidths[j]+2, header))
	}
	sb.WriteString("\n")

	// Filas
	for i := m.RowOffset; i < m.RowOffset+visibleRows && i < m.Sheet.Rows; i++ {
		sb.WriteString(fmt.Sprintf("%3d |", i+1))
		for j := m.ColOffset; j < m.ColOffset+visibleCols && j < m.Sheet.Cols; j++ {
			cellContent := m.Sheet.Cells[i][j].Value
			if i == m.Sheet.CurRow && j == m.Sheet.CurCol {
				cellContent = "[" + cellContent + "]"
			}
			sb.WriteString(fmt.Sprintf("%-*s ", colWidths[j]+2, cellContent))
		}
		sb.WriteString("\n")
	}

	sb.WriteString(fmt.Sprintf("\nMode: %v  Input: %s", m.Mode, m.Input))
	return sb.String()
}

// Calcula ancho de cada columna
func computeColWidths(sheet *domain.Sheet) []int {
	widths := make([]int, sheet.Cols)
	for _, row := range sheet.Cells {
		for j, cell := range row {
			if len(cell.Value) > widths[j] {
				widths[j] = len(cell.Value)
			}
		}
	}
	// Asegurar ancho mínimo 1
	for j := range widths {
		if widths[j] < 1 {
			widths[j] = 1
		}
	}
	return widths
}

// Calcula cuántas columnas caben en pantalla
func calculateVisibleCols(totalCols, offset, termWidth int, colWidths []int) int {
	count := 0
	w := 0
	for i := offset; i < totalCols; i++ {
		w += colWidths[i] + 3
		if w+5 > termWidth {
			break
		}
		count++
	}
	return count
}

// Obtener tamaño de terminal
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

func getTermWidth() int {
	w, _, _ := terminalSize()
	return w
}

func getTermHeight() int {
	_, h, _ := terminalSize()
	return h
}
