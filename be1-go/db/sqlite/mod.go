package sqlite

// register the driver
import (
	_ "github.com/mattn/go-sqlite3"
)

// Interactions with the DB will be moved here once we can use a storage
// interface in the implementation. In the meantime the code is directly in
// base_hub.go and inbox.go.
