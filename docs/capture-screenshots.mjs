// Capture UI screenshots with headless Edge (no extra dependencies).
// Usage: node docs/capture-screenshots.mjs

import { spawn, spawnSync } from 'node:child_process'
import { mkdir, rm, writeFile } from 'node:fs/promises'
import os from 'node:os'
import path from 'node:path'

const EDGE = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe'
const PORT = 9333
const BASE = 'http://localhost:5173'
const OUT = path.join(process.cwd(), 'docs', 'screenshots')
const PROFILE = path.join(os.tmpdir(), `lims-shot-${Date.now()}`)

const defaultPages = [
  { name: 'active-projects', path: '/service/current', wait: 3000 },
  { name: 'inspection-input', path: '/inspection/input', wait: 2500 },
  { name: 'report-download', path: '/sample/report-download', wait: 3000 },
]

// Optional argument: "name:/route,name2:/route2"
const pages = process.argv[2]
  ? process.argv[2].split(',').map((item) => {
      const index = item.indexOf(':')
      return { name: item.slice(0, index), path: item.slice(index + 1), wait: 2500 }
    })
  : defaultPages

// Optional third argument: click the first button/link containing this text.
const clickText = process.argv[3]
// Optional fourth argument: "nologin" keeps the page unauthenticated.
const noLogin = process.argv[4] === 'nologin'
// Optional fifth argument: a small UI action to run before capture.
const uiAction = process.argv[5]

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

async function waitJson(url, tries = 60) {
  for (let i = 0; i < tries; i++) {
    try {
      const response = await fetch(url)
      if (response.ok) return await response.json()
    } catch {}
    await sleep(200)
  }
  throw new Error(`CDP not ready: ${url}`)
}

let ws
let nextId = 1
const pending = new Map()

function send(method, params = {}) {
  const id = nextId++
  ws.send(JSON.stringify({ id, method, params }))
  return new Promise((resolve, reject) => pending.set(id, { resolve, reject }))
}

async function main() {
  await mkdir(OUT, { recursive: true })

  const edge = spawn(
    EDGE,
    [
      `--remote-debugging-port=${PORT}`,
      `--user-data-dir=${PROFILE}`,
      '--headless=new',
      '--disable-gpu',
      '--no-first-run',
      '--no-default-browser-check',
      '--window-size=1440,900',
      'about:blank',
    ],
    { stdio: 'ignore' },
  )

  try {
    await waitJson(`http://127.0.0.1:${PORT}/json/version`)
    const target = await fetch(`http://127.0.0.1:${PORT}/json/new?about:blank`, {
      method: 'PUT',
    }).then((response) => response.json())

    ws = new WebSocket(target.webSocketDebuggerUrl)
    await new Promise((resolve, reject) => {
      ws.onopen = resolve
      ws.onerror = reject
    })
    ws.onmessage = (event) => {
      const message = JSON.parse(event.data)
      const waiter = pending.get(message.id)
      if (!waiter) return
      pending.delete(message.id)
      if (message.error) waiter.reject(new Error(JSON.stringify(message.error)))
      else waiter.resolve(message.result)
    }

    await send('Page.enable')
    await send('Runtime.enable')
    await send('Emulation.setDeviceMetricsOverride', {
      width: 1440,
      height: 900,
      deviceScaleFactor: 1,
      mobile: false,
    })

    if (!noLogin) {
      await send('Page.navigate', { url: `${BASE}/login` })
      await sleep(2500)
      const login = await send('Runtime.evaluate', {
        expression: `(async () => {
          const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: 'nj001', password: 'nj001' }),
          });
          const body = await response.json();
          if (body.code === 0) {
            localStorage.setItem('lims_access_token', body.data.token);
          }
          return body.code;
        })()`,
        awaitPromise: true,
        returnByValue: true,
      })
      console.log('login code:', login.result.value)
    }

    for (const page of pages) {
      await send('Page.navigate', { url: BASE + page.path })
      await sleep(page.wait)
      if (clickText) {
        await send('Runtime.evaluate', {
          expression: `(() => {
            const target = [...document.querySelectorAll('button, a')]
              .find((el) => el.textContent.includes(${JSON.stringify(clickText)}));
            if (target) { target.click(); return true; }
            return false;
          })()`,
          returnByValue: true,
        })
        await sleep(1200)
      }
      if (uiAction === 'open-ai' || uiAction === 'ask-ai') {
        await send('Runtime.evaluate', {
          expression: `(() => {
            const ball = document.querySelector('.ai-ball');
            if (!ball) return 'no-ball';
            ball.dispatchEvent(new PointerEvent('pointerdown', {
              bubbles: true, button: 0,
              clientX: window.innerWidth - 80,
              clientY: window.innerHeight - 140,
            }));
            window.dispatchEvent(new PointerEvent('pointerup', { bubbles: true, button: 0 }));
            return 'opened';
          })()`,
          returnByValue: true,
        })
        await sleep(800)
        if (uiAction === 'ask-ai') {
          await send('Runtime.evaluate', {
            expression: `(() => {
              const chip = [...document.querySelectorAll('.ai-chips button')]
                .find((item) => item.textContent.includes('待安排有多少'));
              if (!chip) return 'no-chip';
              chip.click();
              return 'asked';
            })()`,
            returnByValue: true,
          })
          await sleep(2500)
        }
      }
      if (uiAction === 'context-ai') {
        await send('Runtime.evaluate', {
          expression: `(() => {
            const ball = document.querySelector('.ai-ball');
            if (!ball) return 'no-ball';
            ball.dispatchEvent(new PointerEvent('pointerdown', {
              bubbles: true, button: 0,
              clientX: window.innerWidth - 80,
              clientY: window.innerHeight - 140,
            }));
            window.dispatchEvent(new PointerEvent('pointerup', { bubbles: true, button: 0 }));
            return 'opened';
          })()`,
          returnByValue: true,
        })
        await sleep(800)
        for (const text of ['查样品 JK(2023)-SA-004', '打开页面']) {
          await send('Runtime.evaluate', {
            expression: `(() => {
              const input = document.querySelector('.ai-input input');
              if (!input) return 'no-input';
              const setter = Object.getOwnPropertyDescriptor(
                window.HTMLInputElement.prototype, 'value'
              ).set;
              setter.call(input, ${JSON.stringify(text)});
              input.dispatchEvent(new Event('input', { bubbles: true }));
              return 'typed';
            })()`,
            returnByValue: true,
          })
          await sleep(400)
          await send('Runtime.evaluate', {
            expression: `(() => {
              const button = document.querySelector('.ai-send');
              if (!button || button.disabled) return 'send-disabled';
              button.click();
              return 'sent';
            })()`,
            returnByValue: true,
          })
          await sleep(2500)
        }
      }
      const shot = await send('Page.captureScreenshot', {
        format: 'png',
        captureBeyondViewport: false,
      })
      await writeFile(path.join(OUT, `${page.name}.png`), Buffer.from(shot.data, 'base64'))
      console.log('saved:', page.name)
    }
  } finally {
    try {
      ws?.close()
    } catch {}
    spawnSync('taskkill', ['/PID', String(edge.pid), '/T', '/F'], { stdio: 'ignore' })
    await sleep(500)
    await rm(PROFILE, { recursive: true, force: true }).catch(() => {})
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
