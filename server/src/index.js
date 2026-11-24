"use strict";

const http = require("node:http");
const { WebSocketServer, WebSocket } = require("ws");

const PORT = Number(process.env.PORT || 8787);
const MAX_MESSAGE_BYTES = 64 * 1024;
const ROOM_IDLE_MS = 10 * 60 * 1000;
const ALLOWED_ROLES = new Set(["phone", "desktop"]);
const ALLOWED_TYPES = new Set(["offer", "answer", "ice-candidate", "renegotiate", "bye"]);
const rooms = new Map();

function validRoomCode(value) {
  return typeof value === "string" && /^[A-Za-z0-9_-]{40,64}$/.test(value);
}

function getRoom(code) {
  let room = rooms.get(code);
  if (!room) {
    room = { peers: new Map(), touchedAt: Date.now() };
    rooms.set(code, room);
  }
  room.touchedAt = Date.now();
  return room;
}

function sendJson(socket, payload) {
  if (socket.readyState === WebSocket.OPEN) socket.send(JSON.stringify(payload));
}

const server = http.createServer((req, res) => {
  if (req.method === "GET" && req.url === "/healthz") {
    res.writeHead(200, { "content-type": "application/json", "cache-control": "no-store" });
    return res.end(JSON.stringify({ status: "ok", service: "akira-link-relay" }));
  }
  res.writeHead(404, { "content-type": "application/json", "cache-control": "no-store" });
  res.end(JSON.stringify({ error: "not_found" }));
});

const wss = new WebSocketServer({
  noServer: true,
  maxPayload: MAX_MESSAGE_BYTES,
  perMessageDeflate: false
});

server.on("upgrade", (req, socket, head) => {
  let url;
  try { url = new URL(req.url, "http://localhost"); } catch {
    socket.destroy();
    return;
  }
  if (url.pathname !== "/v1/signal") {
    socket.write("HTTP/1.1 404 Not Found\r\n\r\n");
    socket.destroy();
    return;
  }
  const roomCode = url.searchParams.get("room");
  const role = url.searchParams.get("role");
  if (!validRoomCode(roomCode) || !ALLOWED_ROLES.has(role)) {
    socket.write("HTTP/1.1 400 Bad Request\r\n\r\n");
    socket.destroy();
    return;
  }
  wss.handleUpgrade(req, socket, head, ws => {
    ws.roomCode = roomCode;
    ws.role = role;
    wss.emit("connection", ws);
  });
});

wss.on("connection", ws => {
  const room = getRoom(ws.roomCode);
  if (room.peers.has(ws.role) || room.peers.size >= 2) {
    sendJson(ws, { type: "error", code: "room_full" });
    ws.close(1008, "room full");
    return;
  }

  room.peers.set(ws.role, ws);
  sendJson(ws, { type: "ready", role: ws.role, peerConnected: room.peers.size === 2 });
  for (const [role, peer] of room.peers) {
    if (peer !== ws) sendJson(peer, { type: "peer-joined", role: ws.role });
  }

  ws.on("message", raw => {
    let message;
    try { message = JSON.parse(raw.toString()); } catch {
      sendJson(ws, { type: "error", code: "invalid_json" });
      return;
    }
    if (!message || typeof message !== "object" || !ALLOWED_TYPES.has(message.type)) {
      sendJson(ws, { type: "error", code: "unsupported_message" });
      return;
    }
    const peerRole = ws.role === "phone" ? "desktop" : "phone";
    const peer = room.peers.get(peerRole);
    if (!peer) {
      sendJson(ws, { type: "error", code: "peer_not_connected" });
      return;
    }
    room.touchedAt = Date.now();
    // Forward signaling only. The relay does not inspect, store, or log SDP/ICE payloads.
    sendJson(peer, { type: message.type, data: message.data ?? null, from: ws.role });
  });

  ws.on("close", () => {
    const current = rooms.get(ws.roomCode);
    if (!current) return;
    if (current.peers.get(ws.role) === ws) current.peers.delete(ws.role);
    for (const peer of current.peers.values()) sendJson(peer, { type: "peer-left", role: ws.role });
    current.touchedAt = Date.now();
    if (current.peers.size === 0) rooms.delete(ws.roomCode);
  });
  ws.on("error", () => {});
});

const cleanup = setInterval(() => {
  const now = Date.now();
  for (const [code, room] of rooms) {
    if (room.peers.size === 0 || now - room.touchedAt > ROOM_IDLE_MS) {
      for (const peer of room.peers.values()) peer.close(1001, "room expired");
      rooms.delete(code);
    }
  }
}, 60_000);
cleanup.unref();

server.listen(PORT, "0.0.0.0", () => {
  console.log(`AKIRA LINK relay listening on :${PORT}`);
});

function shutdown() {
  clearInterval(cleanup);
  for (const peer of wss.clients) peer.close(1001, "server shutdown");
  server.close(() => process.exit(0));
}
process.on("SIGTERM", shutdown);
process.on("SIGINT", shutdown);
