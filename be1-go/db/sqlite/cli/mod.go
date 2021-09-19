// Package main implements a simple CLI to initialize the SQLite database.
package main

import (
	"database/sql"
	"flag"
	"log"
	"os"

	_ "github.com/mattn/go-sqlite3"
)

func main() {
	var dbPath string
	var schemaPath string
	var force bool

	flag.StringVar(&dbPath, "db", "pop_go_hub.db", "location of the database file")
	flag.StringVar(&schemaPath, "schema", "schema.sql", "location of the SQL schema")
	flag.BoolVar(&force, "f", false, "if true removes the existing db")

	flag.Parse()

	_, err := os.Stat(dbPath)
	if err == nil && !force {
		log.Printf("db already exists, use -f if you want to overwrite it")
		return
	}

	os.Remove(dbPath)

	log.Println("opening db")

	db, err := sql.Open("sqlite3", dbPath)
	if err != nil {
		log.Fatalf("failed to open db: %v", err)
	}

	defer db.Close()

	log.Println("reading schema")

	schema, err := os.ReadFile(schemaPath)
	if err != nil {
		log.Fatalf("failed to read schema: %v", err)
	}

	log.Println("setting up db")

	_, err = db.Exec(string(schema))
	if err != nil {
		log.Fatalf("failed to exec schema: %v", err)
	}

	log.Printf("done, db saved in %s", dbPath)
}
