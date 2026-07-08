@echo off
REM 本地开发链路一键启动（getData -> simulate -> backend）
REM 双击即用；绕过 PowerShell 执行策略限制。
cd /d "%~dp0"
powershell -ExecutionPolicy Bypass -File "%~dp0start-local.ps1"
