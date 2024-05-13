package main

import (
	"context"
	"github.com/rs/zerolog"
	"github.com/stretchr/testify/require"
	"io"
	"os"
	"popstellar/internal/popserver/state"
	"popstellar/internal/popserver/types"
	"sync"
	"testing"
	"time"
)

const waitUp = time.Second * 2

func TestConnectMultipleServers(t *testing.T) {

	noLog := zerolog.New(io.Discard)
	ctx, cancel := context.WithCancel(context.Background())
	wait := sync.WaitGroup{}

	err := state.SetState(t, types.NewSubscribers(), types.NewPeers(), types.NewQueries(&noLog))
	require.NoError(t, err)

	wait.Add(1)
	go func() {
		defer wait.Done()

		args := []string{os.Args[0], "server", "--pk", "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=", "serve", "--server-port", "9011", "--client-port", "8010", "--auth-port", "9103"}
		t.Logf("running server 1: %v", args)

		run(ctx, args)
		t.Log("server 1 done")
	}()

	t.Log("server 1 up")

	err = state.SetState(t, types.NewSubscribers(), types.NewPeers(), types.NewQueries(&noLog))
	require.NoError(t, err)

	wait.Add(1)
	go func() {
		defer wait.Done()

		args := []string{os.Args[0], "server", "--pk", "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=", "serve",
			"--client-port", "8020", "--server-port", "9003", "--other-servers", "localhost:9011", "--auth-port", "9101"}
		t.Logf("running server 2: %v", args)

		run(ctx, args)
		t.Log("server 2 done")
	}()

	t.Log("server 2 up")

	err = state.SetState(t, types.NewSubscribers(), types.NewPeers(), types.NewQueries(&noLog))
	require.NoError(t, err)

	wait.Add(1)
	go func() {
		defer wait.Done()

		args := []string{os.Args[0], "server", "--pk", "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=", "serve", "--server-port", "9004", "--client-port", "8021", "--other-servers", "localhost:9011", "localhost:9003", "--auth-port", "9102"}
		t.Logf("running server 3: %v", args)

		run(ctx, args)
		t.Log("server 3 done")
	}()

	t.Log("server 3 up")

	cancel()
	wait.Wait()
}

func TestConnectMultipleServersWithoutPK(t *testing.T) {

	noLog := zerolog.New(io.Discard)
	ctx, cancel := context.WithCancel(context.Background())
	wait := sync.WaitGroup{}

	err := state.SetState(t, types.NewSubscribers(), types.NewPeers(), types.NewQueries(&noLog))
	require.NoError(t, err)

	wait.Add(1)
	go func() {
		defer wait.Done()

		args := []string{os.Args[0], "server", "serve", "--server-port", "9011", "--client-port", "8010", "--auth-port", "9101"}
		t.Logf("running server 1: %v", args)

		run(ctx, args)
		t.Log("server 1 done")
	}()

	t.Log("server 1 up")

	err = state.SetState(t, types.NewSubscribers(), types.NewPeers(), types.NewQueries(&noLog))
	require.NoError(t, err)

	wait.Add(1)
	go func() {
		defer wait.Done()

		args := []string{os.Args[0], "server", "serve", "--server-port", "9003", "--client-port", "8020", "--other-servers", "localhost:9011", "--auth-port", "9102"}
		t.Logf("running server 2: %v", args)

		run(ctx, args)
		t.Log("server 2 done")
	}()

	t.Log("server 2 up")

	err = state.SetState(t, types.NewSubscribers(), types.NewPeers(), types.NewQueries(&noLog))
	require.NoError(t, err)

	wait.Add(1)
	go func() {
		defer wait.Done()

		args := []string{os.Args[0], "server", "serve", "--server-port", "9004", "--client-port", "8021", "--other-servers", "localhost:9011", "localhost:9003", "--auth-port", "9103"}
		t.Logf("running server 3: %v", args)

		run(ctx, args)
		t.Log("server 3 done")
	}()

	t.Log("server 3 up")

	cancel()
	wait.Wait()
}
