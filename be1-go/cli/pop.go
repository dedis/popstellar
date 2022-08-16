// Package main is the entry point for the pop application
//
// The pop binary may be compiled by executing `make build` in the be1-go
// directory. More information about the subcommands is available by
// executing the binary with a -h flag
//
//	./pop -h
//	NAME:
//	  pop - backend for the PoP project
//
//	USAGE:
//	  pop [global options] command [command options] [arguments...]
//
//	COMMANDS:
//	   organizer  manage the organizer
//	   witness    manage the witness
//	   help, h    Shows a list of commands or help for one command
//
//	GLOBAL OPTIONS:
//	   --help, -h  show help (default: false)
package main

import (
	"context"
	"log"
	"os"

	"github.com/urfave/cli/v2"
)

func main() {
	run(context.Background(), os.Args)
}

func run(ctx context.Context, args []string) {
	publicKeyFlag := &cli.StringFlag{
		Name:    "public-key",
		Aliases: []string{"pk"},
		Usage:   "base64url encoded server's public key",
	}
	serverPublicAddressFlag := &cli.StringFlag{
		Name:    "server-public-address",
		Aliases: []string{"spa"},
		Usage:   "address for clients to connect to",
		Value:   "localhost",
	}
	serverListenAddressFlag := &cli.StringFlag{
		Name:    "server-listen-address",
		Aliases: []string{"sla"},
		Usage:   "address where the server should listen to",
		Value:   "localhost",
	}
	organizerAddressFlag := &cli.StringFlag{
		Name:    "organizer-address",
		Aliases: []string{"org"},
		Usage:   "address and witness port of organizer",
		Value:   "localhost:9002",
	}
	clientPortFlag := &cli.IntFlag{
		Name:    "client-port",
		Aliases: []string{"cp"},
		Usage:   "port to listen websocket connections from clients on",
		Value:   9000,
	}
	witnessPortFlag := &cli.IntFlag{
		Name:    "witness-port",
		Aliases: []string{"wp"},
		Usage:   "port to listen websocket connections from witnesses on",
		Value:   9002,
	}
	otherWitnessFlag := &cli.StringSliceFlag{
		Name:    "other-witness",
		Aliases: []string{"ow"},
		Usage:   "address and port to connect to other witness",
	}

	app := &cli.App{
		Name:  "pop",
		Usage: "backend for the PoP project",
		Commands: []*cli.Command{
			{
				Name:  "organizer",
				Usage: "manage the organizer",
				Flags: []cli.Flag{
					publicKeyFlag,
				},
				Subcommands: []*cli.Command{
					{
						Name:  "serve",
						Usage: "start the organizer server",
						Flags: []cli.Flag{
							serverPublicAddressFlag,
							serverListenAddressFlag,
							clientPortFlag,
							witnessPortFlag,
						},
						Action: func(c *cli.Context) error {
							err := Serve(c, "organizer")
							return err
						},
					},
				},
			},
			{
				Name:  "witness",
				Usage: "manage the witness",
				Flags: []cli.Flag{
					publicKeyFlag,
				},
				Subcommands: []*cli.Command{
					{
						Name:  "serve",
						Usage: "start the witness server",
						Flags: []cli.Flag{
							serverPublicAddressFlag,
							serverListenAddressFlag,
							organizerAddressFlag,
							clientPortFlag,
							witnessPortFlag,
							otherWitnessFlag,
						},
						Action: func(c *cli.Context) error {
							err := Serve(c, "witness")
							return err
						},
					},
				},
			},
		},
	}

	err := app.RunContext(ctx, args)
	if err != nil {
		log.Fatal(err)
	}
}
