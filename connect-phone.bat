@echo off
setlocal
rem Connect the phone to the local dev server via an adb reverse tunnel.
rem Run this once every time you plug the phone in - the tunnel dies with the
rem USB connection. It maps the phone's localhost:8080 to this PC's port 8080,
rem so the app keeps working even when the Wi-Fi changes: no IP edit, no rebuild.
rem
rem Kept ASCII-only on purpose: cmd reads .bat files in the system codepage,
rem and non-ASCII text here breaks parsing on some machines.

set "PORT=8080"
set "ADB="

where adb >nul 2>nul && set "ADB=adb"
if not defined ADB if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
if not defined ADB if exist "%ANDROID_HOME%\platform-tools\adb.exe" set "ADB=%ANDROID_HOME%\platform-tools\adb.exe"

if not defined ADB (
    echo [FAIL] adb not found. Check the Android SDK platform-tools location.
    exit /b 1
)

echo [1/3] Checking for a connected device
"%ADB%" devices | findstr /r /c:"device$" >nul
if errorlevel 1 (
    echo.
    echo [FAIL] No device connected.
    "%ADB%" devices
    echo.
    echo   - Is the USB cable plugged in?
    echo   - Approve the "Allow USB debugging" prompt on the phone.
    echo   - If it says "unauthorized", unlock the phone and approve the prompt.
    exit /b 1
)

echo [2/3] Opening tunnel: phone localhost:%PORT% to PC %PORT%
"%ADB%" reverse tcp:%PORT% tcp:%PORT%
if errorlevel 1 (
    echo [FAIL] Could not open the tunnel.
    exit /b 1
)

echo [3/3] Asking the phone to reach the server
"%ADB%" shell curl -s --max-time 5 http://localhost:%PORT%/health
echo.
echo.
echo If you see status:ok above, you are ready.
echo If the line is empty, the server is not running:  gradlew.bat :server:run
endlocal
