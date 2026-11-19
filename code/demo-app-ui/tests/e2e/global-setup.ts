import { chromium, FullConfig } from '@playwright/test';

export default async function globalSetup(config: FullConfig) {
  const baseURL = config.projects[0]?.use?.baseURL ?? 'http://localhost:3001';
  const browser = await chromium.launch();
  const page = await browser.newPage();

  const response = await page.request.get(`${baseURL}/login`, {
    timeout: 60_000,
  });

  if (!response.ok()) {
    throw new Error(`UI is not ready at ${baseURL}: HTTP ${response.status()}`);
  }

  await browser.close();
}
