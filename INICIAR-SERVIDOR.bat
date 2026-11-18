@echo off
setlocal
cd /d "%~dp0"
title Minecraft 1.8.9 - Servidor Local
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0iniciar.ps1"
echo.
pause
