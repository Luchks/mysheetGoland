package domain

type Sheet struct {
	Cells  [][]Cell
	Rows   int
	Cols   int
	CurRow int
	CurCol int
}

func NewSheet(rows, cols int) *Sheet {
	cells := make([][]Cell, rows)
	for i := 0; i < rows; i++ {
		cells[i] = make([]Cell, cols)
	}
	return &Sheet{
		Cells: cells,
		Rows:  rows,
		Cols:  cols,
	}
}
