# AKIRA LINK — signaling relay (phase 1)

This service is the first server-side building block for cross-network AKIRA REMOTE connections. It brokers **WebRTC signaling only**; it does not carry screen video, remote-control commands, chat, or files yet.

## What it does

- HTTP health check at `GET /healthz`.
- WebSocket signaling endpoint at `/v1/signal?room=<invite>&role=phone|desktop`.
- Keeps at most one phone and one desktop peer in a room.
- Forwards only `offer`, `answer`, `ice-candidate`, `renegotiate`, and `bye` messages.
- Keeps room state in memory only; no message or SDP/ICE payload logging or persistence.
- Expires idle rooms after 10 minutes.

## Security notes

- Generate a cryptographically random invite value with at least 32 random bytes (base64url, typically 43 characters). Share it only with the intended second device.
- Use **HTTPS/WSS** in production, terminated by a trusted reverse proxy or hosting platform. Do not expose this service as plain ws:// over the public internet.
- This initial relay is a signaling prototype, not a complete production security boundary. Invite expiry, device identity, user approval, abuse throttling, and TURN credentials still need to be integrated before general release.
- Never put permanent server secrets in the Android APK or Windows client.
- WebRTC media encryption (DTLS-SRTP) is separate from the signaling channel and must be configured and validated in the client.

## Run locally

```sh
cd server
npm install
npm start
```

The service listens on port 8787 by default. Test health with `curl http://localhost:8787/healthz`.

## Deployment

Build the Docker image from this directory and deploy it to a host that supports persistent WebSocket connections. Configure the host's TLS endpoint to proxy WebSocket upgrades to container port 8787. Set the public client URL to the resulting `wss://...` address when client integration is added.

A TURN server (for example coturn with time-limited credentials) is a separate deployment step and is required for reliable connectivity on restrictive NAT/firewall networks.
