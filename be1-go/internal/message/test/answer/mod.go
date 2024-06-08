package answer

import "path/filepath"

var relativeExamplePath string

func init() {
	relativeExamplePath = filepath.Join("..", "..", "..", "..", "..", "protocol",
		"examples", "answer")
}
