'use strict';
const assert = require('node:assert/strict');
const vm = require('node:vm');
const acorn = require('acorn');
const {transformProgram} = require('./prepare.cjs');
const fixtures = [
  ['loader is not shadowed by lowered loop',
    'var result=(function(){function n(x){return x+100;}return(function(){var before=n(1);for(let n=0;n<3;n++){}return[before,n(2)];})();})();', [101,102]],
  ['captured loop variables stay distinct',
    'var result=(function(){const callbacks=[];for(let n of [1,2,3])callbacks.push(()=>n);return callbacks.map(f=>f());})();', [1,2,3]],
  ['public shorthand keys are preserved',
    'var result=(function(){const stage=5;return {stage};})();', {stage:5}],
  ['async functions preserve awaited values',
    'var result=(async function(){const a=await Promise.resolve(3);return a+2;})();', 5],
  ['async connection helpers preserve lexical scope',
    'var result=(function(){let l;async function s(){return await new Promise(function(resolve){const u={readyState:1};l=u;resolve(u);});}async function u(e){return l||await s(),e();}return u(function(){return 42;});})();', 42]
];
async function main() {
  for (const [name,source,expected] of fixtures) {
    const code=transformProgram(source);
    acorn.parse(code,{ecmaVersion:5});
    const context={require};vm.runInNewContext(code,context);
    assert.deepEqual(JSON.parse(JSON.stringify(await context.result)),expected,name);
    console.log('PASS',name);
  }
}
main().catch(e => {console.error(e);process.exitCode=1;});
