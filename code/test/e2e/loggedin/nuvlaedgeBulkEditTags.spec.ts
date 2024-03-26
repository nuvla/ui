import { test, expect } from '@playwright/test';

test('Edges selection and bulk edits', async ({ page, context }, { project, config }) => {
  const { baseURL } = config.projects[0].use;
  await page.goto(baseURL + '/ui/welcome');
  await page.waitForResponse((resp) => resp.url().includes('get-subscription'));
  await page.getByRole('link', { name: 'Edges' }).click();

  const edgesPageRegex = /\/ui\/edges/;

  let selectAll = async () => page.getByRole('cell', { name: 'select all on page' }).click();
  let selectFirst = async () => page.locator("td[aria-label='select row 0']").click();
  let editTagsModal = async () => page.getByText('Edit Tags').click();
  let closeDropDown = async () => page.getByText('Bulk update of tags').click();

  await expect(page).toHaveURL(edgesPageRegex);

   // Remove multiple tags on one edge
  await selectAll();
  await editTagsModal();

  await page.getByText('Remove all tags').click();
  await page.getByRole('button', { name: 'edit tags' }).click();
  await page.getByRole('button', { name: 'Yes: Remove all tags' }).click();

  // ---------- TESTING WITH SELECTION BY CHECKBOXES -----------
  // Add one tag on all edges
  await selectAll();
  await editTagsModal();
  await page.waitForTimeout(500);
  await page.locator('div[role="combobox"] input[type="text"]').fill('AddTagTest');
  await page.locator('div[role="combobox"] input[type="text"]').press('Enter');
  await closeDropDown();
  await page.getByRole('button', { name: 'edit tags' }).click();
  await page.getByRole('button', { name: 'Yes: Add tags' }).click();

  const allNuvlaEdgesCount = await page.getByRole('link', { name: /select row/i }).count();


  // ASSERTION 1
  await expect(page.getByTestId('bulk-edit-success-message')).toHaveText(allNuvlaEdgesCount +' Edges updated with operation: Add tags');
  await expect(page.getByRole('link').filter({ hasText: 'AddTagTest' })).toHaveCount(allNuvlaEdgesCount);

  // Set multiple tags on one edge
  await selectFirst();
  await editTagsModal();
  await page.waitForTimeout(500);
  await page.locator('div[role="combobox"] input[type="text"]').fill('SetTagTest1');
  await page.locator('div[role="combobox"] input[type="text"]').press('Enter');
  await page.locator('div[role="combobox"] input[type="text"]').fill('SetTagTest2');
  await page.locator('div[role="combobox"] input[type="text"]').press('Enter');
  await page.locator('div[role="combobox"] input[type="text"]').fill('SetTagTest3');
  await page.locator('div[role="combobox"] input[type="text"]').press('Enter');
  await closeDropDown();
  await page.getByText('Set tags (overwrites all current tags!)').click();
  await page.getByRole('button', { name: 'edit tags' }).click();
  await page.getByRole('button', { name: 'Yes: Set tags' }).click();

  // ASSERTION 2
  await expect(page.getByText('1 Edge updated with operation: Set tags', { exact: true })).toBeVisible();
  await page.waitForTimeout(500);
  await expect(page.getByRole('link').filter({ hasText: 'SetTagTest1' })).toHaveCount(1);
  await expect(page.getByRole('link').filter({ hasText: 'SetTagTest2' })).toHaveCount(1);
  await expect(page.getByRole('link').filter({ hasText: 'SetTagTest3' })).toHaveCount(1);

  // Remove multiple tags on one edge

  await selectFirst();
  await editTagsModal();


  await page.waitForTimeout(500);
  await page.locator('div[role="combobox"] input[type="text"]').fill('SetTagTest1');
  await page.locator('div[role="combobox"] input[type="text"]').press('Enter');
  await page.locator('div[role="combobox"] input[type="text"]').fill('SetTagTest2');
  await page.locator('div[role="combobox"] input[type="text"]').press('Enter');
  await closeDropDown();

  await page.getByText('Remove specific tags').click();
  await page.getByRole('button', { name: 'edit tags' }).click();
  await page.getByRole('button', { name: 'Yes: Remove specific tags' }).click();

  // ASSERTION 3
  await expect(page.getByText('1 Edge updated with operation: Remove specific tags')).toBeVisible();
  await page.waitForTimeout(500);
  await expect(page.getByRole('link').filter({ hasText: 'SetTagTest1' })).toHaveCount(0);
  await expect(page.getByRole('link').filter({ hasText: 'SetTagTest2' })).toHaveCount(0);
  await expect(page.getByRole('link').filter({ hasText: 'SetTagTest3' })).toHaveCount(1);

  // Remove all tags on all edges
  await selectAll();
  await editTagsModal();

  await page.getByText('Remove all tags').click();
  await page.getByRole('button', { name: 'edit tags' }).click();
  await page.getByRole('button', { name: 'Yes: Remove all tags' }).click();

  // ASSERTION 4
  await expect(page.getByText(allNuvlaEdgesCount + ' Edges updated with operation: Remove all tags')).toBeVisible();
  await page.waitForTimeout(500);
  await expect(page.getByRole('link').filter({ hasText: 'AddTagTest' })).toHaveCount(0);

  // ---------- TESTING WITH SELECTION BY SELECT ALL AND FILTER TEXT -----------
  // Add one tag on all edges
  let selectAllEmptySearch = async () => {
    await page.getByPlaceholder('Search ...').fill('');
    await page.getByText(/Select all \d*/).click();
  };
  let selectAllSearchDonot = async () => {
    await page.getByPlaceholder('Search ...').fill('Do_not');
    await page.getByText(/Select all \d*/).click();
  };
  await selectAllEmptySearch();
  await editTagsModal();
  await page.waitForTimeout(500);
  await page.locator('div[role="combobox"] input[type="text"]').fill('AddTagTest');
  await page.locator('div[role="combobox"] input[type="text"]').press('Enter');
  await closeDropDown();
  await page.getByRole('button', { name: 'edit tags' }).click();
  await page.getByRole('button', { name: 'Yes: Add tags' }).click();


  // ASSERTION 1
  await expect(page.getByText(/Edges updated with operation: Remove all tags/)).toBeVisible();
  await page.waitForTimeout(500);
  await expect(page.getByRole('link').filter({ hasText: 'AddTagTest' })).toHaveCount(allNuvlaEdgesCount);

  // Set multiple tags on one edge

  await selectAllSearchDonot();
  await editTagsModal();

  await page.waitForTimeout(500);
  await page.locator('div[role="combobox"] input[type="text"]').fill('SetTagTest1');
  await page.locator('div[role="combobox"] input[type="text"]').press('Enter');
  await page.locator('div[role="combobox"] input[type="text"]').fill('SetTagTest2');
  await page.locator('div[role="combobox"] input[type="text"]').press('Enter');
  await page.locator('div[role="combobox"] input[type="text"]').fill('SetTagTest3');
  await page.locator('div[role="combobox"] input[type="text"]').press('Enter');
  await closeDropDown();
  await page.getByText('Set tags (overwrites all current tags!)').click();
  await page.getByRole('button', { name: 'edit tags' }).click();
  await page.getByRole('button', { name: 'Yes: Set tags' }).click();

  // ASSERTION 2
  await expect(page.getByText('1 Edge updated with operation: Set tags', {exact: true})).toBeVisible();
  await page.waitForTimeout(500);
  await expect(page.getByRole('link').filter({ hasText: 'SetTagTest1' })).toHaveCount(1);
  await expect(page.getByRole('link').filter({ hasText: 'SetTagTest2' })).toHaveCount(1);
  await expect(page.getByRole('link').filter({ hasText: 'SetTagTest3' })).toHaveCount(1);
  await page.getByPlaceholder('Search ...').fill('');
  await expect(page.getByRole('link').filter({ hasText: 'AddTagTest' })).toHaveCount(allNuvlaEdgesCount - 1);

  // Remove multiple tags on one edge
  await selectAllSearchDonot();
  await editTagsModal();

  await page.waitForTimeout(500);
  await page.locator('div[role="combobox"] input[type="text"]').fill('SetTagTest1');
  await page.locator('div[role="combobox"] input[type="text"]').press('Enter');
  await page.locator('div[role="combobox"] input[type="text"]').fill('SetTagTest2');
  await page.locator('div[role="combobox"] input[type="text"]').press('Enter');
  await closeDropDown();
  await page.getByText('Remove specific tags').click();
  await page.getByRole('button', { name: 'edit tags' }).click();
  await page.getByRole('button', { name: 'Yes: Remove specific tags' }).click();
  await page.getByPlaceholder('Search ...').fill('');

  // ASSERTION 3
  await expect(page.getByText('1 Edge updated with operation: Remove specific tags')).toBeVisible();
  await page.waitForTimeout(500);
  await expect(page.getByRole('link').filter({ hasText: 'SetTagTest1' })).toHaveCount(0);
  await expect(page.getByRole('link').filter({ hasText: 'SetTagTest2' })).toHaveCount(0);
  await expect(page.getByRole('link').filter({ hasText: 'SetTagTest3' })).toHaveCount(1);
  await expect(page.getByRole('link').filter({ hasText: 'AddTagTest' })).toHaveCount(allNuvlaEdgesCount - 1);

  // Remove multiple tags on one edge
  await selectAllEmptySearch();
  await editTagsModal();

  await page.getByText('Remove all tags').click();
  await page.getByRole('button', { name: 'edit tags' }).click();
  await page.getByRole('button', { name: 'Yes: Remove all tags' }).click();

  // ASSERTION 4
  await expect(page.getByText(allNuvlaEdgesCount + ' Edges updated with operation: Remove all tags')).toBeVisible();
  await page.waitForTimeout(500);
  await expect(page.getByRole('link').filter({ hasText: 'AddTagTest' })).toHaveCount(0);
});
