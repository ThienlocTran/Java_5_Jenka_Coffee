@echo off
echo Checking Java compilation...
cd /d "%~dp0"
call mvnw.cmd clean compile -DskipTests -e
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ========================================
    echo COMPILATION FAILED
    echo ========================================
    exit /b 1
)
echo.
echo ========================================
echo COMPILATION SUCCESSFUL
echo ========================================
exit /b 0
