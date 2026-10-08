@echo off
title XABAR SOS Local Server
color 0A
echo ========================================
echo   XABAR SOS Local Server Initialization
echo ========================================
cd server
echo Installing dependencies...
call npm install
echo.
echo Starting Node.js server...
node index.js
pause
