const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const readline = require('node:readline');
const {spawn, spawnSync} = require('node:child_process');
const {chromium} = require(process.env.PLAYWRIGHT_MODULES
  ? path.join(process.env.PLAYWRIGHT_MODULES, 'playwright') : 'playwright');

function arg(name, fallback) {
  const index = process.argv.indexOf(name);
  return index < 0 ? fallback : process.argv[index + 1];
}
const classes = path.resolve(arg('--classes', 'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes'));
const jdk = arg('--jdk', process.env.JAVA_HOME);
assert(jdk, 'Provide --jdk or JAVA_HOME');
const expected = Number(arg('--expected-status', '200'));
assert([200, 403].includes(expected));
const fixture = 'http://192.0.2.53:5050/warp/?token=spk_synthetic_browser_fixture_123456';
const temporary = fs.mkdtempSync(path.join(os.tmpdir(), 'monitor-setup-browser-'));
const compile = spawnSync(path.join(jdk, 'bin', 'javac'), ['-cp', classes, '-d', temporary,
  path.join(__dirname, 'SetupBrowserHarness.java')], {encoding: 'utf8'});
assert.equal(compile.status, 0, compile.stderr);

async function run(browser, viewport) {
  const java = spawn(path.join(jdk, 'bin', 'java'), ['-cp', [temporary, classes].join(path.delimiter),
    'br.com.polegar.ndimonitor.SetupBrowserHarness'], {stdio: ['ignore', 'pipe', 'pipe']});
  let received = false;
  let diagnostics = '';
  java.stderr.on('data', chunk => { diagnostics += chunk; });
  let resolveReady, rejectReady, resolveReceived;
  const ready = new Promise((resolve, reject) => { resolveReady = resolve; rejectReady = reject; });
  const accepted = new Promise(resolve => { resolveReceived = resolve; });
  const lines = readline.createInterface({input: java.stdout});
  lines.on('line', line => {
    try {
      const event = JSON.parse(line);
      if (event.address) resolveReady(event);
      if (event.received !== undefined) { received = event.received; resolveReceived(); }
    } catch (error) { rejectReady(error); }
  });
  java.on('error', rejectReady);
  java.on('exit', code => { if (code && code !== 0) rejectReady(new Error(diagnostics)); });
  const timer = setTimeout(() => rejectReady(new Error('Java setup did not start: ' + diagnostics)), 10000);
  const context = await browser.newContext({viewport});
  const page = await context.newPage();
  try {
    const setup = await ready;
    clearTimeout(timer);
    const initial = await page.goto(setup.address, {waitUntil: 'domcontentloaded'});
    assert.equal(initial.status(), 200);
    assert.equal(await page.locator('input[name=code]').count(), 1);
    assert.equal(await page.locator('textarea[name=link]').count(), 1);
    assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth), false);
    await page.locator('input[name=code]').fill(setup.code);
    await page.locator('textarea[name=link]').fill(fixture);
    const submission = page.waitForResponse(response => response.request().method() === 'POST');
    await page.getByRole('button', {name: 'Enviar para esta TV'}).click();
    const result = await submission;
    const origin = (await result.request().allHeaders()).origin;
    assert.equal(result.status(), expected);
    if (expected === 200) {
      assert.equal(initial.headers()['referrer-policy'], 'same-origin');
      assert.equal(origin, setup.address);
      await Promise.race([accepted, new Promise((_, reject) =>
        setTimeout(() => reject(new Error('Java did not receive the connection')), 5000))]);
      assert(received);
      await page.waitForLoadState('domcontentloaded');
      assert.match(await page.locator('main').innerText(), /Link recebido/);
    } else {
      assert.equal(origin, 'null');
      assert.equal(received, false);
    }
    return {viewport, status: result.status(), origin: expected === 200 ? 'same-origin' : origin,
      received, referrerPolicy: initial.headers()['referrer-policy']};
  } finally {
    clearTimeout(timer);
    await context.close();
    lines.close();
    java.kill();
  }
}

(async () => {
  const browser = await chromium.launch({headless: true, executablePath: arg('--browser', undefined)});
  try {
    const reports = [];
    for (const viewport of [{width: 1280, height: 800}, {width: 390, height: 844}]) {
      reports.push(await run(browser, viewport));
    }
    process.stdout.write(JSON.stringify({browser: browser.version(), reports}) + '\n');
  } finally {
    await browser.close();
    fs.rmSync(temporary, {recursive: true, force: true});
  }
})().catch(error => { console.error(error); process.exitCode = 1; });
