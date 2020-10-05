package dap

import (
	"flag"
	"io"
	"net"
	"os"
	"path/filepath"
	"reflect"
	"strings"
	"sync"
	"testing"
	"time"

	"github.com/go-delve/delve/pkg/logflags"
	protest "github.com/go-delve/delve/pkg/proc/test"
	"github.com/go-delve/delve/service"
	"github.com/go-delve/delve/service/dap/daptest"
	"github.com/go-delve/delve/service/debugger"
	"github.com/google/go-dap"
)

const stopOnEntry bool = true

func TestMain(m *testing.M) {
	var logOutput string
	flag.StringVar(&logOutput, "log-output", "", "configures log output")
	flag.Parse()
	logflags.Setup(logOutput != "", logOutput, "")
	os.Exit(protest.RunTestsWithFixtures(m))
}

// name is for _fixtures/<name>.go
func runTest(t *testing.T, name string, test func(c *daptest.Client, f protest.Fixture)) {
	var buildFlags protest.BuildFlags
	fixture := protest.BuildFixture(name, buildFlags)

	// Start the DAP server.
	listener, err := net.Listen("tcp", ":0")
	if err != nil {
		t.Fatal(err)
	}
	disconnectChan := make(chan struct{})
	server := NewServer(&service.Config{
		Listener:       listener,
		DisconnectChan: disconnectChan,
		Debugger: debugger.Config{
			Backend: "default",
		},
	})
	server.Run()
	// Give server time to start listening for clients
	time.Sleep(100 * time.Millisecond)

	var stopOnce sync.Once
	// Run a goroutine that stops the server when disconnectChan is signaled.
	// This helps us test that certain events cause the server to stop as
	// expected.
	go func() {
		<-disconnectChan
		stopOnce.Do(func() { server.Stop() })
	}()

	client := daptest.NewClient(listener.Addr().String())
	defer client.Close()

	defer func() {
		stopOnce.Do(func() { server.Stop() })
	}()

	test(client, fixture)
}

// TestStopOnEntry emulates the message exchange that can be observed with
// VS Code for the most basic debug session with "stopOnEntry" enabled:
// - User selects "Start Debugging":  1 >> initialize
//                                 :  1 << initialize
//                                 :  2 >> launch
//                                 :    << initialized event
//                                 :  2 << launch
//                                 :  3 >> setBreakpoints (empty)
//                                 :  3 << setBreakpoints
//                                 :  4 >> setExceptionBreakpoints (empty)
//                                 :  4 << setExceptionBreakpoints
//                                 :  5 >> configurationDone
// - Program stops upon launching  :    << stopped event
//                                 :  5 << configurationDone
//                                 :  6 >> threads
//                                 :  6 << threads (Dummy)
//                                 :  7 >> threads
//                                 :  7 << threads (Dummy)
//                                 :  8 >> stackTrace
//                                 :  8 << stackTrace (Unable to produce stack trace)
//                                 :  9 >> stackTrace
//                                 :  9 << stackTrace (Unable to produce stack trace)
// - User selects "Continue"       : 10 >> continue
//                                 : 10 << continue
// - Program runs to completion    :    << terminated event
//                                 : 11 >> disconnect
//                                 : 11 << disconnect
// This test exhaustively tests Seq and RequestSeq on all messages from the
// server. Other tests do not necessarily need to repeat all these checks.
func TestStopOnEntry(t *testing.T) {
	runTest(t, "increment", func(client *daptest.Client, fixture protest.Fixture) {
		// 1 >> initialize, << initialize
		client.InitializeRequest()
		initResp := client.ExpectInitializeResponse(t)
		if initResp.Seq != 0 || initResp.RequestSeq != 1 {
			t.Errorf("\ngot %#v\nwant Seq=0, RequestSeq=1", initResp)
		}

		// 2 >> launch, << initialized, << launch
		client.LaunchRequest("exec", fixture.Path, stopOnEntry)
		initEvent := client.ExpectInitializedEvent(t)
		if initEvent.Seq != 0 {
			t.Errorf("\ngot %#v\nwant Seq=0", initEvent)
		}
		launchResp := client.ExpectLaunchResponse(t)
		if launchResp.Seq != 0 || launchResp.RequestSeq != 2 {
			t.Errorf("\ngot %#v\nwant Seq=0, RequestSeq=2", launchResp)
		}

		// 3 >> setBreakpoints, << setBreakpoints
		client.SetBreakpointsRequest(fixture.Source, nil)
		sbpResp := client.ExpectSetBreakpointsResponse(t)
		if sbpResp.Seq != 0 || sbpResp.RequestSeq != 3 || len(sbpResp.Body.Breakpoints) != 0 {
			t.Errorf("\ngot %#v\nwant Seq=0, RequestSeq=3, len(Breakpoints)=0", sbpResp)
		}

		// 4 >> setExceptionBreakpoints, << setExceptionBreakpoints
		client.SetExceptionBreakpointsRequest()
		sebpResp := client.ExpectSetExceptionBreakpointsResponse(t)
		if sebpResp.Seq != 0 || sebpResp.RequestSeq != 4 {
			t.Errorf("\ngot %#v\nwant Seq=0, RequestSeq=4", sebpResp)
		}

		// 5 >> configurationDone, << stopped, << configurationDone
		client.ConfigurationDoneRequest()
		stopEvent := client.ExpectStoppedEvent(t)
		if stopEvent.Seq != 0 ||
			stopEvent.Body.Reason != "entry" ||
			stopEvent.Body.ThreadId != 1 ||
			!stopEvent.Body.AllThreadsStopped {
			t.Errorf("\ngot %#v\nwant Seq=0, Body={Reason=\"entry\", ThreadId=1, AllThreadsStopped=true}", stopEvent)
		}
		cdResp := client.ExpectConfigurationDoneResponse(t)
		if cdResp.Seq != 0 || cdResp.RequestSeq != 5 {
			t.Errorf("\ngot %#v\nwant Seq=0, RequestSeq=5", cdResp)
		}

		// 6 >> threads, << threads
		client.ThreadsRequest()
		tResp := client.ExpectThreadsResponse(t)
		if tResp.Seq != 0 || tResp.RequestSeq != 6 || len(tResp.Body.Threads) != 1 {
			t.Errorf("\ngot %#v\nwant Seq=0, RequestSeq=6 len(Threads)=1", tResp)
		}
		if tResp.Body.Threads[0].Id != 1 || tResp.Body.Threads[0].Name != "Dummy" {
			t.Errorf("\ngot %#v\nwant Id=1, Name=\"Dummy\"", tResp)
		}

		// 7 >> threads, << threads
		client.ThreadsRequest()
		tResp = client.ExpectThreadsResponse(t)
		if tResp.Seq != 0 || tResp.RequestSeq != 7 || len(tResp.Body.Threads) != 1 {
			t.Errorf("\ngot %#v\nwant Seq=0, RequestSeq=7 len(Threads)=1", tResp)
		}

		// 8 >> stackTrace, << stackTrace
		client.StackTraceRequest(1, 0, 20)
		stResp := client.ExpectErrorResponse(t)
		if stResp.Seq != 0 || stResp.RequestSeq != 8 || stResp.Body.Error.Format != "Unable to produce stack trace: unknown goroutine 1" {
			t.Errorf("\ngot %#v\nwant Seq=0, RequestSeq=8 Format=\"Unable to produce stack trace: unknown goroutine 1\"", stResp)
		}

		// 9 >> stackTrace, << stackTrace
		client.StackTraceRequest(1, 0, 20)
		stResp = client.ExpectErrorResponse(t)
		if stResp.Seq != 0 || stResp.RequestSeq != 9 || stResp.Body.Error.Id != 2004 {
			t.Errorf("\ngot %#v\nwant Seq=0, RequestSeq=9 Id=2004", stResp)
		}

		// 10 >> continue, << continue, << terminated
		client.ContinueRequest(1)
		contResp := client.ExpectContinueResponse(t)
		if contResp.Seq != 0 || contResp.RequestSeq != 10 {
			t.Errorf("\ngot %#v\nwant Seq=0, RequestSeq=10", contResp)
		}
		termEvent := client.ExpectTerminatedEvent(t)
		if termEvent.Seq != 0 {
			t.Errorf("\ngot %#v\nwant Seq=0", termEvent)
		}

		// 11 >> disconnect, << disconnect
		client.DisconnectRequest()
		dResp := client.ExpectDisconnectResponse(t)
		if dResp.Seq != 0 || dResp.RequestSeq != 11 {
			t.Errorf("\ngot %#v\nwant Seq=0, RequestSeq=11", dResp)
		}
	})
}

// Like the test above, except the program is configured to continue on entry.
func TestContinueOnEntry(t *testing.T) {
	runTest(t, "increment", func(client *daptest.Client, fixture protest.Fixture) {
		// 1 >> initialize, << initialize
		client.InitializeRequest()
		client.ExpectInitializeResponse(t)

		// 2 >> launch, << initialized, << launch
		client.LaunchRequest("exec", fixture.Path, !stopOnEntry)
		client.ExpectInitializedEvent(t)
		client.ExpectLaunchResponse(t)

		// 3 >> setBreakpoints, << setBreakpoints
		client.SetBreakpointsRequest(fixture.Source, nil)
		client.ExpectSetBreakpointsResponse(t)

		// 4 >> setExceptionBreakpoints, << setExceptionBreakpoints
		client.SetExceptionBreakpointsRequest()
		client.ExpectSetExceptionBreakpointsResponse(t)

		// 5 >> configurationDone, << configurationDone
		client.ConfigurationDoneRequest()
		client.ExpectConfigurationDoneResponse(t)
		// "Continue" happens behind the scenes

		// For now continue is blocking and runs until a stop or
		// termination. But once we upgrade the server to be async,
		// a simultaneous threads request can be made while continue
		// is running. Note that vscode-go just keeps track of the
		// continue state and would just return a dummy response
		// without talking to debugger if continue was in progress.
		// TODO(polina): test this once it is possible

		client.ExpectTerminatedEvent(t)

		// It is possible for the program to terminate before the initial
		// threads request is processed.

		// 6 >> threads, << threads
		client.ThreadsRequest()
		tResp := client.ExpectThreadsResponse(t)
		if tResp.Seq != 0 || tResp.RequestSeq != 6 || len(tResp.Body.Threads) != 0 {
			t.Errorf("\ngot %#v\nwant Seq=0, RequestSeq=6 len(Threads)=0", tResp)
		}

		// 7 >> disconnect, << disconnect
		client.DisconnectRequest()
		dResp := client.ExpectDisconnectResponse(t)
		if dResp.Seq != 0 || dResp.RequestSeq != 7 {
			t.Errorf("\ngot %#v\nwant Seq=0, RequestSeq=7", dResp)
		}
	})
}

// TestSetBreakpoint corresponds to a debug session that is configured to
// continue on entry with a pre-set breakpoint.
func TestSetBreakpoint(t *testing.T) {
	runTest(t, "increment", func(client *daptest.Client, fixture protest.Fixture) {
		client.InitializeRequest()
		client.ExpectInitializeResponse(t)

		client.LaunchRequest("exec", fixture.Path, !stopOnEntry)
		client.ExpectInitializedEvent(t)
		client.ExpectLaunchResponse(t)

		client.SetBreakpointsRequest(fixture.Source, []int{8, 100})
		sResp := client.ExpectSetBreakpointsResponse(t)
		if len(sResp.Body.Breakpoints) != 1 {
			t.Errorf("got %#v, want len(Breakpoints)=1", sResp)
		}
		bkpt0 := sResp.Body.Breakpoints[0]
		if !bkpt0.Verified || bkpt0.Line != 8 {
			t.Errorf("got breakpoints[0] = %#v, want Verified=true, Line=8", bkpt0)
		}

		client.SetExceptionBreakpointsRequest()
		client.ExpectSetExceptionBreakpointsResponse(t)

		client.ConfigurationDoneRequest()
		client.ExpectConfigurationDoneResponse(t)
		// This triggers "continue"

		// TODO(polina): add a no-op threads request
		// with dummy response here once server becomes async
		// to match what happens in VS Code.

		stopEvent1 := client.ExpectStoppedEvent(t)
		if stopEvent1.Body.Reason != "breakpoint" ||
			stopEvent1.Body.ThreadId != 1 ||
			!stopEvent1.Body.AllThreadsStopped {
			t.Errorf("got %#v, want Body={Reason=\"breakpoint\", ThreadId=1, AllThreadsStopped=true}", stopEvent1)
		}

		client.ThreadsRequest()
		tResp := client.ExpectThreadsResponse(t)
		if len(tResp.Body.Threads) < 2 { // 1 main + runtime
			t.Errorf("\ngot  %#v\nwant len(Threads)>1", tResp.Body.Threads)
		}
		// TODO(polina): can we reliably test for these values?
		wantMain := dap.Thread{Id: 1, Name: "main.Increment"}
		wantRuntime := dap.Thread{Id: 2, Name: "runtime.gopark"}
		for _, got := range tResp.Body.Threads {
			if !reflect.DeepEqual(got, wantMain) && !strings.HasPrefix(got.Name, "runtime") {
				t.Errorf("\ngot  %#v\nwant []dap.Thread{%#v, %#v, ...}", tResp.Body.Threads, wantMain, wantRuntime)
			}
		}

		client.StackTraceRequest(1, 0, 20)
		stResp := client.ExpectStackTraceResponse(t)

		if stResp.Body.TotalFrames != 6 {
			t.Errorf("\ngot %#v\nwant TotalFrames=6", stResp.Body.TotalFrames)
		}
		if len(stResp.Body.StackFrames) != 6 {
			t.Errorf("\ngot %#v\nwant len(StackFrames)=6", stResp.Body.StackFrames)
		} else {
			expectFrame := func(got dap.StackFrame, id int, name string, sourceName string, line int) {
				t.Helper()
				if got.Id != id || got.Name != name {
					t.Errorf("\ngot  %#v\nwant Id=%d Name=%s", got, id, name)
				}
				if (sourceName != "" && got.Source.Name != sourceName) || (line > 0 && got.Line != line) {
					t.Errorf("\ngot  %#v\nwant Source.Name=%s Line=%d", got, sourceName, line)
				}
			}
			expectFrame(stResp.Body.StackFrames[0], 1000, "main.Increment", "increment.go", 8)
			expectFrame(stResp.Body.StackFrames[1], 1001, "main.Increment", "increment.go", 11)
			expectFrame(stResp.Body.StackFrames[2], 1002, "main.Increment", "increment.go", 11)
			expectFrame(stResp.Body.StackFrames[3], 1003, "main.main", "increment.go", 17)
			expectFrame(stResp.Body.StackFrames[4], 1004, "runtime.main", "proc.go", -1)
			expectFrame(stResp.Body.StackFrames[5], 1005, "runtime.goexit", "", -1)
		}

		// TODO(polina): add other status checking requests
		// that are not yet supported (scopes, variables)

		client.ContinueRequest(1)
		client.ExpectContinueResponse(t)
		// "Continue" is triggered after the response is sent

		client.ExpectTerminatedEvent(t)
		client.DisconnectRequest()
		client.ExpectDisconnectResponse(t)
	})
}

// expectStackFrames is a helper for verifying the values within StackTraceResponse.
//     wantStartLine - file line of the first returned frame (non-positive values are ignored).
//     wantStartID - id of the first frame returned (ignored if wantFrames is 0).
//     wantFrames - number of frames returned.
//     wantTotalFrames - total number of stack frames (StackTraceResponse.Body.TotalFrames).
func expectStackFrames(t *testing.T, got *dap.StackTraceResponse,
	wantStartLine, wantStartID, wantFrames, wantTotalFrames int) {
	t.Helper()
	if got.Body.TotalFrames != wantTotalFrames {
		t.Errorf("\ngot  %#v\nwant TotalFrames=%d", got.Body.TotalFrames, wantTotalFrames)
	}
	if len(got.Body.StackFrames) != wantFrames {
		t.Errorf("\ngot  len(StackFrames)=%d\nwant %d", len(got.Body.StackFrames), wantFrames)
	} else {
		// Verify that frame ids are consecutive numbers starting at wantStartID
		for i := 0; i < wantFrames; i++ {
			if got.Body.StackFrames[i].Id != wantStartID+i {
				t.Errorf("\ngot  %#v\nwant Id=%d", got.Body.StackFrames[i], wantStartID+i)
			}
		}
		// Verify the line corresponding to the first returned frame (if any).
		// This is useful when the first frame is the frame corresponding to the breakpoint at
		// a predefined line. Values < 0 are a signal to skip the check (which can be useful
		// for frames in the third-party code, where we do not control the lines).
		if wantFrames > 0 && wantStartLine > 0 && got.Body.StackFrames[0].Line != wantStartLine {
			t.Errorf("\ngot  Line=%d\nwant %d", got.Body.StackFrames[0].Line, wantStartLine)
		}
	}
}

// TestStackTraceRequest executes to a breakpoint (similarly to TestSetBreakpoint
// that includes more thorough checking of that sequence) and tests different
// good and bad configurations of 'stackTrace' requests.
func TestStackTraceRequest(t *testing.T) {
	runTest(t, "increment", func(client *daptest.Client, fixture protest.Fixture) {
		var stResp *dap.StackTraceResponse
		runDebugSessionWithBPs(t, client,
			// Launch
			func() {
				client.LaunchRequest("exec", fixture.Path, !stopOnEntry)
			},
			// Set breakpoints
			fixture.Source, []int{8, 18},
			[]func(){
				// Stop at line 8
				func() {
					t.Helper()
					client.StackTraceRequest(1, 0, 0)
					stResp = client.ExpectStackTraceResponse(t)
					expectStackFrames(t, stResp, 8, 1000, 6, 6)

					// Even though the stack frames are the same,
					// repeated requests at the same breakpoint,
					// would assign unique ids to them each time.
					client.StackTraceRequest(1, -100, 0) // Negative startFrame is treated as 0
					stResp = client.ExpectStackTraceResponse(t)
					expectStackFrames(t, stResp, 8, 1006, 6, 6)

					client.StackTraceRequest(1, 3, 0)
					stResp = client.ExpectStackTraceResponse(t)
					expectStackFrames(t, stResp, 17, 1015, 3, 6)

					client.StackTraceRequest(1, 6, 0)
					stResp = client.ExpectStackTraceResponse(t)
					expectStackFrames(t, stResp, -1, -1, 0, 6)

					client.StackTraceRequest(1, 7, 0) // Out of bounds startFrame is capped at len
					stResp = client.ExpectStackTraceResponse(t)
					expectStackFrames(t, stResp, -1, -1, 0, 6)
				},
				// Stop at line 18
				func() {
					t.Helper()
					// Frame ids get reset at each breakpoint.
					client.StackTraceRequest(1, 0, 0)
					stResp = client.ExpectStackTraceResponse(t)
					expectStackFrames(t, stResp, 18, 1000, 3, 3)

					client.StackTraceRequest(1, 0, -100) // Negative levels is treated as 0
					stResp = client.ExpectStackTraceResponse(t)
					expectStackFrames(t, stResp, 18, 1003, 3, 3)

					client.StackTraceRequest(1, 0, 2)
					stResp = client.ExpectStackTraceResponse(t)
					expectStackFrames(t, stResp, 18, 1006, 2, 3)

					client.StackTraceRequest(1, 0, 3)
					stResp = client.ExpectStackTraceResponse(t)
					expectStackFrames(t, stResp, 18, 1009, 3, 3)

					client.StackTraceRequest(1, 0, 4) // Out of bounds levels is capped at len
					stResp = client.ExpectStackTraceResponse(t)
					expectStackFrames(t, stResp, 18, 1012, 3, 3)

					client.StackTraceRequest(1, 1, 2)
					stResp = client.ExpectStackTraceResponse(t)
					expectStackFrames(t, stResp, -1, 1016, 2, 3) // Don't test for runtime line we don't control
				}})
	})
}

// Tests that 'stackTraceDepth' from LaunchRequest is parsed and passed to
// stacktrace requests handlers.
func TestLaunchRequestWithStackTraceDepth(t *testing.T) {
	runTest(t, "increment", func(client *daptest.Client, fixture protest.Fixture) {
		var stResp *dap.StackTraceResponse
		runDebugSessionWithBPs(t, client,
			// Launch
			func() {
				client.LaunchRequestWithArgs(map[string]interface{}{
					"mode": "exec", "program": fixture.Path, "stackTraceDepth": 1,
				})
			},
			// Set breakpoints
			fixture.Source, []int{8},
			[]func(){
				// Stop at line 8
				func() {
					t.Helper()
					client.StackTraceRequest(1, 0, 0)
					stResp = client.ExpectStackTraceResponse(t)
					expectStackFrames(t, stResp, 8, 1000, 2, 2)
				}})
	})
}

// runDebugSesionWithBPs is a helper for executing the common init and shutdown
// sequences for a program that does not stop on entry
// while specifying breakpoints and unique launch criteria via parameters.
//     launchRequest - a function that sends a launch request, so the test author
//                     has full control of its arguments. Note that he rest of
//                     the test sequence assumes that stopOneEntry is false.
//     breakpoints   - list of lines, where breakpoints are to be set
//     onBreakpoints - list of functions to be called at each of the above breakpoints.
//                     These can be used to simulate additional editor-driven or
//                     user-driven requests that could occur at each stopped breakpoint.
func runDebugSessionWithBPs(t *testing.T, client *daptest.Client, launchRequest func(), source string, breakpoints []int, onBreakpoints []func()) {
	t.Helper()
	client.InitializeRequest()
	client.ExpectInitializeResponse(t)

	launchRequest()
	client.ExpectInitializedEvent(t)
	client.ExpectLaunchResponse(t)

	client.SetBreakpointsRequest(source, breakpoints)
	client.ExpectSetBreakpointsResponse(t)

	// Skip no-op setExceptionBreakpoints

	client.ConfigurationDoneRequest()
	client.ExpectConfigurationDoneResponse(t)

	// Program automatically continues to breakpoint or completion

	for _, onBP := range onBreakpoints {
		client.ExpectStoppedEvent(t)
		onBP()
		client.ContinueRequest(1)
		client.ExpectContinueResponse(t)
		// "Continue" is triggered after the response is sent
	}

	client.ExpectTerminatedEvent(t)
	client.DisconnectRequest()
	client.ExpectDisconnectResponse(t)
}

// runDebugSesion is a helper for executing the standard init and shutdown
// sequences for a program that does not stop on entry
// while specifying unique launch criteria via parameters.
func runDebugSession(t *testing.T, client *daptest.Client, launchRequest func()) {
	runDebugSessionWithBPs(t, client, launchRequest, "", nil, nil)
}

func TestLaunchDebugRequest(t *testing.T) {
	runTest(t, "increment", func(client *daptest.Client, fixture protest.Fixture) {
		// We reuse the harness that builds, but ignore the built binary,
		// only relying on the source to be built in response to LaunchRequest.
		runDebugSession(t, client, func() {
			// Use the default output directory.
			client.LaunchRequestWithArgs(map[string]interface{}{
				"mode": "debug", "program": fixture.Source})
		})
	})
}

func TestLaunchTestRequest(t *testing.T) {
	runTest(t, "increment", func(client *daptest.Client, fixture protest.Fixture) {
		runDebugSession(t, client, func() {
			// We reuse the harness that builds, but ignore the built binary,
			// only relying on the source to be built in response to LaunchRequest.
			fixtures := protest.FindFixturesDir()
			testdir, _ := filepath.Abs(filepath.Join(fixtures, "buildtest"))
			client.LaunchRequestWithArgs(map[string]interface{}{
				"mode": "test", "program": testdir, "output": "__mytestdir"})
		})
	})
}

// Tests that 'args' from LaunchRequest are parsed and passed to the target
// program. The target program exits without an error on success, and
// panics on error, causing an unexpected StoppedEvent instead of
// Terminated Event.
func TestLaunchRequestWithArgs(t *testing.T) {
	runTest(t, "testargs", func(client *daptest.Client, fixture protest.Fixture) {
		runDebugSession(t, client, func() {
			client.LaunchRequestWithArgs(map[string]interface{}{
				"mode": "exec", "program": fixture.Path,
				"args": []string{"test", "pass flag"}})
		})
	})
}

// Tests that 'buildFlags' from LaunchRequest are parsed and passed to the
// compiler. The target program exits without an error on success, and
// panics on error, causing an unexpected StoppedEvent instead of
// TerminatedEvent.
func TestLaunchRequestWithBuildFlags(t *testing.T) {
	runTest(t, "buildflagtest", func(client *daptest.Client, fixture protest.Fixture) {
		runDebugSession(t, client, func() {
			// We reuse the harness that builds, but ignore the built binary,
			// only relying on the source to be built in response to LaunchRequest.
			client.LaunchRequestWithArgs(map[string]interface{}{
				"mode": "debug", "program": fixture.Source,
				"buildFlags": "-ldflags '-X main.Hello=World'"})
		})
	})
}

func TestUnupportedCommandResponses(t *testing.T) {
	var got *dap.ErrorResponse
	runTest(t, "increment", func(client *daptest.Client, fixture protest.Fixture) {
		seqCnt := 1
		expectUnsupportedCommand := func(cmd string) {
			t.Helper()
			got = client.ExpectUnsupportedCommandErrorResponse(t)
			if got.RequestSeq != seqCnt || got.Command != cmd {
				t.Errorf("\ngot  %#v\nwant RequestSeq=%d Command=%s", got, seqCnt, cmd)
			}
			seqCnt++
		}

		client.RestartFrameRequest()
		expectUnsupportedCommand("restartFrame")

		client.GotoRequest()
		expectUnsupportedCommand("goto")

		client.SourceRequest()
		expectUnsupportedCommand("source")

		client.TerminateThreadsRequest()
		expectUnsupportedCommand("terminateThreads")

		client.StepInTargetsRequest()
		expectUnsupportedCommand("stepInTargets")

		client.GotoTargetsRequest()
		expectUnsupportedCommand("gotoTargets")

		client.CompletionsRequest()
		expectUnsupportedCommand("completions")

		client.ExceptionInfoRequest()
		expectUnsupportedCommand("exceptionInfo")

		client.DataBreakpointInfoRequest()
		expectUnsupportedCommand("dataBreakpointInfo")

		client.SetDataBreakpointsRequest()
		expectUnsupportedCommand("setDataBreakpoints")

		client.BreakpointLocationsRequest()
		expectUnsupportedCommand("breakpointLocations")

		client.ModulesRequest()
		expectUnsupportedCommand("modules")
	})
}

func TestRequiredNotYetImplementedResponses(t *testing.T) {
	var got *dap.ErrorResponse
	runTest(t, "increment", func(client *daptest.Client, fixture protest.Fixture) {
		seqCnt := 1
		expectNotYetImplemented := func(cmd string) {
			t.Helper()
			got = client.ExpectNotYetImplementedErrorResponse(t)
			if got.RequestSeq != seqCnt || got.Command != cmd {
				t.Errorf("\ngot  %#v\nwant RequestSeq=%d Command=%s", got, seqCnt, cmd)
			}
			seqCnt++
		}

		client.AttachRequest()
		expectNotYetImplemented("attach")

		client.NextRequest()
		expectNotYetImplemented("next")

		client.StepInRequest()
		expectNotYetImplemented("stepIn")

		client.StepOutRequest()
		expectNotYetImplemented("stepOut")

		client.PauseRequest()
		expectNotYetImplemented("pause")

		client.ScopesRequest()
		expectNotYetImplemented("scopes")

		client.VariablesRequest()
		expectNotYetImplemented("variables")

		client.EvaluateRequest()
		expectNotYetImplemented("evaluate")
	})
}

func TestOptionalNotYetImplementedResponses(t *testing.T) {
	var got *dap.ErrorResponse
	runTest(t, "increment", func(client *daptest.Client, fixture protest.Fixture) {
		seqCnt := 1
		expectNotYetImplemented := func(cmd string) {
			t.Helper()
			got = client.ExpectNotYetImplementedErrorResponse(t)
			if got.RequestSeq != seqCnt || got.Command != cmd {
				t.Errorf("\ngot  %#v\nwant RequestSeq=%d Command=%s", got, seqCnt, cmd)
			}
			seqCnt++
		}

		client.TerminateRequest()
		expectNotYetImplemented("terminate")

		client.RestartRequest()
		expectNotYetImplemented("restart")

		client.SetFunctionBreakpointsRequest()
		expectNotYetImplemented("setFunctionBreakpoints")

		client.StepBackRequest()
		expectNotYetImplemented("stepBack")

		client.ReverseContinueRequest()
		expectNotYetImplemented("reverseContinue")

		client.SetVariableRequest()
		expectNotYetImplemented("setVariable")

		client.SetExpressionRequest()
		expectNotYetImplemented("setExpression")

		client.LoadedSourcesRequest()
		expectNotYetImplemented("loadedSources")

		client.ReadMemoryRequest()
		expectNotYetImplemented("readMemory")

		client.DisassembleRequest()
		expectNotYetImplemented("disassemble")

		client.CancelRequest()
		expectNotYetImplemented("cancel")
	})
}

func TestBadLaunchRequests(t *testing.T) {
	runTest(t, "increment", func(client *daptest.Client, fixture protest.Fixture) {
		seqCnt := 1
		expectFailedToLaunch := func(response *dap.ErrorResponse) {
			t.Helper()
			if response.RequestSeq != seqCnt {
				t.Errorf("RequestSeq got %d, want %d", seqCnt, response.RequestSeq)
			}
			if response.Command != "launch" {
				t.Errorf("Command got %q, want \"launch\"", response.Command)
			}
			if response.Message != "Failed to launch" {
				t.Errorf("Message got %q, want \"Failed to launch\"", response.Message)
			}
			if response.Body.Error.Id != 3000 {
				t.Errorf("Id got %d, want 3000", response.Body.Error.Id)
			}
			seqCnt++
		}

		expectFailedToLaunchWithMessage := func(response *dap.ErrorResponse, errmsg string) {
			t.Helper()
			expectFailedToLaunch(response)
			if response.Body.Error.Format != errmsg {
				t.Errorf("\ngot  %q\nwant %q", response.Body.Error.Format, errmsg)
			}
		}

		// Test for the DAP-specific detailed error message.
		client.LaunchRequest("exec", "", stopOnEntry)
		expectFailedToLaunchWithMessage(client.ExpectErrorResponse(t),
			"Failed to launch: The program attribute is missing in debug configuration.")

		client.LaunchRequestWithArgs(map[string]interface{}{"program": 12345})
		expectFailedToLaunchWithMessage(client.ExpectErrorResponse(t),
			"Failed to launch: The program attribute is missing in debug configuration.")

		client.LaunchRequestWithArgs(map[string]interface{}{"program": nil})
		expectFailedToLaunchWithMessage(client.ExpectErrorResponse(t),
			"Failed to launch: The program attribute is missing in debug configuration.")

		client.LaunchRequestWithArgs(map[string]interface{}{})
		expectFailedToLaunchWithMessage(client.ExpectErrorResponse(t),
			"Failed to launch: The program attribute is missing in debug configuration.")

		client.LaunchRequest("remote", fixture.Path, stopOnEntry)
		expectFailedToLaunchWithMessage(client.ExpectErrorResponse(t),
			"Failed to launch: Unsupported 'mode' value \"remote\" in debug configuration.")

		client.LaunchRequest("notamode", fixture.Path, stopOnEntry)
		expectFailedToLaunchWithMessage(client.ExpectErrorResponse(t),
			"Failed to launch: Unsupported 'mode' value \"notamode\" in debug configuration.")

		client.LaunchRequestWithArgs(map[string]interface{}{"mode": 12345, "program": fixture.Path})
		expectFailedToLaunchWithMessage(client.ExpectErrorResponse(t),
			"Failed to launch: Unsupported 'mode' value %!q(float64=12345) in debug configuration.")

		client.LaunchRequestWithArgs(map[string]interface{}{"mode": "exec", "program": fixture.Path, "args": nil})
		expectFailedToLaunchWithMessage(client.ExpectErrorResponse(t),
			"Failed to launch: 'args' attribute '<nil>' in debug configuration is not an array.")

		client.LaunchRequestWithArgs(map[string]interface{}{"mode": "exec", "program": fixture.Path, "args": 12345})
		expectFailedToLaunchWithMessage(client.ExpectErrorResponse(t),
			"Failed to launch: 'args' attribute '12345' in debug configuration is not an array.")

		client.LaunchRequestWithArgs(map[string]interface{}{"mode": "exec", "program": fixture.Path, "args": []int{1, 2}})
		expectFailedToLaunchWithMessage(client.ExpectErrorResponse(t),
			"Failed to launch: value '1' in 'args' attribute in debug configuration is not a string.")

		client.LaunchRequestWithArgs(map[string]interface{}{"mode": "debug", "program": fixture.Source, "buildFlags": 123})
		expectFailedToLaunchWithMessage(client.ExpectErrorResponse(t),
			"Failed to launch: 'buildFlags' attribute '123' in debug configuration is not a string.")

		// Skip detailed message checks for potentially different OS-specific errors.
		client.LaunchRequest("exec", fixture.Path+"_does_not_exist", stopOnEntry)
		expectFailedToLaunch(client.ExpectErrorResponse(t))

		client.LaunchRequest("debug", fixture.Path+"_does_not_exist", stopOnEntry)
		expectFailedToLaunch(client.ExpectErrorResponse(t)) // Build error

		client.LaunchRequest("exec", fixture.Source, stopOnEntry)
		expectFailedToLaunch(client.ExpectErrorResponse(t)) // Not an executable

		client.LaunchRequestWithArgs(map[string]interface{}{"mode": "debug", "program": fixture.Source, "buildFlags": "123"})
		expectFailedToLaunch(client.ExpectErrorResponse(t)) // Build error

		// We failed to launch the program. Make sure shutdown still works.
		client.DisconnectRequest()
		dresp := client.ExpectDisconnectResponse(t)
		if dresp.RequestSeq != seqCnt {
			t.Errorf("got %#v, want RequestSeq=%d", dresp, seqCnt)
		}
	})
}

func TestBadlyFormattedMessageToServer(t *testing.T) {
	runTest(t, "increment", func(client *daptest.Client, fixture protest.Fixture) {
		// Send a badly formatted message to the server, and expect it to close the
		// connection.
		client.UnknownRequest()
		time.Sleep(100 * time.Millisecond)

		_, err := client.ReadMessage()

		if err != io.EOF {
			t.Errorf("got err=%v, want io.EOF", err)
		}
	})
}
