package db

// Just global functions used for every databases. Write is currently not used but we keep it as a syntactic guide.

import (
	"github.com/boltdb/bolt"
	"log"
	"student20_pop/lib"
)

func OpenDB(dbName string) (*bolt.DB, error) {
	db, err := bolt.Open(dbName, 0600, nil)
	if err != nil {
		log.Printf("could not open database: %v", err)
		return nil, lib.ErrDBFault
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
