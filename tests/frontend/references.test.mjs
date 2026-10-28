import test from 'node:test';
import assert from 'node:assert/strict';
import { readdir, readFile, access } from 'node:fs/promises';
import { resolve, dirname, extname } from 'node:path';

test('HTML, CSS and JS local references resolve to real files', async () => {
  const root = resolve('.'), missing = [];
  async function files(folder) {
    const entries = await readdir(folder, { withFileTypes: true });
    const groups = await Promise.all(entries.map(e => e.isDirectory() ? files(resolve(folder, e.name)) : [resolve(folder, e.name)]));
    return groups.flat();
  }
  const targets = [resolve('index.html'), ...(await Promise.all(['pages', 'js', 'css'].map(d => files(resolve(d))))).flat()];
  for (const file of targets) {
    const ext = extname(file); if (!['.html', '.js', '.css'].includes(ext)) continue;
    const source = await readFile(file, 'utf8');
    const pattern = ext === '.html' ? /(?:href|src)\s*=\s*["']([^"']+)["']/g : ext === '.css' ? /url\(\s*["']?([^"')]+)["']?\s*\)/g : /(?:from\s*|import\s*)["']([^"']+)["']/g;
    for (const match of source.matchAll(pattern)) {
      const ref = match[1]; if (/^(?:[a-z][a-z0-9+.-]*:|#|\/\/)/i.test(ref)) continue;
      const path = decodeURIComponent(ref.split(/[?#]/)[0]); if (!path) continue;
      const target = path.startsWith('/') ? resolve(root, '.' + path) : resolve(dirname(file), path);
      try { await access(target); } catch { missing.push(`${file}: ${ref}`); }
    }
  }
  assert.deepEqual(missing, []);
});
