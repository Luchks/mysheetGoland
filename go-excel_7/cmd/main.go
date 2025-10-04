package main

import (
	"fmt"
	"log"
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

	// 1️⃣ Creamos hoja vacía con tamaño dinámico
	sheet := domain.NewSheet(50, 20) // 50 filas, 20 columnas

	// 2️⃣ Cargamos CSV (si existe)
	if err := usecases.LoadCSV(sheet, path); err != nil {
		fmt.Println("No se pudo cargar CSV:", err)
	}

	// 3️⃣ Creamos modelo Bubble Tea
	model := terminal.NewModel(sheet)

	// 4️⃣ Ejecutamos TUI
	p := tea.NewProgram(model)
	if err := p.Start(); err != nil {
		log.Fatal(err)
	}
}
