
// internal/usecases/edit.go
package usecases

import "github.com/luchks/go-excel/internal/domain"

func InsertValue(sheet *domain.Sheet, row, col int, value string) {
	if row < sheet.Rows && col < sheet.Cols {
		sheet.Cells[row][col].Value = value
	}
}

func DeleteCell(sheet *domain.Sheet, row, col int) {
	if row < sheet.Rows && col < sheet.Cols {
		sheet.Cells[row][col] = domain.Cell{}
	}
}
