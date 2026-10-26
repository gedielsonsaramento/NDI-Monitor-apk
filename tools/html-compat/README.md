# Preparar uma captura para motores HTML antigos

Esta ferramenta é usada na compilação, não no aparelho. Prepara JavaScript/CSS de uma captura para Chrome 52. O aplicativo usa o perfil somente em Chrome 52–86, após verificar os bytes que o servidor da fonte entrega.

## Entrada e execução

Requer Node.js 18+ e uma captura HTML completa extraída. Use os arquivos de programa/estilo, não o HTML já preenchido com os dados do culto. Exemplo de `Captura-Retorno.zip`, recebido em 26/10/2026:

```bash
cd tools/html-compat
npm ci
node prepare.cjs "/captura/Slide do apresentador_files/main.ff832fdd.js.baixados" "/captura/Slide do apresentador_files/main.eb127441.css" "../../app/src/main/assets/html-compat"
```

O sufixo `.baixados` é removido apenas do nome registrado. O hash continua correspondendo ao programa original. A saída contém programa/estilo adaptados, manifesto com hashes e avisos das dependências. A captura não inclui `imported.css`; esse recurso e as fontes continuam carregados do servidor.

Na raiz, compile com o SDK NDI integrado ou com o reuso nativo de depuração já documentado:

```bash
./gradlew testDebugUnitTest lintDebug assembleTvboxTest
```

Não acrescente o diretório gerado nem a captura ao Git. Sem o perfil, o app mantém o carregamento original e o diagnóstico informa a ausência do pacote. Outro build do S-Present exige nova análise, geração e teste: o app não entrega a captura anterior para bytes diferentes.

## Adaptação

- Babel/preset-env transforma sintaxe para Chrome 52 e identifica polyfills ECMAScript utilizados.
- core-js, abortcontroller-polyfill e resize-observer-polyfill cobrem as funções e APIs ausentes observadas.
- O passe de AST converte somente `inset: 0` e `identificador.style.inset = "0"`. Outros valores/alvos exigem revisão, evitando mudar a ordem de avaliação sem análise.
- PostCSS acrescenta fallbacks de estilo para Chrome 52. Acorn confirma que o programa gerado é aceito como ECMAScript 2016.

WebSocket e requisições continuam no servidor selecionado. O adaptador só verifica/substitui os dois arquivos conhecidos; não encaminha mídia por um proxy nem acrescenta varredura de rede.

## Integração local

```bash
node check-live.cjs "../../app/src/main/assets/html-compat" "/captura/Slide do apresentador_files/main.ff832fdd.js.baixados"
```

A fixture usa HTTP/WebSocket em loopback e JSDOM, remove algumas APIs modernas antes da carga e verifica polyfills e mudanças de letra/elemento por mensagens WebSocket. Confere o parser ES2016 e as quatro bordas explícitas do contêiner.

JSDOM não mede pixels nem reproduz mídia. Não é Chrome 52 nem o firmware da HTV. A tentativa de Chromium nativo neste ambiente foi bloqueada por socket local; nenhum resultado de navegador nativo é reivindicado. O teste físico continua obrigatório.

Referências: [Babel preset-env](https://babeljs.io/docs/babel-preset-env/), [core-js](https://core-js.io/docs/), [WebViewClient](https://developer.android.com/reference/android/webkit/WebViewClient).
