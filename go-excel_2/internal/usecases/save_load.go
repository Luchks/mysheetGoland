
// internal/usecases/save_load.go
package usecases


import (
    "encoding/csv"
    "os"
    "github.com/luchks/go-excel/internal/domain"
)


func SaveCSV(sheet *domain.Sheet, path string) error {
	file, err := os.Create(path)
	if err != nil {
		return err
	}
	defer file.Close()

	writer := csv.NewWriter(file)
	defer writer.Flush()

	for _, row := range sheet.Cells {
		record := []string{}
		for _, cell := range row {
			record = append(record, cell.Value)
		}
		writer.Write(record)
	}
	return nil
}

func LoadCSV(sheet *domain.Sheet, path string) error {
	file, err := os.Open(path)
	if err != nil {
		return err
	}
	defer file.Close()

	reader := csv.NewReader(file)
	rows, err := reader.ReadAll()
	if err != nil {
		return err
	}

	for i, r := range rows {
		if i >= sheet.Rows {
			break
		}
		for j, val := range r {
			if j >= sheet.Cols {
				break
			}
			sheet.Cells[i][j].Value = val
		}
	}
	return nil
}
