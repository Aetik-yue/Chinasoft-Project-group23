@echo off
chcp 65001 >nul
cd /d "%~dp0"
REM 优先用 PowerShell 7 (pwsh),它对中文支持最好;没有就用 Windows PowerShell,
REM 并自动给 config.ps1 补上 UTF-8 BOM,防止中文乱码。
where pwsh >nul 2>nul
if %errorlevel%==0 (
    pwsh -NoProfile -ExecutionPolicy Bypass -File "%~dp0config.ps1"
) else (
    powershell -NoProfile -ExecutionPolicy Bypass -Command "$p='%~dp0config.ps1'; [IO.File]::WriteAllText($p,[IO.File]::ReadAllText($p),[Text.UTF8Encoding]::new($true)); & $p"
)
echo.
pause
