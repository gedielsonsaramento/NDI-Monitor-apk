# Teste do formulário da box em navegador

Executa o `SpresenterPairingServer` real em Java/loopback e envia seu formulário usando Chromium, com credencial sintética. Confere recebimento, Origin emitido pelo navegador, resposta e largura da página em áreas de desktop e celular. Não precisa de Android, NDI, chave real, TV Box ou S-Present em execução.

Requer JDK 17, Node.js e as classes compiladas do app. Na raiz do projeto, prepare as classes com `./gradlew compileDebugJavaWithJavac` e a integração NDI ou o reuso nativo documentado. Para executar:

```bash
cd tools/setup-browser-test
npm ci
npx playwright install chromium --only-shell
node check.cjs --classes ../../app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes --jdk /caminho/do/jdk-17
```

Um ambiente que já tenha Playwright instalado pode fornecer seu diretório de módulos por `PLAYWRIGHT_MODULES`, sem instalar outra cópia. Binários de navegador e node_modules não pertencem ao Git.

Se já houver Chromium/Chrome for Testing instalado, informe seu executável com `--browser /caminho/do/chrome-headless-shell`. A validação desta correção usou Chrome for Testing 133.0.6943.16, obtido do arquivo oficial do Google, com Playwright 1.62.1.

Para reproduzir a falha contra classes compiladas da 1.4.9, acrescente `--expected-status 403`. O teste exige que o navegador envie Origin null e que não haja recebimento. Com as classes corrigidas da 1.4.10, a execução normal exige HTTP 200, Origin igual ao endereço do formulário e recebimento da conexão.

A falha decorre da combinação de Referrer-Policy no-referrer com validação estrita de Origin em um POST de formulário. A correção usa same-origin e mantém a rejeição de Origin null ou externo. Referência primária: [Fetch Standard — append a request Origin header](https://fetch.spec.whatwg.org/#append-a-request-origin-header).

O teste não comprova as condições de rede, o navegador ou o firmware do usuário.
