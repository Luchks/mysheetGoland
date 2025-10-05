# mini_excel_curses.R
library(CursesR)
library(readr)

# Leer CSV
df <- read_csv("people.csv")
nrows <- nrow(df)
ncols <- ncol(df)

# Variables de posición
row <- 1
col <- 1

# Iniciar pantalla de curses
screen <- initscr()
cbreak(screen)
noecho(screen)
keypad(screen, TRUE)

draw_table <- function() {
  clear(screen)
  for (r in 1:nrows) {
    line <- ""
    for (c in 1:ncols) {
      cell <- as.character(df[r, c])
      # Resaltar celda seleccionada
      if (r == row && c == col) {
        line <- paste0(line, "[", cell, "]\t")
      } else {
        line <- paste0(line, " ", cell, " \t")
      }
    }
    mvaddstr(screen, r-1, 0, line)
  }
  refresh(screen)
}

draw_table()

repeat {
  k <- getch(screen)
  
  if (k == 113) {  # q para salir
    break
  } else if (k == KEY_UP) {
    row <- max(1, row-1)
  } else if (k == KEY_DOWN) {
    row <- min(nrows, row+1)
  } else if (k == KEY_LEFT) {
    col <- max(1, col-1)
  } else if (k == KEY_RIGHT) {
    col <- min(ncols, col+1)
  } else if (k == 10) {  # Enter para editar
    mvaddstr(screen, nrows+1, 0, "Nuevo valor: ")
    refresh(screen)
    new_val <- scan(what=character(), nmax=1, quiet=TRUE)
    df[row, col] <- new_val
  }
  
  draw_table()
}

# Salir y guardar cambios
endwin(screen)
write_csv(df, "people_editado.csv")
cat("Cambios guardados en people_editado.csv\n")
