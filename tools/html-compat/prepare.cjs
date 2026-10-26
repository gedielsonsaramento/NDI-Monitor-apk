'use strict';

// Build-time only. The captured S-Present program is deliberately excluded from Git.
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const babel = require('@babel/core');
const postcss = require('postcss');
const presetEnv = require('postcss-preset-env');
const esbuild = require('esbuild');
const acorn = require('acorn');

const counts = { insetObjects: 0, insetAssignments: 0 };
function legacyInsets({ types: t }) {
  return { visitor: {
    ObjectProperty(p) {
      const key = p.node.key;
      if (p.node.computed || (key.name || key.value) !== 'inset') return;
      const value = p.node.value;
      if (!(t.isNumericLiteral(value, {value: 0}) || t.isStringLiteral(value, {value: '0'}))) {
        throw p.buildCodeFrameError('Review this nonzero inset before generating compatibility files.');
      }
      counts.insetObjects++;
      p.replaceWithMultiple(['top', 'right', 'bottom', 'left'].map(side =>
        t.objectProperty(t.identifier(side), t.cloneNode(value))));
    },
    AssignmentExpression(p) {
      const left = p.node.left;
      if (!t.isMemberExpression(left) || left.computed || left.property.name !== 'inset') return;
      if (!(t.isStringLiteral(p.node.right, {value: '0'}) || t.isNumericLiteral(p.node.right, {value: 0}))) {
        throw p.buildCodeFrameError('Review this inset assignment before generating compatibility files.');
      }
      // The captured program assigns a simple identifier.style; avoid repeated side effects.
      if (!t.isMemberExpression(left.object) || left.object.computed ||
          !t.isIdentifier(left.object.object) || left.object.property.name !== 'style') {
        throw p.buildCodeFrameError('Review inset target evaluation.');
      }
      counts.insetAssignments++;
      p.replaceWith(t.sequenceExpression(['top', 'right', 'bottom', 'left'].map(side =>
        t.assignmentExpression('=', t.memberExpression(t.cloneNode(left.object), t.identifier(side)),
          t.cloneNode(p.node.right)))));
    }
  }};
}

async function main() {
  const [scriptPath, cssPath, outputPath] = process.argv.slice(2);
  if (!scriptPath || !cssPath || !outputPath) {
    throw new Error('Usage: node prepare.cjs CAPTURED_MAIN_JS CAPTURED_MAIN_CSS OUTPUT_DIRECTORY');
  }
  const source = fs.readFileSync(scriptPath, 'utf8');
  const css = fs.readFileSync(cssPath, 'utf8');
  const output = path.resolve(outputPath);
  fs.mkdirSync(output, {recursive: true});
  const corejs = require('core-js/package.json').version;
  const transformed = babel.transformSync(source, {
    filename: 'captured-output.js', sourceType: 'unambiguous', configFile: false, babelrc: false,
    comments: true, compact: true, plugins: [legacyInsets],
    presets: [[require.resolve('@babel/preset-env'), {
      targets: {chrome: '52'}, ignoreBrowserslistConfig: true, bugfixes: true,
      modules: 'commonjs', useBuiltIns: 'usage', corejs
    }]]
  });
  const entry = [
    'require("core-js/modules/web.url-search-params");',
    'require("abortcontroller-polyfill/dist/polyfill-patch-fetch");',
    'var ResizeObserverFallback = require("resize-observer-polyfill");',
    'if (!window.ResizeObserver) window.ResizeObserver = ResizeObserverFallback.default || ResizeObserverFallback;',
    transformed.code
  ].join('\n');
  const built = esbuild.buildSync({
    stdin: {contents: entry, resolveDir: __dirname, sourcefile: 'captured-output.chrome52.js'},
    bundle: true, write: false, platform: 'browser', format: 'iife', target: 'chrome52',
    minify: true, legalComments: 'inline', define: {'process.env.NODE_ENV': '"production"'},
    nodePaths: [process.env.NODE_PATH || path.join(__dirname, 'node_modules')]
  });
  const javascript = built.outputFiles[0].text;
  acorn.parse(javascript, {ecmaVersion: 2016});
  const convertedCss = await postcss([presetEnv({
    browsers: ['Chrome 52'], stage: 2, preserve: false,
    features: {'logical-properties-and-values': true}
  })]).process(css, {from: undefined});
  const files = [
    {name: path.basename(scriptPath).replace(/\.baixados$/, ''), mime: 'application/javascript',
      file: 'spresenter-chrome52.js', original: source, content: javascript},
    {name: path.basename(cssPath), mime: 'text/css', file: 'spresenter-chrome52.css',
      original: css, content: convertedCss.css}
  ];
  const sha = x => crypto.createHash('sha256').update(x).digest('hex');
  const manifest = {schema: 1, profile: 'captura-2026-10-26', minChrome: 52, maxChrome: 86,
    resources: files.map(f => ({name: f.name, mime: f.mime,
      sourceSha256: sha(f.original), asset: 'html-compat/' + f.file, assetSha256: sha(f.content)}))};
  for (const file of files) fs.writeFileSync(path.join(output, file.file), file.content);
  fs.writeFileSync(path.join(output, 'manifest.json'), JSON.stringify(manifest, null, 2) + '\n');
  fs.writeFileSync(path.join(output, 'NOTICE.txt'),
    'Adaptacao local do JavaScript/CSS de Captura-Retorno.zip fornecido pelo usuario.\n' +
    'O programa S-Present e seus componentes mantem os respectivos direitos e avisos originais.\n' +
    'Nao inclui o HTML salvo, dados pessoais, enderecos da LAN ou configuracoes do culto.\n' +
    'Polyfills core-js, abortcontroller-polyfill e resize-observer-polyfill: licenca MIT.\n');
  // Preserve notices from every polyfill, even when a minifier removes comments.
  for (const mod of ['core-js', 'abortcontroller-polyfill', 'resize-observer-polyfill']) {
    const dir = path.dirname(require.resolve(mod + '/package.json'));
    const license = ['LICENSE', 'LICENSE.md', 'LICENSE.txt'].map(n => path.join(dir, n)).find(fs.existsSync);
    if (!license) throw new Error('Missing license for ' + mod);
    fs.appendFileSync(path.join(output, 'NOTICE.txt'), '\n=== ' + mod + ' ===\n' + fs.readFileSync(license, 'utf8'));
  }
  console.log(JSON.stringify({counts, resources: manifest.resources,
    javascriptBytes: Buffer.byteLength(javascript), cssBytes: Buffer.byteLength(convertedCss.css),
    syntax: 'ECMAScript 2016 parsed'}, null, 2));
}
main().catch(e => { console.error(e); process.exitCode = 1; });
