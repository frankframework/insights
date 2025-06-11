import { chromium } from 'playwright';

const browser = await chromium.launch();
const page = await browser.newPage();

await page.goto('http://localhost:4200');
await page.waitForTimeout(5000);

await browser.close();
