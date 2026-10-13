@echo off
echo =====================================================================
echo  Digital Library Management System (DLMS) - Backend Server
echo  Embedded HTTP Server on http://localhost:8080
echo =====================================================================
if not exist "bin" mkdir bin
javac -cp "lib/*" -d bin src/com/evoriea/dlms/config/*.java src/com/evoriea/dlms/model/*.java src/com/evoriea/dlms/dao/*.java src/com/evoriea/dlms/service/*.java src/com/evoriea/dlms/data/*.java src/com/evoriea/dlms/web/*.java
if errorlevel 1 (
    echo Compilation failed!
    pause
    exit /b %errorlevel%
)
echo Starting DLMS Server on http://localhost:8080...
java -cp "bin;lib/*" com.evoriea.dlms.web.DLMSWebServer 8080
pause
