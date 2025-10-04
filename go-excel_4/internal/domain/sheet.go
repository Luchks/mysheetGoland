
// internal/domain/sheet.go
package domain

type Sheet struct {
	Cells       [][]Cell
	CurRow      int
	CurCol      int
	Rows, Cols  int
}

func NewSheet(rows, cols int) *Sheet {
	cells := make([][]Cell, rows)
	for i := range cells {
		cells[i] = make([]Cell, cols)
	}
	return &Sheet{
		Cells: cells,
		Rows:  rows,
		Cols:  cols,
	}
}
