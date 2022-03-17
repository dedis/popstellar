IF /I "%1"=="build" GOTO build
IF /I "%1"=="lint" GOTO lint
IF /I "%1"=="check" GOTO check
IF /I "%1"=="vet" GOTO vet
IF /I "%1"=="protocol" GOTO protocol 
IF /I "%1"=="clean" GOTO clean
GOTO error

:build
	CALL :protocol
	go build -o pop ./cli/
	REN pop pop.exe
	GOTO :EOF

:lint
	go get -v honnef.co/go/tools/cmd/staticcheck
	go mod tidy
	staticcheck ./...
	GOTO :EOF

:check
	CALL :protocol
	CALL :lint
	CALL :vet

	go test -v ./...
	go test -race -v ./...

	GOTO :EOF

:vet
	CALL :protocol
	go vet ./...
	GOTO :EOF

:protocol
	XCOPY /E /H /Y ..\protocol\ validation\protocol\
	GOTO :EOF

:clean
	RMDIR /S /Q validation\protocol\
	GOTO :EOF
	
:error
    IF "%1"=="" (
        ECHO make: *** No targets specified and no makefile found.  Stop.
    ) ELSE (
        ECHO make: *** No rule to make target '%1%'. Stop.
    )
    GOTO :EOF
