# Preparar uma captura para motores HTML antigos

Esta ferramenta é usada na compilação, não no aparelho. Prepara JavaScript de uma captura como ES5 e CSS para Chrome 44. O aplicativo usa o perfil somente em Chrome 44–86, após verificar os bytes que o servidor da fonte entrega. A 1.4.7 estende o perfil anterior (Chrome 52–86) para o motor Chrome 44 fotografado na BTV B9/API 23.

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

- Um passe de escopo renomeia bindings let/const antes de Babel: impede que var de um laço oculte o carregador de módulos externo no programa minificado. Mantém closures e chaves públicas.
- Babel/preset-env usa alvo IE 11 para forçar sintaxe ES5 e identifica polyfills ECMAScript utilizados. Isso não declara suporte ao navegador IE. O empacotador também usa alvo ES5; Acorn confere o resultado como ECMAScript 5.
- core-js/stable é carregado antes dos módulos de APIs web. whatwg-fetch cobre fetch quando ausente; URLSearchParams, abortcontroller-polyfill e resize-observer-polyfill cobrem as outras APIs observadas. As versões estão fixadas no lock e os avisos de licença acompanham o perfil.
- O passe de AST converte somente `inset: 0` e `identificador.style.inset = "0"`. Outros valores/alvos exigem revisão, evitando mudar a ordem de avaliação sem análise.
- PostCSS acrescenta fallbacks de estilo para Chrome 44.

WebSocket e requisições continuam no servidor selecionado. O adaptador só verifica/substitui os dois arquivos conhecidos; não encaminha mídia por um proxy nem acrescenta varredura de rede.

## Integração local

```bash
node check-transform.cjs
node check-live.cjs "../../app/src/main/assets/html-compat" "/captura/Slide do apresentador_files/main.ff832fdd.js.baixados"
```

Cinco fixtures executam o JavaScript transformado e verificam carregador externo, closures de laço, chaves públicas e dois caminhos assíncronos. Cada resultado passa pelo parser ES5.

A fixture de integração usa HTTP/WebSocket em loopback e JSDOM, remove algumas APIs modernas antes da carga e verifica polyfills e mudanças de letra/elemento por mensagens WebSocket. Propriedades ausentes são removidas com delete, preservando o comportamento de detecção por presença. Confere ES5 e as quatro bordas explícitas do contêiner.

JSDOM não mede pixels nem reproduz mídia. Não é Chrome 44/52 nem o firmware das boxes. Uma execução inicial expirou sem erro JavaScript antes da carga da tela; a repetição passou. Isso não comprova estabilidade de inicialização. Nenhum resultado de navegador nativo é reivindicado. O teste físico continua obrigatório; veja [VALIDACAO_1.4.7.md](../../docs/historico/VALIDACAO_1.4.7.md).

Referências: [Babel preset-env](https://babeljs.io/docs/babel-preset-env/), [core-js](https://core-js.io/docs/), [WebViewClient](https://developer.android.com/reference/android/webkit/WebViewClient).
