const express = require('express');
const http = require('http');
const WebSocket = require('ws');

const app = express();
app.use(express.json());

const server = http.createServer(app);
const wss = new WebSocket.Server({ server, path: '/ws' });

let connectedClients = new Set();

wss.on('connection', (ws) => {
    connectedClients.add(ws);
    console.log(`[XABAR-SOS] Client connected. Total active clients: ${connectedClients.size}`);

    ws.on('message', (data) => {
        try {
            const parsed = JSON.parse(data.toString());
            console.log(`[XABAR-SOS] Received SOS message:`, parsed);

            // Broadcast to ALL connected devices
            broadcastMessage(parsed);
        } catch (e) {
            console.error('[XABAR-SOS] Error parsing message:', e.message);
        }
    });

    ws.on('close', () => {
        connectedClients.delete(ws);
        console.log(`[XABAR-SOS] Client disconnected. Remaining: ${connectedClients.size}`);
    });

    ws.on('error', (err) => {
        console.error('[XABAR-SOS] WebSocket client error:', err.message);
    });
});

function broadcastMessage(messageObj) {
    const jsonString = JSON.stringify(messageObj);
    connectedClients.forEach((client) => {
        if (client.readyState === WebSocket.OPEN) {
            client.send(jsonString);
        }
    });
}

// REST API Fallback
app.post('/api/sos', (req, res) => {
    const sosData = req.body;
    console.log('[XABAR-SOS] HTTP POST received SOS:', sosData);

    if (sosData && (sosData.messageText || sosData.senderName)) {
        broadcastMessage(sosData);
        return res.json({ status: 'ok', broadcastedTo: connectedClients.size });
    }
    return res.status(400).json({ error: 'Invalid SOS payload' });
});

app.get('/', (req, res) => {
    res.send(`
        <html>
            <body style="font-family: Arial, sans-serif; background: #121212; color: #fff; text-align: center; padding: 50px;">
                <h1 style="color: #ff3d00;">🚨 XABAR SOS Server Active</h1>
                <p>Status: Running on Render</p>
                <p>Connected Active Android Devices: <strong>${connectedClients.size}</strong></p>
                <p>WebSocket Path: <code>/ws</code></p>
            </body>
        </html>
    `);
});

const PORT = process.env.PORT || 3000;
server.listen(PORT, () => {
    console.log(`[XABAR-SOS] Server started on port ${PORT}`);
});
