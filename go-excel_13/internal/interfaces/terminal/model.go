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

func (m *Model) Init() tea.Cmd {
	return nil
}

func (m *Model) Update(msg tea.Msg) (tea.Model, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.KeyMsg:
		switch m.Mode {
		case Normal:
			switch msg.String() {
			case "h": usecases.MoveLeft(m.Sheet)
			case "l": usecases.MoveRight(m.Sheet)
			case "k": usecases.MoveUp(m.Sheet)
			case "j": usecases.MoveDown(m.Sheet)
			case "i": m.Mode = Insert
			case "x": usecases.DeleteCell(m.Sheet, m.Sheet.CurRow, m.Sheet.CurCol)
			case "s": usecases.SaveCSV(m.Sheet, "sheet.csv")
			case "q": return m, tea.Quit

			case "H": if m.ColOffset > 0 { m.ColOffset-- } // scroll horiz izquierda
			case "L": if m.ColOffset < m.Sheet.Cols-1 { m.ColOffset++ } // scroll horiz derecha

			case "pgup": 
				_, h, _ := terminalSize()
				m.RowOffset -= h - 5
				if m.RowOffset < 0 { m.RowOffset = 0 }

			case "pgdn": 
				_, h, _ := terminalSize()
				m.RowOffset += h - 5
				if m.RowOffset > m.Sheet.Rows - (h - 5) { m.RowOffset = m.Sheet.Rows - (h - 5) }
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

func (m *Model) View() string {
	var sb strings.Builder
	colWidths := computeColWidths(m.Sheet)
	termWidth, termHeight, _ := terminalSize()
	visibleRows := termHeight - 5

	sb.WriteString(drawSeparator(m, colWidths, termWidth))

	// encabezados
	sb.WriteString("|    |")
	for j := m.ColOffset; j < m.Sheet.Cols; j++ {
		sb.WriteString(fmt.Sprintf(" %-*s |", colWidths[j], string('A'+j)))
	}
	sb.WriteString("\n")
	sb.WriteString(drawSeparator(m, colWidths, termWidth))

	// filas
	for i := m.RowOffset; i < m.Sheet.Rows && i < m.RowOffset+visibleRows; i++ {
		sb.WriteString(fmt.Sprintf("| %2d |", i+1))
		for j := m.ColOffset; j < m.Sheet.Cols; j++ {
			cellContent := m.Sheet.Cells[i][j].Value
			if i == m.Sheet.CurRow && j == m.Sheet.CurCol {
				cellContent = "[" + cellContent + "]"
			}
			sb.WriteString(fmt.Sprintf(" %-*s |", colWidths[j], cellContent))
		}
		sb.WriteString("\n")
		sb.WriteString(drawSeparator(m, colWidths, termWidth))
	}

	sb.WriteString(fmt.Sprintf("\nMode: %v  Input: %s", m.Mode, m.Input))
	return sb.String()
}

// -------- FUNCIONES AUXILIARES --------

func computeColWidths(sheet *domain.Sheet) []int {
	colWidths := make([]int, sheet.Cols)
	for _, row := range sheet.Cells {
		for j, cell := range row {
			if len(cell.Value) > colWidths[j] {
				colWidths[j] = len(cell.Value)
			}
		}
	}
	for j := 0; j < sheet.Cols; j++ {
		h := string('A' + j)
		if len(h) > colWidths[j] {
			colWidths[j] = len(h)
		}
	}
	return colWidths
}

func drawSeparator(m *Model, colWidths []int, termWidth int) string {
	var sb strings.Builder
	sb.WriteString("+----")
	for j := m.ColOffset; j < m.Sheet.Cols; j++ {
		sb.WriteString("+")
		sb.WriteString(strings.Repeat("-", colWidths[j]+2))
	}
	sb.WriteString("+\n")
	return sb.String()
}

func adjustScrollForCursor(m *Model) {
	termWidth, termHeight, _ := terminalSize()
	visibleRows := termHeight - 5
	colWidths := computeColWidths(m.Sheet)

	// scroll vertical
	if m.Sheet.CurRow < m.RowOffset { m.RowOffset = m.Sheet.CurRow }
	if m.Sheet.CurRow >= m.RowOffset+visibleRows { m.RowOffset = m.Sheet.CurRow - visibleRows + 1 }

	// scroll horizontal
	totalWidth := 5
	visibleCols := 0
	for j := m.ColOffset; j < m.Sheet.Cols; j++ {
		if totalWidth+colWidths[j]+3 > termWidth { break }
		totalWidth += colWidths[j] + 3
		visibleCols++
	}

	if m.Sheet.CurCol < m.ColOffset { m.ColOffset = m.Sheet.CurCol }
	if m.Sheet.CurCol >= m.ColOffset+visibleCols { m.ColOffset = m.Sheet.CurCol - visibleCols + 1 }
}

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
	if int(retCode) == -1 { return 0, 0, errno }
	return int(ws.Col), int(ws.Row), nil
}
