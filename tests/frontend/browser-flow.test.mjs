import test from 'node:test';
import assert from 'node:assert/strict';
import { createServer } from 'node:http';
import { createServer as createSmtpServer } from 'node:net';
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
  await mkdir(resolve(root, 'backend/target/browser-results'), { recursive: true });
  const password = `Test-${randomUUID()}`, email = 'browser-admin@example.org';
  const mails=[];
  const smtp=createSmtpServer(socket=>{
    socket.setEncoding('utf8');socket.write('220 local-capture\r\n');let buffer='', data=false, body='';
    socket.on('data',chunk=>{buffer+=chunk;let index;
      while((index=buffer.indexOf('\r\n'))>=0) { const line=buffer.slice(0,index);buffer=buffer.slice(index+2);
        if(data) { if(line==='.') {mails.push(body.replace(/=\r\n/g,'').replace(/=3D/g,'='));body='';data=false;socket.write('250 accepted\r\n');} else body+=line+'\r\n'; }
        else if(line.startsWith('DATA')) {data=true;socket.write('354 send data\r\n');}
        else if(line.startsWith('QUIT')) socket.end('221 bye\r\n');
        else socket.write('250 OK\r\n');
      }
    });
  });
  await new Promise(resolve=>smtp.listen(0,'127.0.0.1',resolve));
  const mailLink=async path=>{
    for(let i=0;i<100;i++) {
      const index=mails.findIndex(body=>body.includes(path+'#token='));
      if(index>=0) {const body=mails.splice(index,1)[0];const token=body.split(path+'#token=')[1].match(/^[A-Za-z0-9_-]{43}/)?.[0];assert.ok(token);return 'http://127.0.0.1:5501/pages/'+path+'#token='+token;}
      await new Promise(resolve=>setTimeout(resolve,50));
    }
    throw new Error('E-mail esperado não capturado localmente');
  };
  const cp = ['target/test-classes', 'target/classes', (await readFile(resolve(backend, 'target/test-classpath.txt'), 'utf8')).trim()].join(process.platform === 'win32' ? ';' : ':');
  const java = spawn('java', ['-cp', cp, 'com.bemfeito.api.BrowserTestServer'], { cwd: backend, windowsHide: true, env: { ...process.env, TEST_ADMIN_NAME: 'Browser admin', TEST_ADMIN_EMAIL: email, TEST_ADMIN_PASSWORD: password, TEST_SMTP_PORT: String(smtp.address().port) }, stdio: ['ignore', 'pipe', 'pipe'] });
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
    async function observe(p) {
      // Only tile transport is stubbed: avoid automated pan/zoom traffic to community tile servers.
      await p.route('https://tile.openstreetmap.org/**', route=>route.fulfill({contentType:'image/png',body:Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a5xkAAAAASUVORK5CYII=','base64')}));
      p.setDefaultTimeout(15000);
      p.on('console', m => { if (m.type() === 'error' && !(m.location().url.includes('/api/auth/me') && m.text().includes('401'))) consoleErrors.push(m.text() + ' ' + m.location().url); });
      p.on('pageerror', e => { errors.push(e.message); console.log('Page error', e.message); });
      p.on('response', r => { if (r.url().startsWith('http://127.0.0.1:5501') && r.status() >= 400) resources.push(`${r.status()} ${r.url()}`); });
      p.on('dialog', d => d.accept());
    }
    await observe(page);
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
    await page.locator('[data-registration-request-form] button[type=submit]').click();
    assert.equal(await page.locator('[data-registration-request-form]').evaluate(form => form.checkValidity()), false);
    assert.equal(await page.locator('[name=displayName]').getAttribute('aria-invalid'), 'true');
    assert.equal(await page.locator('[name=displayName]').evaluate(input => input === document.activeElement), true);
    assert.match(await page.locator('#error-displayName').innerText(), /Preencha/);
    assert.equal(await page.evaluate(() => !!(document.querySelector('[name=state]').compareDocumentPosition(document.querySelector('[name=city]')) & Node.DOCUMENT_POSITION_FOLLOWING)), true);
    const fields = { displayName: 'Instituição de teste temporário', legalName: 'Instituição de teste temporário LTDA', document: '11222333000181', objective: 'Apoio comunitário', description: 'Texto longo de teste em memória. '.repeat(28), responsibleName: 'Responsável teste', responsibleRole: 'Direção', responsibleEmail: 'browser-owner@example.org', responsiblePhone: '11999999999', unitName: 'Sede', postalCode: '01001000', street: 'Praça da Sé', number: '1', neighborhood: 'Sé' };
    for (const [name, value] of Object.entries(fields)) await page.locator(`[data-registration-request-form] [name="${name}"]`).fill(value);
    assert.equal(await page.locator('[name=displayName]').getAttribute('aria-invalid'), null);
    await page.locator('[name=category]').selectOption({ index: 1 }); await page.locator('[name=state]').selectOption('SP');
    await page.waitForFunction(() => !document.querySelector('[name=city]').disabled); await page.locator('[name=city]').selectOption({ index: 1 });
    await page.locator('[data-public-contact-type]').selectOption('WHATSAPP'); await page.locator('[data-public-contact-value]').fill('11999999999'); await page.locator('[data-add-public-contact]').click();
    await responsive(page, 'registration');
    await page.locator('[data-registration-request-form] button[type=submit]').click();
    await page.waitForFunction(() => document.querySelector('[data-registration-form-message]').textContent.includes('enviada com sucesso')).catch(async e => { throw new Error(await page.locator('[data-registration-form-message]').innerText()); });
    assert.equal(await page.locator('[data-registration-form-message]').evaluate(el => getComputedStyle(el).color), 'rgb(36, 88, 61)');
    await login(page, email); await page.locator('[data-admin-dashboard-page]').waitFor(); await responsive(page, 'dashboard');
    await goto(page, '/pages/admin-requests.html'); await page.locator('button[data-request-details]').first().click();
    await page.locator('[data-request-decision=approve]').click(); await page.waitForFunction(() => !document.querySelector('[data-request-dialog]').open);
    assert.match(await page.locator('[data-request-notice]').innerText(), /servidor de e-mail/);
    const activation = await mailLink('account-activation.html'); assert.equal(await page.locator('[data-request-dialog]').evaluate(d => d.open), false);
    await goto(page, '/pages/admin-institutions.html'); await page.waitForFunction(() => document.querySelectorAll('tbody tr').length > 0); await responsive(page, 'admin institutions');
    const adminList = await api(page, '/api/admin/institutions'); const id = adminList.content[0].id;
    const ownerContext = await browser.newContext(), owner = await ownerContext.newPage(); await observe(owner);
    await owner.goto(activation); await owner.locator('[data-activation-form]').waitFor();
    await owner.locator('[name=password]').fill(password); await owner.locator('[name=confirmPassword]').fill(password); await owner.locator('button[type=submit]').click();
    await owner.waitForFunction(() => document.querySelector('[data-account-activation-page]').dataset.state === 'success');
    await login(owner, 'browser-owner@example.org'); await responsive(owner, 'institution selection');
    await owner.locator('label.institution-selection-card').first().click(); await owner.locator('a[href*="my-institution.html?institutionId="]').first().click();
    await owner.locator('[data-edit-general]').waitFor(); await owner.locator('[data-edit-general]').click(); await owner.locator('[name=displayName]').fill('Instituição atualizada');
    await owner.locator('[data-general-form] button[type=submit]').click(); await owner.waitForFunction(() => document.querySelector('[data-general-form]').hidden);
    await owner.locator('[data-edit-about]').click(); await owner.locator('[name=history]').fill('História real do fluxo de teste temporário.');
    await owner.locator('[data-about-form] button[type=submit]').click(); await owner.waitForFunction(() => document.querySelector('[data-about-form]').hidden);
    const png = Buffer.from(await owner.evaluate(() => {
      const canvas = document.createElement('canvas'); canvas.width = 640; canvas.height = 360;
      const ctx = canvas.getContext('2d'); ctx.fillStyle = '#397653'; ctx.fillRect(0, 0, 640, 360);
      ctx.fillStyle = '#ffffff'; ctx.font = '32px sans-serif'; ctx.fillText('Imagem do teste isolado', 60, 190);
      return canvas.toDataURL('image/png').split(',')[1];
    }), 'base64');
    assert.equal(await owner.locator('[data-images-form]').isVisible(), true);
    await owner.locator('[data-history-image-input]').setInputFiles([{ name: 'invalid.txt', mimeType: 'text/plain', buffer: Buffer.from('invalid') }]);
    assert.match(await owner.locator('[data-my-institution-feedback]').innerText(), /JPEG, PNG ou WebP/);
    assert.equal(await owner.locator('[data-history-image-field=alt]').count(), 0);
    await owner.locator('[data-history-image-input]').setInputFiles([{ name: 'history.png', mimeType: 'image/png', buffer: png }]);
    await owner.locator('[data-images-form] button[type=submit]').click();
    assert.equal(await owner.locator('[data-history-image-field=alt]').getAttribute('aria-invalid'), 'true');
    await owner.locator('[data-history-image-field=alt]').fill('Foto de teste acessível');
    // Saving text must preserve an independent, unsaved image draft.
    await owner.locator('[data-edit-about]').click(); await owner.locator('[name=history]').fill('História longa em memória. '.repeat(160));
    await owner.locator('[data-about-form] button[type=submit]').click(); await owner.waitForFunction(() => document.querySelector('[data-about-form]').hidden);
    assert.equal(await owner.locator('[data-history-image-field=alt]').inputValue(), 'Foto de teste acessível');
    await owner.locator('[data-images-form] button[type=submit]').click(); await owner.waitForFunction(() => document.querySelector('[data-my-institution-feedback]').textContent === 'Alterações salvas.');
    assert.equal(await owner.locator('[data-history-image-action=remove]').evaluate(el => getComputedStyle(el).color), 'rgb(180, 35, 24)');
    await responsive(owner, 'my institution');
    await owner.locator('#images').screenshot({ path: resolve(root, 'backend/target/browser-results/images.png') });

    const webp = await owner.evaluate(() => { const c = document.createElement('canvas'); c.width = c.height = 2; c.getContext('2d').fillRect(0,0,2,2); return c.toDataURL('image/webp').split(',')[1]; });
    await owner.locator('[data-history-image-input]').setInputFiles([{ name: 'second.webp', mimeType: 'image/webp', buffer: Buffer.from(webp, 'base64') }]);
    await owner.locator('[data-history-image-field=alt]').last().fill('Segunda foto acessível');
    await owner.locator('[data-images-form] button[type=submit]').click(); await owner.waitForFunction(() => document.querySelector('[data-my-institution-feedback]').textContent === 'Alterações salvas.');
    await owner.locator('[data-add-contact]').click();
    await owner.locator('[data-contact-form] [name=value]').fill('private@example.org');
    await owner.locator('[data-contact-form] [name=label]').fill('Contato privado');
    await owner.locator('[data-contact-form] button[type=submit]').click(); await owner.waitForFunction(() => document.querySelector('[data-contact-form]').hidden);
    let managed = await api(owner, `/api/management/institutions/${id}`); assert.equal(managed.historyImages.length, 2); assert.equal(managed.displayName, 'Instituição atualizada');
    await goto(owner, `/pages/unit-form.html?institutionId=${id}`); await owner.waitForFunction(() => document.querySelector('[name=state]').options.length > 1);
    await owner.locator('[name=name]').fill('Segunda unidade'); await owner.locator('[data-next-step]').click();
    for (const [key, val] of Object.entries({ postalCode: '01001000', address: 'Praça da Sé', number: '2', neighborhood: 'Sé', latitude: '-23.55', longitude: '-46.63' })) await owner.locator(`[name=${key}]`).fill(val);
    await owner.locator('[name=state]').selectOption('SP'); await owner.waitForFunction(() => document.querySelector('[name=cityId]').options.length > 1); await owner.locator('[name=cityId]').selectOption({ index: 1 });
    assert.equal(await owner.evaluate(() => !!(document.querySelector('[name=state]').compareDocumentPosition(document.querySelector('[name=cityId]')) & Node.DOCUMENT_POSITION_FOLLOWING)), true);
    await owner.locator('[name=latitude]').fill('-23.55'); await owner.locator('[name=longitude]').fill('-46.63'); await owner.locator('[data-confirm-location]').click();
    await responsive(owner, 'unit address'); await owner.locator('[data-next-step]').click();
    await owner.locator('[data-service-area-options] label').first().click(); await responsive(owner, 'unit needs and hours');
    await owner.locator('[data-hours-day="1"] [data-hours-field=opensAt]').fill('09:00'); await owner.locator('[data-hours-day="1"] [data-hours-field=closesAt]').fill('16:30');
    await owner.locator('[data-submit-unit]').click(); await owner.waitForFunction(() => new URLSearchParams(location.search).has('unitId')).catch(async () => { throw new Error(await owner.locator('[data-unit-form-feedback]').innerText()); });
    const unitId = new URL(owner.url()).searchParams.get('unitId');
    await owner.locator('[data-add-unit-need]').click(); await owner.locator('[name=needName]').fill('Alimentos necessários'); await owner.locator('[name=needCategoryId]').selectOption({ index: 1 }); await owner.locator('[data-save-unit-need]').click(); await owner.waitForFunction(() => document.querySelector('[data-unit-need-editor]').hidden);
    await owner.waitForFunction(() => !document.querySelector('[data-unit-form-page]').hasAttribute('aria-busy'));
    const unit = await api(owner, `/api/management/units/${unitId}`); assert.equal(unit.needs.length, 1); assert.equal(unit.serviceAreas.length, 1); assert.equal(unit.openingHours[0].opensAt, '09:00:00');
    await goto(owner, `/pages/my-institution.html?institutionId=${id}`);
    await owner.locator(`a[href*="unitId=${unitId}"][href$="#needs"]`).click();
    await owner.locator('[data-add-unit-need]').waitFor();
    assert.equal(await owner.locator('.unit-form__step[data-unit-step="3"]').isVisible(),true);
    await responsive(owner,'direct needs management');
    await goto(owner, `/pages/unit-form.html?institutionId=${id}&unitId=${unitId}`); await owner.waitForFunction(() => document.querySelector('[name=name]').value === 'Segunda unidade');
    await owner.locator('[data-next-step]').click();
    await owner.locator('[data-open-location]').click();
    await owner.locator('[data-location-picker] .leaflet-marker-icon').waitFor();
    await owner.locator('[data-location-picker]').click({position:{x:180,y:140}});
    await owner.locator('[data-confirm-location]').click();
    const picked=Number(await owner.locator('[name=latitude]').inputValue());
    await owner.locator('[data-next-step]').click();await owner.locator('[data-submit-unit]').click();
    await owner.waitForFunction(()=>document.querySelector('[data-unit-form-feedback]').textContent==='Alterações salvas.');
    assert.equal((await api(owner,`/api/management/units/${unitId}`)).latitude,picked);
    await goto(owner,`/pages/unit-form.html?unitId=${unitId}`);await owner.waitForFunction(()=>document.querySelector('[name=name]').value==='Segunda unidade');
    assert.equal(new URL(owner.url()).searchParams.get('institutionId'),String(id));
    const visitor = await browser.newPage(); await observe(visitor); await goto(visitor, '/pages/cities.html'); await visitor.locator('a[href*="institutions.html?cityId="]').first().waitFor(); await responsive(visitor, 'cities');
    await visitor.locator('a[href*="institutions.html?cityId="]').first().click(); await visitor.locator('a.institution-card__link').first().waitFor(); await responsive(visitor, 'institutions');
    assert.equal(await visitor.locator('.institution-card__media img').evaluate(img => img.complete && img.naturalWidth > 0), true);
    assert.match(await visitor.locator('.institution-card__contact').getAttribute('href'), /^https:\/\/wa.me\//);
    assert.equal(await visitor.locator('.institution-card__description').evaluate(p => getComputedStyle(p).webkitLineClamp), '3');
    await visitor.screenshot({ path: resolve(root, 'backend/target/browser-results/cards.png'), fullPage: true });
    await visitor.setViewportSize({ width: 390, height: 900 });
    await visitor.screenshot({ path: resolve(root, 'backend/target/browser-results/cards-mobile.png'), fullPage: true });
    await visitor.locator('a.institution-card__link').first().click();
    await visitor.waitForFunction(() => document.body.textContent.includes('Alimentos necessários')); await responsive(visitor, 'institution detail');
    assert.match(await visitor.locator('[data-donation-needs]').innerText(),/Alimentos necessários/);
    assert.match(await visitor.locator('[data-donation-needs]').innerText(),/Quer ajudar/);
    await visitor.getByText('Ver localização desta unidade',{exact:true}).click();
    await visitor.locator('.institution-unit-card .leaflet-marker-icon').waitFor();
    assert.ok(await visitor.getByText('Como chegar',{exact:true}).count()>0);
    await responsive(visitor,'unit location');
    await visitor.locator('[data-donation-needs]').screenshot({path:resolve(root,'backend/target/browser-results/donation-needs.png')});
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
    await visitor.reload();await visitor.locator('[data-donation-needs]').waitFor();
    assert.match(await visitor.locator('[data-donation-needs]').innerText(),/ainda não informou necessidades atuais/);
    await owner.locator('[data-need-action=edit]').first().click(); await owner.locator('[name=needStatus]').selectOption('INACTIVE'); await owner.locator('[data-save-unit-need]').click(); await owner.waitForFunction(() => document.querySelector('[data-unit-need-editor]').hidden);
    await owner.locator('[data-need-action=remove]').first().click(); await owner.waitForFunction(() => !document.querySelector('[data-unit-needs-empty]').hidden);
    // 1 / 0 photos and units use real persisted changes, not route interception.
    await goto(owner, `/pages/my-institution.html?institutionId=${id}`);
    await owner.locator('[data-history-image-action=remove]').last().click();
    await owner.locator('[data-images-form] button[type=submit]').click();
    await owner.waitForFunction(() => document.querySelector('[data-my-institution-feedback]').textContent === 'Alterações salvas.');
    assert.equal((await api(owner, `/api/management/institutions/${id}`)).historyImages.length, 1);
    await goto(visitor, `/pages/institution.html?id=${id}`); await visitor.locator('[data-institution-history-open]').click(); assert.equal(await visitor.locator('[data-institution-history-navigation]').isVisible(), false); await visitor.keyboard.press('Escape');
    await api(owner, `/api/institutions/${id}/history-images/${managed.historyImages[0].id}`, 'DELETE');
    await api(owner, `/api/units/${unitId}`, 'PATCH', { status: 'INACTIVE' });
    await goto(visitor, `/pages/institution.html?id=${id}`); await visitor.locator('[data-institution-history-open]').click(); assert.equal(await visitor.locator('[data-institution-history-gallery]').isVisible(), false); await visitor.keyboard.press('Escape');
    assert.equal(await visitor.locator('.institution-unit-card').count(), 1);
    await api(owner, `/api/units/${managed.units[0].id}`, 'PATCH', { status: 'INACTIVE' });
    await goto(visitor, `/pages/institution.html?id=${id}`); await visitor.locator('[data-institution-units-empty]').waitFor();
    // Additional institution and roles go through public requests and administrative approval.
    const send = async (ctx, path, method = 'GET', data) => {
      const csrfResponse = await ctx.request.get('http://127.0.0.1:8081/api/auth/csrf');
      const token = await csrfResponse.json();
      return ctx.request.fetch('http://127.0.0.1:8081' + path, {
        method, data, headers: { [token.headerName]: token.token }
      });
    };
    const secondRequest = await send(ownerContext, '/api/registration-requests', 'POST', {
      categoryId: managed.categoryId, displayName: 'Segunda instituição temporária', legalName: 'Segunda instituição temporária',
      cnpj: '11444777000161', objective: null, description: null, history: null, website: null,
      responsible: { name: 'Responsável teste', email: 'browser-owner@example.org', phone: '11999999999', role: 'Direção' },
      publicContacts: [{ type: 'EMAIL', value: 'public@example.org', label: null, isPrimary: true }], mainUnit: { name: 'Sede', document: null, description: null, cityId: unit.address.cityId,
        postalCode: '01001000', street: 'Praça da Sé', number: '3', complement: null, neighborhood: 'Sé', contacts: [] }
    });
    assert.equal(secondRequest.status(), 201, await secondRequest.text());
    const secondRequestId = (await secondRequest.json()).id;
    const decisions = await Promise.all([1, 2].map(() => send(context, `/api/admin/registration-requests/${secondRequestId}/approve`, 'POST', { notes: null, membershipRole: null })));
    assert.deepEqual(decisions.map(r => r.status()).sort(), [200, 409]);
    const secondId = (await decisions.find(r => r.status() === 200).json()).institutionId;
    assert.equal((await api(owner, '/api/me/institutions')).length, 2);
    await goto(visitor, '/pages/institutions.html');
    await visitor.locator(`.institution-card[data-institution-id="${secondId}"]`).waitFor();
    const emptyCard = visitor.locator(`.institution-card[data-institution-id="${secondId}"]`);
    assert.equal(await emptyCard.locator('img').count(), 0);
    assert.equal(await emptyCard.locator('.institution-card__contact').count(), 0);
    assert.equal(await emptyCard.locator('.institution-card__description').count(), 0);
    await goto(owner, '/pages/institution-selection.html');
    await owner.locator('label.institution-selection-card').nth(1).waitFor();
    await owner.locator(`input[value="${secondId}"]`).check({force:true});
    await owner.locator('[data-institution-continue-link]').click();
    await owner.waitForFunction(id=>document.querySelector('[data-institution-selector]').value===String(id),secondId);
    await goto(owner,'/pages/my-institution.html');
    assert.equal(new URL(owner.url()).searchParams.get('institutionId'),String(secondId));
    await owner.reload();await owner.waitForFunction(()=>!document.documentElement.classList.contains('auth-pending'));
    assert.equal(new URL(owner.url()).searchParams.get('institutionId'),String(secondId));
    await owner.locator('[data-institution-selector]').selectOption(String(id));
    await owner.waitForURL(url=>url.searchParams.get('institutionId')===String(id));
    await owner.locator('[data-institution-setup]').waitFor();
    assert.ok(await owner.locator('a[href$="#needs"]').count()>0);
    for (const role of ['MANAGER', 'EDITOR']) {
      const roleContext = await browser.newContext(), rolePage = await roleContext.newPage();
      const roleEmail = `browser-${role.toLowerCase()}@example.org`;
      const requested = await send(roleContext, '/api/access-requests', 'POST', {
        institutionId: id, requesterName: 'Teste ' + role, requesterEmail: roleEmail,
        requesterPhone: '11999999999', requesterRole: 'Colaborador', justification: 'Teste isolado de permissão'
      });
      assert.equal(requested.status(), 201);
      const reviewed = await send(context, `/api/admin/access-requests/${(await requested.json()).id}/approve`, 'POST', { notes: null, membershipRole: role });
      assert.equal(reviewed.status(), 200);
      await reviewed.json(); await rolePage.goto(await mailLink('account-activation.html'));
      await rolePage.locator('[data-activation-form]').waitFor();
      await rolePage.locator('[name=password]').fill(password); await rolePage.locator('[name=confirmPassword]').fill(password);
      await rolePage.locator('button[type=submit]').click();
      await rolePage.waitForFunction(() => document.querySelector('[data-account-activation-page]').dataset.state === 'success');
      await login(rolePage, roleEmail);
      assert.equal((await send(roleContext, `/api/institutions/${id}`, 'PATCH', { description: 'Conteúdo editado por ' + role })).status(), 200);
      assert.equal((await send(roleContext, `/api/institutions/${id}`, 'PATCH', { legalName: 'Não permitido' })).status(), 403);
      assert.equal((await send(roleContext, `/api/management/institutions/${secondId}`)).status(), 403);
      assert.equal((await send(roleContext, `/api/institutions/${id}`, 'PATCH', { displayName: 'Instituição atualizada' })).status(), role === 'MANAGER' ? 200 : 403);
      assert.equal((await send(roleContext, '/api/auth/logout', 'POST')).status(), 200);
      assert.equal((await roleContext.request.get('http://127.0.0.1:8081/api/auth/me')).status(), 401);
      await roleContext.close();
    }
    await api(page, `/api/institutions/${secondId}`, 'PATCH', { status: 'INACTIVE' });
    const recover=await browser.newPage();await observe(recover);
    await goto(recover,'/pages/password-recovery.html');
    await recover.locator('[name=email]').fill('browser-owner@example.org');await recover.locator('button[type=submit]').first().click();
    const resetLink=await mailLink('password-recovery.html');await recover.goto(resetLink);
    const newPassword=`Changed-${randomUUID()}`;
    await recover.locator('[name=password]').fill(newPassword);await recover.locator('[name=confirmPassword]').fill(newPassword);
    await responsive(recover,'password recovery');await recover.locator('[data-recovery-reset] button').click();
    await recover.waitForFunction(()=>document.querySelector('[data-recovery-reset]').hidden);
    assert.equal((await ownerContext.request.get('http://127.0.0.1:8081/api/auth/me')).status(),401);
    await goto(owner,'/pages/institution-login.html');await owner.locator('[data-institution-email]').fill('browser-owner@example.org');await owner.locator('[data-institution-password]').fill(newPassword);await owner.locator('button[type=submit]').click();await owner.waitForURL(/selection/);
    await goto(owner,'/pages/my-institution.html');assert.equal(new URL(owner.url()).searchParams.get('institutionId'),String(id));
    await owner.locator('[data-auth-logout]').first().click();
    await owner.waitForURL(/institution-login\.html/);
    assert.equal((await ownerContext.request.get('http://127.0.0.1:8081/api/auth/me')).status(), 401);
    await visitor.emulateMedia({ reducedMotion: 'reduce' });
    await goto(visitor, '/'); await responsive(visitor, 'home reduced motion');
    await visitor.setViewportSize({ width: 338, height: 900 });
    await visitor.locator('[data-menu-toggle]').click(); await visitor.keyboard.press('Escape');
    assert.equal(await visitor.locator('[data-menu-toggle]').getAttribute('aria-expanded'), 'false');
    assert.equal(await visitor.locator('[data-menu-toggle]').evaluate(button => button === document.activeElement), true);
    await page.locator('[data-institution-details]').first().click(); await page.locator('.api-detail-dialog h3').first().waitFor(); await responsive(page, 'admin detail dialog');
    await page.getByLabel('Status global').selectOption('SUSPENDED'); await page.getByRole('button', { name: 'Salvar status' }).click();
    await page.waitForFunction(() => document.querySelector('.api-detail-dialog [role=status]').textContent.includes('salvas'));
    assert.equal((await api(page, `/api/management/institutions/${id}`)).status, 'SUSPENDED');
    assert.equal((await api(visitor, '/api/institutions')).totalElements, 0);
    assert.deepEqual(errors, []); assert.deepEqual(resources, []); assert.deepEqual(consoleErrors, []);
    await writeFile(resolve(root, 'backend/target/browser-results/report.json'), JSON.stringify({ checked, widths, pageErrors: errors, localResourceErrors: resources, consoleErrors }, null, 2));
  } finally {
    await browser?.close(); await new Promise(r => server.close(r)); java.kill(); await new Promise(resolve=>smtp.close(resolve));
  }
});
