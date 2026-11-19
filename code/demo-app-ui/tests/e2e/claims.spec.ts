import { test, expect } from '@playwright/test';

const CLAIMANT = {
  email: 'claimant@demo.com',
  password: 'Claimant123!',
};

const ADMIN = {
  email: 'admin@demo.com',
  password: 'Admin123!',
};

async function login(page: import('@playwright/test').Page, email: string, password: string) {
  await page.goto('/login', { waitUntil: 'domcontentloaded', timeout: 60_000 });
  await page.getByLabel('Email').fill(email);
  await page.locator('#password').fill(password);
  const loginResponsePromise = page.waitForResponse((response) =>
    response.url().includes('/api/auth/login')
  );
  await page.getByRole('button', { name: 'Log In' }).click();
  await loginResponsePromise;
}

test.describe('Claims user journey', () => {
  test('claimant can log in and submit a claim', async ({ page }) => {
    await login(page, CLAIMANT.email, CLAIMANT.password);

    await expect(page).toHaveURL(/\/claims(?:\/)?$/);
    await expect(page.getByRole('heading', { name: 'My Claims' })).toBeVisible();
    await expect(page.getByText('Loading your claims...')).not.toBeVisible({ timeout: 30_000 });

    await page.goto('/claims/new');
    await expect(page).toHaveURL(/\/claims\/new$/, { timeout: 30_000 });

    await page.getByLabel(/When did the incident occur/).fill('2026-11-18');
    await page.getByLabel(/Where did the incident occur/).fill('123 Test Street, Singapore');
    await page.getByLabel(/Claim Amount/).fill('2500');
    await page.getByTestId('wizard-step-1').getByRole('button', { name: 'Next' }).click();

    await expect(page.getByTestId('wizard-step-2')).toBeVisible();
    await page.getByLabel(/Describe what happened/).fill(
      'A Playwright test vehicle incident caused damage to the insured property.'
    );
    await page.getByTestId('wizard-step-2').getByRole('button', { name: 'Next' }).click();

    await expect(page.getByTestId('wizard-step-3')).toBeVisible();
    await expect(page.getByText('$2,500.00')).toBeVisible();
    await page.getByTestId('wizard-step-3').getByRole('button', { name: 'Submit Claim' }).click();

    await expect(page).toHaveURL(/\/claims(?:\/)?$/);
    await expect(page.getByText(/Claim submitted successfully/i)).toBeVisible();
  });

  test('invalid login shows an inline error', async ({ page }) => {
    await login(page, CLAIMANT.email, 'WrongPassword123!');

    await expect(page.getByText('Invalid email or password', { exact: true })).toBeVisible();
  });

  test('protected route redirects to login', async ({ page }) => {
    await page.goto('/claims', { waitUntil: 'domcontentloaded', timeout: 60_000 });
    await expect(page).toHaveURL(/\/login/);
  });
});

test.describe('Admin claims management', () => {
  test('admin can view claims and update a submitted claim status', async ({ page }) => {
    await login(page, ADMIN.email, ADMIN.password);

    await expect(page).toHaveURL(/\/admin\/dashboard$/, { timeout: 30_000 });
    await page.goto('/admin/claims');
    await expect(page).toHaveURL(/\/admin\/claims$/);
    await expect(page.getByRole('heading', { name: 'Claims Management' })).toBeVisible();

    const submittedRow = page.getByRole('row').filter({ hasText: 'Submitted' }).first();
    await expect(submittedRow).toBeVisible({ timeout: 30_000 });

    const statusSelect = submittedRow.locator('select');
    await statusSelect.selectOption({ label: 'Under Review' });

    const confirmation = page.getByRole('dialog', { name: 'Confirm Status Change' });
    await expect(confirmation).toBeVisible();
    await confirmation.getByRole('button', { name: 'Confirm' }).click();

    await expect(page.getByText(/Claim status updated to Under Review/i)).toBeVisible();
  });
});
