package method

import "path/filepath"

var relativeExamplePath string

func init() {
	relativeExamplePath = filepath.Join("..", "..", "..", "..", "..", "..", "protocol",
		"examples", "query")
}
