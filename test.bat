@echo off
echo =====================================================================
echo  Digital Library Management System (DLMS) - Test Suite
echo =====================================================================
if not exist "bin" mkdir bin
javac -cp "lib/*" -d bin src/com/evoriea/dlms/config/*.java src/com/evoriea/dlms/model/*.java src/com/evoriea/dlms/dao/*.java src/com/evoriea/dlms/service/*.java src/com/evoriea/dlms/data/*.java src/com/evoriea/dlms/test/*.java
if errorlevel 1 (
    echo Compilation failed!
    pause
    exit /b %errorlevel%
)
echo Running DLMSTestSuite...
java -cp "bin;lib/*" com.evoriea.dlms.test.DLMSTestSuite
pause
