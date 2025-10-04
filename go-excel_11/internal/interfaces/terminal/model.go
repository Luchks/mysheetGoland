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

func (m *Model) Init() tea.Cmd { return nil }

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

// View dibuja la hoja con scroll dinámico
func (m *Model) View() string {
	var sb strings.Builder

	colWidths := computeColWidths(m.Sheet)
	termWidth, termHeight, _ := terminalSize()
	visibleCols := calculateVisibleCols(m.Sheet.Cols, m.ColOffset, termWidth, colWidths)
	visibleRows := termHeight - 5
	if visibleRows > m.Sheet.Rows {
		visibleRows = m.Sheet.Rows
	}

	// Encabezados
	sb.WriteString("     ")
	for j := m.ColOffset; j < m.ColOffset+visibleCols && j < m.Sheet.Cols; j++ {
		sb.WriteString(fmt.Sprintf(" %-*s ", colWidths[j], string('A'+j)))
	}
	sb.WriteString("\n")

	for i := m.RowOffset; i < m.RowOffset+visibleRows && i < m.Sheet.Rows; i++ {
		sb.WriteString(fmt.Sprintf("%4d ", i+1))
		for j := m.ColOffset; j < m.ColOffset+visibleCols && j < m.Sheet.Cols; j++ {
			cell := m.Sheet.Cells[i][j].Value
			if i == m.Sheet.CurRow && j == m.Sheet.CurCol {
				cell = "[" + cell + "]"
			}
			if len(cell) > colWidths[j] {
				cell = cell[:colWidths[j]]
			}
			sb.WriteString(fmt.Sprintf(" %-*s ", colWidths[j], cell))
		}
		sb.WriteString("\n")
	}

	sb.WriteString(fmt.Sprintf("\nMode: %v Input: %s\n", m.Mode, m.Input))
	return sb.String()
}

// -------------------- Helpers --------------------

func adjustScrollForCursor(m *Model) {
	termWidth, termHeight, _ := terminalSize()
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

func computeColWidths(s *domain.Sheet) []int {
	widths := make([]int, s.Cols)
	for j := 0; j < s.Cols; j++ {
		w := 1
		for i := 0; i < s.Rows; i++ {
			if len(s.Cells[i][j].Value) > w {
				w = len(s.Cells[i][j].Value)
			}
		}
		widths[j] = w
	}
	return widths
}

func calculateVisibleCols(totalCols, offset, termWidth int, colWidths []int) int {
	w := 5 // espacio para número de fila
	cols := 0
	for j := offset; j < totalCols; j++ {
		if w+colWidths[j]+1 > termWidth {
			break
		}
		w += colWidths[j] + 1
		cols++
	}
	return cols
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
	if int(retCode) == -1 {
		return 80, 25, errno
	}
	return int(ws.Col), int(ws.Row), nil
}
