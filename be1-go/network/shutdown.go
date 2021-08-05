package network

import (
	"context"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
)

// ShutdownServers blocks until the user passes a SIGINT or SIGTERM and then
// shuts down the http servers.
func ShutdownServers(ctx context.Context, witnessSrv *http.Server, clientSrv *http.Server) {
	done := make(chan os.Signal, 1)
	signal.Notify(done, syscall.SIGINT, syscall.SIGTERM)
	<-done
	log.Println("received ctrl+c")

	shutdownServers(ctx, witnessSrv, clientSrv)
}

// shutdownServers shuts down witnessSrv and clientSrv.
func shutdownServers(ctx context.Context, witnessSrv *http.Server, clientSrv *http.Server) {
	err := clientSrv.Shutdown(ctx)
	if err != nil {
		log.Fatalf("failed to shutdown client server: %v", err)
	}

	err = witnessSrv.Shutdown(ctx)
	if err != nil {
		log.Fatalf("failed to shutdown witness server: %v", err)
	}

	log.Println("shutdown both servers")
}
