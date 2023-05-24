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
//	   server  manage the server
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
	serverAddressFlag := &cli.StringFlag{
		Name:    "server-address",
		Aliases: []string{"sa"},
		Usage:   "address of the server endpoint",
	}
	clientAddressFlag := &cli.StringFlag{
		Name:    "client-address",
		Aliases: []string{"ca"},
		Usage:   "address of the client endpoint",
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
	clientPortFlag := &cli.IntFlag{
		Name:    "client-port",
		Aliases: []string{"cp"},
		Usage:   "port to listen websocket connections from clients on",
		Value:   9000,
	}
	serverPortFlag := &cli.IntFlag{
		Name:    "server-port",
		Aliases: []string{"sp"},
		Usage:   "port to listen websocket connections from remote servers on",
		Value:   9001,
	}
	otherServersFlag := &cli.StringSliceFlag{
		Name:    "other-servers",
		Aliases: []string{"os"},
		Usage:   "address and port to connect to other servers",
	}
	configFileFlag := &cli.StringFlag{
		Name:    "config-file",
		Aliases: []string{"cf"},
		Usage:   "path to the config file which will override other flags if present",
	}

	app := &cli.App{
		Name:  "pop",
		Usage: "backend for the PoP project",
		Commands: []*cli.Command{
			{
				Name:  "server",
				Usage: "manage the server",
				Flags: []cli.Flag{
					publicKeyFlag,
				},
				Subcommands: []*cli.Command{
					{
						Name:  "serve",
						Usage: "start the server",
						Flags: []cli.Flag{
							serverAddressFlag,
							clientAddressFlag,
							serverPublicAddressFlag,
							serverListenAddressFlag,
							clientPortFlag,
							serverPortFlag,
							otherServersFlag,
							configFileFlag,
						},
						Action: func(c *cli.Context) error {
							err := Serve(c)
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
