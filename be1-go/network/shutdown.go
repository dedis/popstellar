package network

import (
	"fmt"
	"log"
	"os"
	"os/signal"
	"strings"
	"syscall"

	"golang.org/x/xerrors"
)

// WaitAndShutdownServers blocks until the user passes a SIGINT or SIGTERM and
// then shuts down the http servers.
func WaitAndShutdownServers(servers ...*Server) error {
	done := make(chan os.Signal, 1)
	signal.Notify(done, syscall.SIGINT, syscall.SIGTERM)
	<-done
	log.Println("received ctrl+c")

	errors := []string{}
	for i, server := range servers {
		err := server.Shutdown()
		if err != nil {
			errors = append(errors, fmt.Sprintf("%d: %s", i, err))
		}
	}

	if len(errors) > 0 {
		return xerrors.Errorf("failed to shutdown one or more servers: %s",
			strings.Join(errors, ";"))
	}

	return nil
}
