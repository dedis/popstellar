package sqlite

// register the driver
import (
	"os"

	_ "github.com/mattn/go-sqlite3"
)

// Interactions with the DB will be moved here once we can use a storage
// interface in the implementation. In the meantime the code is directly in
// base_hub.go and inbox.go.

// databaseKeyPath is the name of the environment variable containing the
// database path. This environment variable can be retrieved with:
//   os.Getenv(databaseKeyPath)
// This env variable can be set when using the CLI like so:
//   $ HUB_DB=my/path/dbfile.db ./pop serve
const databaseKeyPath = "HUB_DB"

// GetDBPath returns the path of the database file using the environment
// variable. Returns an empty string if not found.
func GetDBPath() string {
	return os.Getenv(databaseKeyPath)
}
