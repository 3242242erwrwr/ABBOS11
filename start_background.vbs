Set WshShell = CreateObject("WScript.Shell")
WshShell.Run "cmd.exe /c cd /d E:\ANDROD PRAJEKT\XABARSOS2\XABARSOS2\XABARSOS\server && node index.js", 0, False
WshShell.Run "cmd.exe /c npx cloudflared tunnel --url http://localhost:3000", 0, False
