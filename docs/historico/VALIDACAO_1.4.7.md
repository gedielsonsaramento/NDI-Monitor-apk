# Validação — Monitor de Retorno 1.4.7-TVBox

Candidata de 26/10/2026, `versionCode 12`, baseada na 1.4.6 (`0196390858210482c334fcc8f4df2ba003495c9d`). Pacote: `br.com.polegar.ndimonitor.tvboxtest`.

## Evidência e alteração

A foto da BTV B9 identifica Android 6.0.1/API 23, ARM32 e app 1.4.6-TVBox com menu/diagnóstico aberto. O motor informa Chrome 44.0.2403.119, sem perfil elegível na 1.4.6, e `Unexpected token )` em `main.ff832fdd.js:2`. O histórico de abertura interrompida não registra exceção Java; não confirma falha nativa. NDI indisponível em API 23 é esperado.

A revisão transforma a mesma captura para ES5 e estende o perfil a Chrome 44–86. Renomeia 1.779 bindings let/const antes da conversão para preservar escopo no programa minificado, converte 24 objetos inset zero e uma atribuição direta em quatro bordas e prepara o CSS para Chrome 44. Carrega a base core-js antes dos módulos de APIs web, com fallback de fetch/AbortController/URLSearchParams/ResizeObserver. O diagnóstico manual espera até cinco segundos; não acrescenta consultas periódicas.

| Recurso | SHA-256 original exigido do servidor |
| --- | --- |
| `main.ff832fdd.js` | `f3a8628c9a08d8923db81be8a031b5660234a433af634011418cc7cb8732a043` |
| `main.eb127441.css` | `3139abbab6f00e0fc5fbe7e9562cd0e567eeb35a5f92cdc370308573c23c460a` |

| Asset adaptado | Bytes | SHA-256 |
| --- | ---: | --- |
| `spresenter-legacy.js` | 642.188 | `61ce325688f573fc877b720400cc8277213034edad668de51e4187583f48330c` |
| `spresenter-legacy.css` | 11.353 | `e118f55e761ad34c838958b2eca6b165e03c612dc422d32a1742ccc2e3af6fa7` |

Perfil `captura-2026-10-26-es5`, mínimo Chrome 44 e máximo 86. Só substitui os dois subrecursos conhecidos após conferir nome, mesma origem e hashes do servidor e do asset. Métodos, tamanhos, tempos, cache e limites de verificações permanecem como na 1.4.6. Servidor, WebSocket e mídia continuam nos endereços da fonte selecionada.

## Verificações

- `testDebugUnitTest lintDebug assembleTvboxTest`: **117 testes**, zero falhas, erros ou ignorados. Matriz simulada existente: APIs 23, 25, 28, 34 e 35. Após gerar os assets finais, `assembleTvboxTest` foi executado novamente com sucesso.
- Três testes Android adicionais verificam elegibilidade dos motores 44/52/66, rejeição do perfil antigo mínimo 52 e prazo de cinco segundos do diagnóstico manual. As verificações anteriores de origem/hash/limites permanecem.
- Lint: zero erros, mesmos três avisos (`DiscouragedApi`, `DataExtractionRules`, `MergeRootFrame`).
- Acorn: original recusado como ES5; programa empacotado adaptado aprovado.
- Cinco fixtures executadas em VM, cada uma com parser ES5: carregador externo preservado após conversão de laço let, closures distintas, chave pública de objeto e dois caminhos async/await.
- JSDOM com APIs selecionadas removidas, HTTP/WebSocket em loopback: carga de letra, troca por `refresh.live.0`, alteração de elemento por `element.update.0`, 13 APIs restabelecidas e quatro bordas explícitas; zero erros no resultado aprovado.
- Uma execução inicial da fixture expirou antes da carga da tela, sem erro JavaScript; a repetição com o mesmo código passou. A causa do timeout não foi confirmada. O resultado aprovado não comprova estabilidade de inicialização no aparelho.
- Manifest: mínimo API 23, target API 36, ARM32/ARM64, `StartupActivity`, `1.4.7-tvbox`/12.
- Assinaturas v1/v2 válidas; mesmo certificado TVBox `6007ea0469cbe58451d689913869fcfd617e6e686967ed8a1e113091eac51421`.
- CRC do APK, hashes e igualdade dos assets finais com os arquivos empacotados conferidos. As quatro bibliotecas nativas e `libndi_licenses.txt` são idênticos byte a byte à 1.4.6 verificada.
- `zipalign -c -P 16 4` aprovado: confere empacotamento, não execução num aparelho de páginas de 16 KB.

APK: **11.596.231 bytes**. SHA-256: `dfa662e848d9d6c7d9cc5fb74db3b10c6286e8a63315bc39d0d903e2ad417b67`.

## Limites e teste no aparelho

Não houve acesso à LAN ou à B9/HTV física. Não foi executado Chrome 44 nativo neste ambiente. JSDOM não verifica pixels, fontes, desempenho, reprodução de mídia ou firmware. O parser ES5 confirma sintaxe, sem garantir todas as APIs e comportamentos do motor.

Instale sobre a 1.4.6, abra a mesma fonte HTML e confira **adaptação aplicada a 2 de 2 arquivos (Chrome 44)** no diagnóstico. Teste letra/visitante, enquadramento, reabertura e estabilidade conforme [TVBOX.md](../TVBOX.md). Builds diferentes do servidor exigem outra captura e perfil; não há compatibilidade universal com qualquer página.

Android 6 permanece somente em HTML. NDI/API 24+, áudio associado a letras, mídia e reconexão exigem validação própria no equipamento final. Esta alteração não corrige o áudio da letra. Captura, programa adaptado de terceiros, SDK, chaves e APKs ficam fora do Git. Nenhum release ou envio à Play foi feito.
