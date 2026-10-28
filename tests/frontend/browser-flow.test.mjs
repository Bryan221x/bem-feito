import test from 'node:test';
import assert from 'node:assert/strict';
import { createServer } from 'node:http';
import { readFile, mkdir, writeFile } from 'node:fs/promises';
import { resolve, extname } from 'node:path';
import { spawn } from 'node:child_process';
import { randomUUID } from 'node:crypto';
import { createRequire } from 'node:module';

// Opt-in: real Spring Security, HTTP APIs and browser; all writes stay in ephemeral H2.
test('real browser: registration, approval, activation, management and public catalog', { skip: process.env.BROWSER_INTEGRATION_TESTS !== 'true', timeout: 240000 }, async () => {
  const require = createRequire(import.meta.url);
  const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
  const root = resolve('.'), backend = resolve(root, 'backend');
  const password = `Test-${randomUUID()}`, email = 'browser-admin@example.org';
  const cp = ['target/test-classes', 'target/classes', (await readFile(resolve(backend, 'target/test-classpath.txt'), 'utf8')).trim()].join(process.platform === 'win32' ? ';' : ':');
  const java = spawn('java', ['-cp', cp, 'com.bemfeito.api.BrowserTestServer'], { cwd: backend, windowsHide: true, env: { ...process.env, APP_BOOTSTRAP_ADMIN_NAME: 'Browser admin', APP_BOOTSTRAP_ADMIN_EMAIL: email, APP_BOOTSTRAP_ADMIN_PASSWORD: password }, stdio: ['ignore', 'pipe', 'pipe'] });
  let logs = ''; java.stdout.on('data', b => { logs += b; }); java.stderr.on('data', b => { logs += b; });
  const server = createServer(async (req, res) => {
    try {
      const pathname = decodeURIComponent(new URL(req.url, 'http://localhost').pathname);
      const file = resolve(root, '.' + (pathname === '/' ? '/index.html' : pathname));
      if (!file.startsWith(root + '/') && !file.startsWith(root + '\\')) throw new Error('path');
      let data = await readFile(file); const ext = extname(file);
      if (ext === '.html') data = Buffer.from(data.toString('utf8').replace('</head>', '<meta name="bemfeito-api-base" content="http://127.0.0.1:8081"></head>'));
      res.setHeader('Content-Type', ({ '.html': 'text/html; charset=utf-8', '.js': 'text/javascript', '.css': 'text/css', '.svg': 'image/svg+xml', '.png': 'image/png' })[ext] || 'application/octet-stream'); res.end(data);
    } catch { res.statusCode = 404; res.end(); }
  });
  let browser;
  try {
    await new Promise(r => server.listen(5501, '127.0.0.1', r));
    for (let i = 0; ; i++) {
      try { if (logs.includes('BEMFEITO_BROWSER_TEST_READY') && (await fetch('http://127.0.0.1:8081/api/states')).ok) break; } catch {}
      if (i > 90 || java.exitCode !== null) throw new Error('Test backend failed: ' + logs.slice(-5000));
      await new Promise(r => setTimeout(r, 500));
    }
    browser = await chromium.launch({ headless: true, ...(process.env.BROWSER_EXECUTABLE ? { executablePath: process.env.BROWSER_EXECUTABLE } : {}) });
    const errors = [], resources = [], consoleErrors = [], widths = [1440, 1366, 1024, 768, 390, 338], checked = [];
    const context = await browser.newContext(), page = await context.newPage();
    function observe(p) {
      p.setDefaultTimeout(15000);
      p.on('console', m => { if (m.type() === 'error' && !(m.location().url.includes('/api/auth/me') && m.text().includes('401'))) consoleErrors.push(m.text() + ' ' + m.location().url); });
      p.on('pageerror', e => { errors.push(e.message); console.log('Page error', e.message); });
      p.on('response', r => { if (r.url().startsWith('http://127.0.0.1:5501') && r.status() >= 400) resources.push(`${r.status()} ${r.url()}`); });
      p.on('dialog', d => d.accept());
    }
    observe(page);
    const base = 'http://127.0.0.1:5501';
    const goto = async (p, path) => { console.log("Visit", path); await p.goto(base + path); await p.waitForFunction(() => !document.documentElement.classList.contains('auth-pending')); };
    const api = (p, path, method = 'GET', body) => p.evaluate(async ({ path, method, body }) => (await import('/js/services/api-client.js')).apiRequest(path, { method, body }), { path, method, body });
    const login = async (p, mail) => { await goto(p, '/pages/institution-login.html'); await p.locator('[data-institution-email]').fill(mail); await p.locator('[data-institution-password]').fill(password); await p.locator('button[type=submit]').click(); await p.waitForURL(/(selection|dashboard)\.html/); };
    const responsive = async (p, name) => {
      for (const width of widths) { await p.setViewportSize({ width, height: 900 }); assert.ok(await p.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1), `${name}: horizontal overflow at ${width}`); }
      console.log("Responsive", name); checked.push(name); await p.setViewportSize({ width: 1366, height: 900 });
    };
    await goto(page, '/pages/registration-request.html');
    await page.waitForFunction(() => !document.querySelector('[name=category]').disabled);
    const fields = { displayName: 'Instituição de teste temporário', legalName: 'Instituição de teste temporário LTDA', document: '11222333000181', objective: 'Apoio comunitário', description: 'Cadastro exclusivo do teste em memória.', responsibleName: 'Responsável teste', responsibleRole: 'Direção', responsibleEmail: 'browser-owner@example.org', responsiblePhone: '11999999999', unitName: 'Sede', postalCode: '01001000', street: 'Praça da Sé', number: '1', neighborhood: 'Sé' };
    for (const [name, value] of Object.entries(fields)) await page.locator(`[data-registration-request-form] [name="${name}"]`).fill(value);
    await page.locator('[name=category]').selectOption({ index: 1 }); await page.locator('[name=state]').selectOption('SP');
    await page.waitForFunction(() => !document.querySelector('[name=city]').disabled); await page.locator('[name=city]').selectOption({ index: 1 });
    await page.locator('[data-public-contact-type]').selectOption('EMAIL'); await page.locator('[data-public-contact-value]').fill('public@example.org'); await page.locator('[data-add-public-contact]').click();
    await responsive(page, 'registration');
    await page.locator('[data-registration-request-form] button[type=submit]').click();
    await page.waitForFunction(() => document.querySelector('[data-registration-form-message]').textContent.includes('enviada com sucesso')).catch(async e => { throw new Error(await page.locator('[data-registration-form-message]').innerText()); });
    await login(page, email); await page.locator('[data-admin-dashboard-page]').waitFor(); await responsive(page, 'dashboard');
    await goto(page, '/pages/admin-requests.html'); await page.locator('button[data-request-details]').first().click();
    await page.locator('[data-request-decision=approve]').click(); await page.waitForFunction(() => document.querySelector('[data-invitation-url]').value.length > 0);
    const activation = await page.locator('[data-invitation-url]').inputValue(); await page.locator('[data-close-request-dialog]').click();
    await goto(page, '/pages/admin-institutions.html'); await page.waitForFunction(() => document.querySelectorAll('tbody tr').length > 0); await responsive(page, 'admin institutions');
    const adminList = await api(page, '/api/admin/institutions'); const id = adminList.content[0].id;
    const ownerContext = await browser.newContext(), owner = await ownerContext.newPage(); observe(owner);
    await owner.goto(activation); await owner.locator('[data-activation-form]').waitFor();
    await owner.locator('[name=password]').fill(password); await owner.locator('[name=confirmPassword]').fill(password); await owner.locator('button[type=submit]').click();
    await owner.waitForFunction(() => document.querySelector('[data-account-activation-page]').dataset.state === 'success');
    await login(owner, 'browser-owner@example.org'); await responsive(owner, 'institution selection');
    await owner.locator('label.institution-selection-card').first().click(); await owner.locator('a[href*="my-institution.html?institutionId="]').first().click();
    await owner.locator('[data-edit-general]').waitFor(); await owner.locator('[data-edit-general]').click(); await owner.locator('[name=displayName]').fill('Instituição atualizada');
    await owner.locator('[data-general-form] button[type=submit]').click(); await owner.waitForFunction(() => document.querySelector('[data-general-form]').hidden);
    await owner.locator('[data-edit-about]').click(); await owner.locator('[name=history]').fill('História real do fluxo de teste temporário.');
    const png = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a5xkAAAAASUVORK5CYII=', 'base64');
    await owner.locator('[data-history-image-input]').setInputFiles([{ name: 'history.png', mimeType: 'image/png', buffer: png }]);
    await owner.locator('[data-history-image-field=alt]').fill('Foto de teste acessível');
    await owner.locator('[data-about-form] button[type=submit]').click(); await owner.waitForFunction(() => document.querySelector('[data-about-form]').hidden);
    await responsive(owner, 'my institution');
    await owner.locator('[data-edit-about]').click();
    const webp = await owner.evaluate(() => { const c = document.createElement('canvas'); c.width = c.height = 2; c.getContext('2d').fillRect(0,0,2,2); return c.toDataURL('image/webp').split(',')[1]; });
    await owner.locator('[data-history-image-input]').setInputFiles([{ name: 'second.webp', mimeType: 'image/webp', buffer: Buffer.from(webp, 'base64') }]);
    await owner.locator('[data-history-image-field=alt]').last().fill('Segunda foto acessível');
    await owner.locator('[data-about-form] button[type=submit]').click(); await owner.waitForFunction(() => document.querySelector('[data-about-form]').hidden);
    await owner.locator('[data-add-contact]').click();
    await owner.locator('[data-contact-form] [name=value]').fill('private@example.org');
    await owner.locator('[data-contact-form] [name=label]').fill('Contato privado');
    await owner.locator('[data-contact-form] button[type=submit]').click(); await owner.waitForFunction(() => document.querySelector('[data-contact-form]').hidden);
    let managed = await api(owner, `/api/management/institutions/${id}`); assert.equal(managed.historyImages.length, 2); assert.equal(managed.displayName, 'Instituição atualizada');
    await goto(owner, `/pages/unit-form.html?institutionId=${id}`); await owner.waitForFunction(() => document.querySelector('[name=state]').options.length > 1);
    await owner.locator('[name=name]').fill('Segunda unidade'); await owner.locator('[data-next-step]').click();
    for (const [key, val] of Object.entries({ postalCode: '01001000', address: 'Praça da Sé', number: '2', neighborhood: 'Sé', latitude: '-23.55', longitude: '-46.63' })) await owner.locator(`[name=${key}]`).fill(val);
    await owner.locator('[name=state]').selectOption('SP'); await owner.waitForFunction(() => document.querySelector('[name=cityId]').options.length > 1); await owner.locator('[name=cityId]').selectOption({ index: 1 });
    await responsive(owner, 'unit address'); await owner.locator('[data-next-step]').click();
    await owner.locator('[data-service-area-options] label').first().click(); await responsive(owner, 'unit needs and hours');
    await owner.locator('[data-hours-day="1"] [data-hours-field=opensAt]').fill('09:00'); await owner.locator('[data-hours-day="1"] [data-hours-field=closesAt]').fill('16:30');
    await owner.locator('[data-submit-unit]').click(); await owner.waitForFunction(() => new URLSearchParams(location.search).has('unitId')).catch(async () => { throw new Error(await owner.locator('[data-unit-form-feedback]').innerText()); });
    const unitId = new URL(owner.url()).searchParams.get('unitId');
    await owner.locator('[data-add-unit-need]').click(); await owner.locator('[name=needName]').fill('Alimentos necessários'); await owner.locator('[name=needCategoryId]').selectOption({ index: 1 }); await owner.locator('[data-save-unit-need]').click(); await owner.waitForFunction(() => document.querySelector('[data-unit-need-editor]').hidden);
    await owner.waitForFunction(() => !document.querySelector('[data-unit-form-page]').hasAttribute('aria-busy'));
    const unit = await api(owner, `/api/management/units/${unitId}`); assert.equal(unit.needs.length, 1); assert.equal(unit.serviceAreas.length, 1); assert.equal(unit.openingHours[0].opensAt, '09:00:00');
    await goto(owner, `/pages/unit-form.html?institutionId=${id}&unitId=${unitId}`); await owner.waitForFunction(() => document.querySelector('[name=name]').value === 'Segunda unidade');
    const visitor = await browser.newPage(); observe(visitor); await goto(visitor, '/pages/cities.html'); await visitor.locator('a[href*="institutions.html?cityId="]').first().waitFor(); await responsive(visitor, 'cities');
    await visitor.locator('a[href*="institutions.html?cityId="]').first().click(); await visitor.locator('a.institution-card__link').first().waitFor(); await responsive(visitor, 'institutions'); await visitor.locator('a.institution-card__link').first().click();
    await visitor.waitForFunction(() => document.body.textContent.includes('Alimentos necessários')); await responsive(visitor, 'institution detail');
    assert.ok((await visitor.locator('body').innerText()).includes('Segunda unidade')); assert.ok(!(await visitor.locator('body').innerText()).includes('11222333000181'));
    assert.ok(!(await visitor.locator('body').innerText()).includes('private@example.org'));
    await visitor.locator('[data-institution-history-open]').click(); await responsive(visitor, 'history gallery');
    await visitor.locator('[data-history-next]').click(); assert.match(await visitor.locator('[data-history-counter]').innerText(), /2/); await visitor.keyboard.press('ArrowLeft'); assert.match(await visitor.locator('[data-history-counter]').innerText(), /1/); await visitor.keyboard.press('Escape');
    assert.equal(await visitor.locator('[data-institution-history-dialog]').evaluate(d => d.open), false);
    await mkdir(resolve(root, 'backend/target/browser-results'), { recursive: true });
    await visitor.screenshot({ path: resolve(root, 'backend/target/browser-results/institution.png'), fullPage: true });

    // Independent needs mutations remain persisted after reloading the editor.
    await owner.locator('[data-next-step]').click(); await owner.locator('[data-next-step]').click();
    await owner.locator('[data-need-action=edit]').first().click(); await owner.locator('[name=needStatus]').selectOption('FULFILLED'); await owner.locator('[data-save-unit-need]').click(); await owner.waitForFunction(() => document.querySelector('[data-unit-need-editor]').hidden);
    assert.equal((await api(owner, `/api/management/units/${unitId}`)).needs[0].status, 'FULFILLED');
    assert.equal((await api(visitor, `/api/institutions/${id}`)).units.find(u => String(u.id) === unitId).needs.length, 0);
    await owner.locator('[data-need-action=edit]').first().click(); await owner.locator('[name=needStatus]').selectOption('INACTIVE'); await owner.locator('[data-save-unit-need]').click(); await owner.waitForFunction(() => document.querySelector('[data-unit-need-editor]').hidden);
    await owner.locator('[data-need-action=remove]').first().click(); await owner.waitForFunction(() => !document.querySelector('[data-unit-needs-empty]').hidden);
    // 1 / 0 photos and units use real persisted changes, not route interception.
    await api(owner, `/api/institutions/${id}/history-images/${managed.historyImages[1].id}`, 'DELETE');
    await goto(visitor, `/pages/institution.html?id=${id}`); await visitor.locator('[data-institution-history-open]').click(); assert.equal(await visitor.locator('[data-institution-history-navigation]').isVisible(), false); await visitor.keyboard.press('Escape');
    await api(owner, `/api/institutions/${id}/history-images/${managed.historyImages[0].id}`, 'DELETE');
    await api(owner, `/api/units/${unitId}`, 'PATCH', { status: 'INACTIVE' });
    await goto(visitor, `/pages/institution.html?id=${id}`); await visitor.locator('[data-institution-history-open]').click(); assert.equal(await visitor.locator('[data-institution-history-gallery]').isVisible(), false); await visitor.keyboard.press('Escape');
    assert.equal(await visitor.locator('.institution-unit-card').count(), 1);
    await api(owner, `/api/units/${managed.units[0].id}`, 'PATCH', { status: 'INACTIVE' });
    await goto(visitor, `/pages/institution.html?id=${id}`); await visitor.locator('[data-institution-units-empty]').waitFor();
    await page.locator('[data-institution-details]').first().click(); await page.locator('.api-detail-dialog h3').first().waitFor(); await responsive(page, 'admin detail dialog');
    await page.getByLabel('Status global').selectOption('SUSPENDED'); await page.getByRole('button', { name: 'Salvar status' }).click();
    await page.waitForFunction(() => document.querySelector('.api-detail-dialog [role=status]').textContent.includes('salvas'));
    assert.equal((await api(page, `/api/management/institutions/${id}`)).status, 'SUSPENDED');
    assert.equal((await api(visitor, '/api/institutions')).totalElements, 0);
    assert.deepEqual(errors, []); assert.deepEqual(resources, []); assert.deepEqual(consoleErrors, []);
    await writeFile(resolve(root, 'backend/target/browser-results/report.json'), JSON.stringify({ checked, widths, pageErrors: errors, localResourceErrors: resources, consoleErrors }, null, 2));
  } finally {
    await browser?.close(); await new Promise(r => server.close(r)); java.kill();
  }
});
