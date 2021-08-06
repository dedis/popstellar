package network

import (
	"log"
	"os"
	"os/signal"
	"syscall"

	"golang.org/x/xerrors"
)

// WaitAndShutdownServers blocks until the user passes a SIGINT or SIGTERM and then
// shuts down the http servers.
func WaitAndShutdownServers(servers ...*Server) error {
	done := make(chan os.Signal, 1)
	signal.Notify(done, syscall.SIGINT, syscall.SIGTERM)
	<-done
	log.Println("received ctrl+c")

	for _, server := range servers {
		err := server.Shutdown()
		if err != nil {
			return xerrors.Errorf("failed to shutdown server: %s", err)
		}
	}

	return nil
}
