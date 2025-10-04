
// internal/interfaces/terminal/keybindings.go
package terminal

type KeyAction func()

type KeyMap struct {
    H KeyAction
    J KeyAction
    K KeyAction
    L KeyAction
    I KeyAction
    Esc KeyAction
    Save KeyAction
    Quit KeyAction
}
