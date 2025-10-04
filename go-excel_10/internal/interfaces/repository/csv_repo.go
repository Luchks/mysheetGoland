
// cmd/main.go
package main

import (
	"fmt"
	"github.com/luchks/go-excel/internal/domain"
	"github.com/luchks/go-excel/internal/interfaces/terminal"
	"os"

	"github.com/charmbracelet/bubbletea"
)

func main() {
	sheet := domain.NewSheet(10, 5)

	p := tea.NewProgram(terminal.NewModel(sheet))

	if err := p.Start(); err != nil {
		fmt.Println("Error starting program:", err)
		os.Exit(1)
	}
}
