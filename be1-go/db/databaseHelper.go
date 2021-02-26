package db

// Just global functions used for every databases. Write is currently not used but we keep it as a syntactic guide.

import (
	"github.com/boltdb/bolt"
	"golang.org/x/xerrors"
)

// OpenDB opens a key-value store backed by boltdb.
func OpenDB(dbName string) (*bolt.DB, error) {
	// This blocks if the DB is already open. IMHO we should call `bolt.Open`
	// once and use the same instance throughout.
	db, err := bolt.Open(dbName, 0600, nil)
	if err != nil {
		return nil, xerrors.Errorf("failed to open db: %v", err)
	}
	return db, nil
}

// Write is a function that writes a pair (key, val) in the bucket "bkt" in the database
// DEPRECATED : we only keep it as syntactic reminder
// commented out to improve test coverage
/*
func Write(key []byte, val []byte, bkt []byte, database *bolt.DB) error {
	err := database.Update(func(tx *bolt.Tx) error {
		b, err1 := tx.CreateBucketIfNotExists(bkt)
		if err1 != nil {
			return err1
		}
		err2 := b.Put(key, val)
		return err2

	})
	return err
}*/
