const { test } = require("node:test");
const assert = require("node:assert/strict");
const { spawn } = require("node:child_process");
const { once } = require("node:events");
const WebSocket = require("ws");

const port = 19000 + Math.floor(Math.random() * 1000);
let child;

async function waitForHealth() {
  for (let i = 0; i < 60; i++) {
    try {
      const response = await fetch(`http://127.0.0.1:${port}/healthz`);
      if (response.ok) return;
    } catch {}
    await new Promise(resolve => setTimeout(resolve, 100));
  }
  throw new Error("relay did not become healthy");
}

test("health endpoint and two-peer signaling relay", async t => {
  child = spawn(process.execPath, ["src/index.js"], {
    cwd: __dirname + "/..",
    env: { ...process.env, PORT: String(port) },
    stdio: "ignore"
  });
  t.after(() => { if (child && !child.killed) child.kill("SIGTERM"); });
  await waitForHealth();
  const health = await (await fetch(`http://127.0.0.1:${port}/healthz`)).json();
  assert.equal(health.service, "akira-link-relay");

  const room = "A".repeat(43);
  const connect = role => new WebSocket(`ws://127.0.0.1:${port}/v1/signal?room=${room}&role=${role}`);
  const phone = connect("phone");
  const desktop = connect("desktop");
  await Promise.all([once(phone, "open"), once(desktop, "open")]);

  const phoneReady = once(phone, "message");
  const desktopReady = once(desktop, "message");
  const [pReady, dReady] = await Promise.all([phoneReady, desktopReady]);
  assert.equal(JSON.parse(pReady[0].toString()).type, "ready");
  assert.equal(JSON.parse(dReady[0].toString()).type, "ready");

  const forwarded = once(phone, "message");
  desktop.send(JSON.stringify({ type: "offer", data: { sdp: "test-offer" } }));
  const [raw] = await forwarded;
  const payload = JSON.parse(raw.toString());
  assert.equal(payload.type, "offer");
  assert.equal(payload.from, "desktop");
  assert.equal(payload.data.sdp, "test-offer");

  phone.close();
  desktop.close();
});
