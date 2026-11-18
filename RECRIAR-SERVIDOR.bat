@echo off
setlocal
chcp 65001 >nul
cd /d "%~dp0"
title Template Zyon v2 - Discord
where py >nul 2>nul
if not errorlevel 1 (
    py -3 iniciar.py --recriar
    goto fim
)
where python >nul 2>nul
if not errorlevel 1 (
    python iniciar.py --recriar
    goto fim
)
echo Instale Python 3.12 para Windows:
echo https://www.python.org/downloads/windows/
echo Marque "Add python.exe to PATH" e execute novamente.
:fim
echo.
pause
