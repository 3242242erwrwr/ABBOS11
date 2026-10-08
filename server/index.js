const express = require('express');
const http = require('http');
const WebSocket = require('ws');

const app = express();
app.use(express.json({ limit: '10mb' }));

// Enable CORS for all mobile networks and origin domains
app.use((req, res, next) => {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type');
    if (req.method === 'OPTIONS') {
        return res.sendStatus(200);
    }
    next();
});

const server = http.createServer(app);
const wss = new WebSocket.Server({ server, path: '/ws' });

let connectedClients = new Set();
let recentSosMessages = []; // Store only recent SOS emergency messages for HTTP polling fallback

wss.on('connection', (ws) => {
    connectedClients.add(ws);
    console.log(`[XABAR-SOS] Client connected. Total active clients: ${connectedClients.size}`);

    // Send welcome ping
    try {
        ws.send(JSON.stringify({ type: 'ping', timestamp: Date.now() }));
    } catch (e) {}

    ws.on('message', (data) => {
        try {
            const parsed = JSON.parse(data.toString());

            const typeStr = parsed.type || '';
            const isCallOrVoice = typeStr === 'ping' || typeStr.startsWith('call_') || typeStr.startsWith('voice_');

            if (isCallOrVoice) {
                // Live broadcast only - DO NOT store in SOS message buffer!
                broadcastLive(parsed);
            } else if (parsed.messageText) {
                // Actual SOS Emergency Message
                saveAndBroadcastSos(parsed);
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

function broadcastLive(payloadObj) {
    const jsonString = typeof payloadObj === 'string' ? payloadObj : JSON.stringify(payloadObj);
    connectedClients.forEach((client) => {
        if (client.readyState === WebSocket.OPEN) {
            try {
                client.send(jsonString);
            } catch (e) {}
        }
    });
}

function saveAndBroadcastSos(messageObj) {
    if (!messageObj.timestamp) {
        messageObj.timestamp = Date.now();
    }

    // 1. Broadcast FULL message instantly via WebSocket
    broadcastLive(messageObj);

    // 2. For REST polling cache, store a lightweight version (without heavy audioData) to prevent 4G bandwidth choking!
    const lightObj = { ...messageObj };
    delete lightObj.audioData;

    recentSosMessages.unshift(lightObj);
    if (recentSosMessages.length > 50) {
        recentSosMessages = recentSosMessages.slice(0, 50);
    }
}

// REST API Broadcast
app.post('/api/sos', (req, res) => {
    const sosData = req.body;

    if (sosData) {
        const typeStr = sosData.type || '';
        if (typeStr.startsWith('call_') || typeStr.startsWith('voice_')) {
            broadcastLive(sosData);
        } else if (sosData.messageText) {
            saveAndBroadcastSos(sosData);
        }
        return res.json({ status: 'ok', broadcastedTo: connectedClients.size });
    }
    return res.status(400).json({ error: 'Invalid payload' });
});

// REST API Polling Fallback (Get recent SOS messages)
app.get('/api/sos/recent', (req, res) => {
    res.json({ messages: recentSosMessages.slice(0, 20), serverTime: Date.now() });
});

app.get('/', (req, res) => {
    res.send(`
        <html>
            <body style="font-family: Arial, sans-serif; background: #121212; color: #fff; text-align: center; padding: 50px;">
                <h1 style="color: #ff3d00;">🚨 XABAR SOS Server Active</h1>
                <p>Status: Running on Render (24/7 Always Active)</p>
                <p>Connected Active Android Devices: <strong>${connectedClients.size}</strong></p>
                <p>Total Cached SOS Messages: <strong>${recentSosMessages.length}</strong></p>
                <p>WebSocket Path: <code>/ws</code></p>
            </body>
        </html>
    `);
});

const PORT = process.env.PORT || 3000;
server.listen(PORT, () => {
    console.log(`[XABAR-SOS] Server started on port ${PORT}`);
});
