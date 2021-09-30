// Package main implements a simple CLI to initialize the SQLite database.
package main

import (
	"database/sql"
	"flag"
	_ "github.com/mattn/go-sqlite3"
	"os"
	be1_go "popstellar"
)

func main() {
	var dbPath string
	var schemaPath string
	var force bool

	log := be1_go.Logger

	flag.StringVar(&dbPath, "db", "pop_go_hub.db", "location of the database file")
	flag.StringVar(&schemaPath, "schema", "schema.sql", "location of the SQL schema")
	flag.BoolVar(&force, "f", false, "if true removes the existing db")

	flag.Parse()

	_, err := os.Stat(dbPath)
	if err == nil && !force {
		log.Err(err).Msg("db already exists, use -f if you want to overwrite it")
		return
	}

	os.Remove(dbPath)

	log.Info().Msg("opening db")

	db, err := sql.Open("sqlite3", dbPath)
	if err != nil {
		log.Err(err).Msg("failed to open db")
	}

	defer db.Close()

	log.Info().Msg("reading schema")

	schema, err := os.ReadFile(schemaPath)
	if err != nil {
		log.Err(err).Msg("failed to read schema")
	}

	log.Info().Msg("setting up db")

	_, err = db.Exec(string(schema))
	if err != nil {
		log.Err(err).Msg("failed to exec schema")
	}

	log.Info().Msgf("db successfully saved in %s", dbPath)
}
