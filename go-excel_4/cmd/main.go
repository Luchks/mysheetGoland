package main

import (
	"fmt"
	"os"

	"github.com/luchks/go-excel/internal/domain"
	"github.com/luchks/go-excel/internal/interfaces/terminal"
	"github.com/luchks/go-excel/internal/usecases"

	tea "github.com/charmbracelet/bubbletea"
)

func main() {
	sheet := domain.NewSheet(10, 5) // puedes ajustar filas y columnas

	// Intentar cargar un CSV si se pasa como argumento
	if len(os.Args) > 1 {
		err := usecases.LoadCSV(sheet, os.Args[1])
		if err != nil {
			fmt.Println("Error loading CSV:", err)
		}
	}

	p := tea.NewProgram(terminal.NewModel(sheet))

	if err := p.Start(); err != nil {
		fmt.Println("Error starting program:", err)
		os.Exit(1)
	}
}
