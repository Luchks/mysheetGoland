
// internal/usecases/navigation.go
package usecases

import "github.com/luchks/go-excel/internal/domain"

func MoveUp(sheet *domain.Sheet) {
	if sheet.CurRow > 0 {
		sheet.CurRow--
	}
}

func MoveDown(sheet *domain.Sheet) {
	if sheet.CurRow < sheet.Rows-1 {
		sheet.CurRow++
	}
}

func MoveLeft(sheet *domain.Sheet) {
	if sheet.CurCol > 0 {
		sheet.CurCol--
	}
}

func MoveRight(sheet *domain.Sheet) {
	if sheet.CurCol < sheet.Cols-1 {
		sheet.CurCol++
	}
}
