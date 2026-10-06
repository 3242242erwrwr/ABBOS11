const express = require('express');
const http = require('http');
const WebSocket = require('ws');

const app = express();
app.use(express.json());

const server = http.createServer(app);
const wss = new WebSocket.Server({ server, path: '/ws' });

let connectedClients = new Set();
let recentSosMessages = []; // Store recent messages for HTTP polling fallback

wss.on('connection', (ws) => {
    connectedClients.add(ws);
    console.log(`[XABAR-SOS] Client connected. Total active clients: ${connectedClients.size}`);

    // Send welcome ping
    ws.send(JSON.stringify({ type: 'ping', timestamp: Date.now() }));

    ws.on('message', (data) => {
        try {
            const parsed = JSON.parse(data.toString());
            console.log(`[XABAR-SOS] Received SOS message:`, parsed);

            if (parsed.messageText || parsed.senderName) {
                saveAndBroadcast(parsed);
            }
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

// Server Heartbeat interval (5 seconds) to prevent Render/Carrier socket timeout
setInterval(() => {
    connectedClients.forEach((ws) => {
        if (ws.readyState === WebSocket.OPEN) {
            try {
                ws.ping();
            } catch (e) {}
        }
    });
}, 5000);

function saveAndBroadcast(messageObj) {
    if (!messageObj.timestamp) {
        messageObj.timestamp = Date.now();
    }

    // Keep last 50 messages
    recentSosMessages.unshift(messageObj);
    if (recentSosMessages.length > 50) {
        recentSosMessages = recentSosMessages.slice(0, 50);
    }

    const jsonString = JSON.stringify(messageObj);
    connectedClients.forEach((client) => {
        if (client.readyState === WebSocket.OPEN) {
            try {
                client.send(jsonString);
            } catch (e) {}
        }
    });
}

// REST API Broadcast
app.post('/api/sos', (req, res) => {
    const sosData = req.body;
    console.log('[XABAR-SOS] HTTP POST received SOS:', sosData);

    if (sosData && (sosData.messageText || sosData.senderName)) {
        saveAndBroadcast(sosData);
        return res.json({ status: 'ok', broadcastedTo: connectedClients.size });
    }
    return res.status(400).json({ error: 'Invalid SOS payload' });
});

// REST API Polling Fallback (Get recent messages since timestamp)
app.get('/api/sos/recent', (req, res) => {
    const since = parseInt(req.query.since || '0', 10);
    const newMessages = recentSosMessages.filter(m => (m.timestamp || 0) > since);
    res.json({ messages: newMessages, serverTime: Date.now() });
});

app.get('/', (req, res) => {
    res.send(`
        <html>
            <body style="font-family: Arial, sans-serif; background: #121212; color: #fff; text-align: center; padding: 50px;">
                <h1 style="color: #ff3d00;">🚨 XABAR SOS Server Active</h1>
                <p>Status: Running on Render (24/7 Always Active)</p>
                <p>Connected Active Android Devices: <strong>${connectedClients.size}</strong></p>
                <p>Total Cached Messages: <strong>${recentSosMessages.length}</strong></p>
                <p>WebSocket Path: <code>/ws</code></p>
            </body>
        </html>
    `);
});

const PORT = process.env.PORT || 3000;
server.listen(PORT, () => {
    console.log(`[XABAR-SOS] Server started on port ${PORT}`);
});
