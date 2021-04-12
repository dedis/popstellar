package main

import (
	"log"
	"os"
	"student20_pop/cli/organizer"
	"student20_pop/cli/witness"

	"github.com/urfave/cli/v2"
)

func main() {
	var app *cli.App
	switch os.Args[1] {
	case "organizer":
		app = &cli.App{
			Name:  "pop",
			Usage: "backend for the PoP project",
			Commands: []*cli.Command{
				{
					Name:  "organizer",
					Usage: "manage the organizer",
					Flags: []cli.Flag{
						&cli.StringFlag{
							Name:    "public-key",
							Aliases: []string{"pk"},
							Usage:   "base64 encoded organizer's public key",
						},
					},
					Subcommands: []*cli.Command{
						{
							Name:  "serve",
							Usage: "start the organizer server",
							Flags: []cli.Flag{
								&cli.IntFlag{
									Name:    "port",
									Aliases: []string{"p"},
									Usage:   "port to listen websocket connections on",
									Value:   9000,
								},
							},
							Action: organizer.OrganizerServe,
						},
					},
				},
			},
		}
	case "witness":
		app = &cli.App{
			Name:  "pop",
			Usage: "backend for the PoP project",
			Commands: []*cli.Command{
				{
					Name:  "witness",
					Usage: "manage the witness",
					Flags: []cli.Flag{
						&cli.StringFlag{
							Name:    "public-key",
							Aliases: []string{"pk"},
							Usage:   "base64 encoded witness's public key",
						},
					},
					Subcommands: []*cli.Command{
						{
							Name:  "serve",
							Usage: "start the organizer server",
							Flags: []cli.Flag{
								&cli.IntFlag{
									Name:    "port",
									Aliases: []string{"p"},
									Usage:   "port to listen websocket connections on",
									Value:   9000,
								},
							},
							Action: witness.WitnessServe,
						},
					},
				},
			},
		}
	default:
		log.Printf("error: first arg must be organizer or witness")
		return
	}

	err := app.Run(os.Args)
	if err != nil {
		log.Fatal(err)
	}
}
