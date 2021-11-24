// Package be1_go defines the logger.
//
// This logger is inspired by the DEDIS Ledger Architecture global logger.
// (https://github.com/dedis/dela/blob/master/mod.go)
//
// be1-go is using a global logger with some default parameters. It is enabled by
// default at information level. The level can be modified using an environment variable:
//
//   LLVL=error ./pop organizer --pk "<base64url encoded pk>" serve
//   LLVL=warn ./pop organizer --pk "<base64url encoded pk>" serve
//
package be1_go

import (
	"os"
	"time"

	"github.com/rs/zerolog"
)

// EnvLogLevel is the name of the environment variable to change the logging
// level.
const EnvLogLevel = "LLVL"

const defaultLevel = zerolog.InfoLevel

func init() {
	lvl := os.Getenv(EnvLogLevel)

	var level zerolog.Level

	switch lvl {
	case "error":
		level = zerolog.ErrorLevel
	case "warn":
		level = zerolog.WarnLevel
	case "info":
		level = zerolog.InfoLevel
	case "debug":
		level = zerolog.DebugLevel
	case "trace":
		level = zerolog.TraceLevel
	case "":
		level = defaultLevel
	default:
		level = defaultLevel
	}

	Logger = Logger.Level(level)
}

var logout = zerolog.ConsoleWriter{
	Out:        os.Stdout,
	TimeFormat: time.RFC3339,
}

// Logger is a globally available logger instance.
var Logger = zerolog.New(logout).Level(defaultLevel).
	With().Timestamp().Logger().
	With().Caller().Logger()
