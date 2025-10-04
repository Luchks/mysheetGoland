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
	for i, row := range m.Sheet.Cells {
		for j, cell := range row {
			if i == m.Sheet.CurRow && j == m.Sheet.CurCol {
				sb.WriteString(fmt.Sprintf("[%s]\t", cell.Value))
			} else {
				sb.WriteString(fmt.Sprintf(" %s \t", cell.Value))
			}
		}
		sb.WriteString("\n")
	}
	sb.WriteString(fmt.Sprintf("\nMode: %v  Input: %s", m.Mode, m.Input))
	return sb.String()
}
