// One-off script to capture real screenshots of the running admin panel
// for the README. Not part of the app build — run manually:
//   node scripts/screenshot.mjs
import { chromium } from 'playwright'
import { mkdirSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import path from 'node:path'

const OUT_DIR = fileURLToPath(new URL('../../docs/screenshots/', import.meta.url))
mkdirSync(OUT_DIR, { recursive: true })
const outPath = (name) => path.join(OUT_DIR, name)

const EMAIL = 'rahul@example.com'
const PASSWORD = 'password123'

const VIEWPORT = { width: 1280, height: 800 }
const browser = await chromium.launch()
const page = await browser.newPage({ viewport: VIEWPORT })

async function screenshotContent(name) {
  // Crop to the actual rendered content instead of the full (mostly empty)
  // viewport — .content/.app-shell stretch to min-height:100vh, so their own
  // boundingBox() is useless for this; measure the last real element instead.
  const authCard = page.locator('.auth-card')
  if (await authCard.count()) {
    const box = await authCard.boundingBox()
    const pad = 32
    await page.screenshot({
      path: outPath(name),
      clip: {
        x: Math.max(0, box.x - pad),
        y: 0,
        width: Math.min(VIEWPORT.width, box.width + pad * 2),
        height: Math.min(VIEWPORT.height, box.y + box.height + pad),
      },
    })
    return
  }
  const table = page.locator('table').first()
  const box = await table.boundingBox()
  const pad = 32
  await page.screenshot({
    path: outPath(name),
    clip: { x: 0, y: 0, width: VIEWPORT.width, height: Math.min(VIEWPORT.height, box.y + box.height + pad) },
  })
}

// --- Login page ---
await page.goto('http://localhost:5173/login')
await page.waitForSelector('h1:has-text("Admin Login")')
await screenshotContent('login.png')

// --- Log in ---
await page.fill('input[type="email"]', EMAIL)
await page.fill('input[type="password"]', PASSWORD)
await page.click('button[type="submit"]')
await page.waitForSelector('h1:has-text("Products")')
await page.waitForTimeout(300) // let the products table finish rendering
await screenshotContent('products.png')

// --- Categories page ---
await page.click('a:has-text("Categories")')
await page.waitForSelector('h1:has-text("Categories")')
await page.waitForTimeout(300)
await screenshotContent('categories.png')

console.log('Screenshots written to', OUT_DIR)
await browser.close()
