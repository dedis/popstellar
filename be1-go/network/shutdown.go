package network

import (
	"context"
	"os"
	"os/signal"
	"popstellar/popcha"
	"syscall"
)

// WaitAndShutdownServers blocks until the user passes a SIGINT or SIGTERM and
// then shuts down the http servers. The popchaSrv argument is optional, and handles the case where
// we use the authorization server.
func WaitAndShutdownServers(ctx context.Context, popchaSrv *popcha.AuthorizationServer, servers ...*Server) error {
	done := make(chan os.Signal, 1)
	signal.Notify(done, syscall.SIGINT, syscall.SIGTERM)

	select {
	case <-done:
	case <-ctx.Done():
	}

	for _, server := range servers {
		server.log.Info().Msgf("shutting down server %s", server.srv.Addr)
		err := server.Shutdown()
		if err != nil {
			server.log.Err(err).Msgf("failed to shutdown server %s", server.srv.Addr)
		}
	}

	// making sure that passing the popchaSrv is optional
	if popchaSrv != nil {
		return popchaSrv.Shutdown()
	}
	return nil
}
