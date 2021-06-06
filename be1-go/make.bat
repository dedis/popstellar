IF /I "%1"=="build" GOTO build
IF /I "%1"=="test" GOTO test
IF /I "%1"=="test_race" GOTO test_race
IF /I "%1"=="vet" GOTO vet
IF /I "%1"=="copy_protocol" GOTO copy_protocol
IF /I "%1"=="clean_protocol" GOTO clean_protocol
GOTO error

:build
	CALL :copy_protocol
	go build -o pop ./cli/
	CALL :clean_protocol
	GOTO :EOF

:test
	CALL :copy_protocol
	go test -v ./...
	CALL :clean_protocol
	GOTO :EOF

:test_race
	CALL :copy_protocol
	go test -race -v ./...
	CALL :clean_protocol

	GOTO :EOF

:vet
	CALL :copy_protocol
	go vet ./...
	CALL :clean_protocol
	GOTO :EOF

:copy_protocol
	XCOPY /E /H /Y ..\protocol\ validation\protocol\
	GOTO :EOF

:clean_protocol
	RMDIR /S /Q validation\protocol\
	GOTO :EOF
	
:error
    IF "%1"=="" (
        ECHO make: *** No targets specified and no makefile found.  Stop.
    ) ELSE (
        ECHO make: *** No rule to make target '%1%'. Stop.
    )
    GOTO :EOF
