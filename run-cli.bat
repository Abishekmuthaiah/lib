@echo off
echo =====================================================================
echo  Digital Library Management System (DLMS) - Interactive CLI
echo  Evoriea Infotech Pvt. Ltd. | Abishek Muthaiah S K (95192301003)
echo =====================================================================
echo Compiling Java source files...
if not exist "bin" mkdir bin
javac -cp "lib/*" -d bin src/com/evoriea/dlms/config/*.java src/com/evoriea/dlms/model/*.java src/com/evoriea/dlms/dao/*.java src/com/evoriea/dlms/service/*.java src/com/evoriea/dlms/data/*.java src/com/evoriea/dlms/presentation/*.java
if errorlevel 1 (
    echo Compilation failed!
    pause
    exit /b %errorlevel%
)
echo Launching DLMS Terminal Application...
java -cp "bin;lib/*" com.evoriea.dlms.presentation.MainApplication
pause
