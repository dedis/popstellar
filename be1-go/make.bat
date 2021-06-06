@echo off


IF /I "%1"=="build" GOTO build
IF /I "%1"=="test" GOTO test
IF /I "%1"=="test_race" GOTO test_race
IF /I "%1"=="vet" GOTO vet
IF /I "%1"=="copy_protocol" GOTO copy_protocol
IF /I "%1"=="clean_protocol" GOTO clean_protocol
GOTO error

:build
	%call execute_command, go build -o pop ./cli/%
	GOTO :EOF

:test
	%call execute_command, go test -v ./...%
	GOTO :EOF

:test_race
	%call execute_command, go test -race -v ./...%
	GOTO :EOF

:vet
	%call execute_command, go vet ./...%
	GOTO :EOF

:copy_protocol
	XCOPY /Y ../protocol ./validation -r
	GOTO :EOF

:clean_protocol
	DEL /Q validation/protocol -rf
	GOTO :EOF

:error
    IF "%1"=="" (
        ECHO make: *** No targets specified and no makefile found.  Stop.
    ) ELSE (
        ECHO make: *** No rule to make target '%1%'. Stop.
    )
    GOTO :EOF
