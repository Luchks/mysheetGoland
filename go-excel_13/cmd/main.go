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
	if len(os.Args) < 2 {
		fmt.Println("Uso: go run ./cmd/main.go <archivo.csv>")
		return
	}

	path := os.Args[1]

	sheet := domain.NewSheet(20, 10)

	// Cargar CSV si existe
	if _, err := os.Stat(path); err == nil {
		if err := usecases.LoadCSV(sheet, path); err != nil {
			fmt.Println("Error al cargar CSV:", err)
			return
		}
	}

	m := terminal.NewModel(sheet)
	p := tea.NewProgram(m) // ✅ Pasamos puntero
	if err := p.Start(); err != nil {
		fmt.Println("Error en la terminal:", err)
		os.Exit(1)
	}

	if err := usecases.SaveCSV(sheet, path); err != nil {
		fmt.Println("Error al guardar CSV:", err)
	} else {
		fmt.Println("Archivo guardado en", path)
	}
}
