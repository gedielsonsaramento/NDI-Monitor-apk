# Validação — Monitor de Retorno 1.4.6-TVBox

Candidata de 26/10/2026, `versionCode 11`, baseada na 1.4.5 (`c8b030bcc835f9f4b7a13b37876b77442ccd6480`). Pacote: `br.com.polegar.ndimonitor.tvboxtest`.

## Evidência e alteração

As fotos mostram a interface/diagnóstico 1.4.5 aberta na Amlogic HTV-6H, Android 7.1.2/API 25. O relatório Java identifica 1.4.4 e é histórico. O diagnóstico HTML seguinte informa Chrome 52.0.2743.100, área CSS 1920 × 1080 e `Unexpected token function`.

O CRC da captura completa foi validado. Ela contém HTML salvo, programa e estilo. A análise por AST encontrou 34 funções assíncronas, 88 spreads e 36 blocos catch sem parâmetro. O programa usa ainda APIs posteriores e `inset`. A adaptação converteu 24 objetos de inset zero e uma atribuição direta. A falta de Grid/clamp não prova a causa: esses recursos não aparecem no programa/estilo recebidos.

| Recurso | SHA-256 original |
| --- | --- |
| `main.ff832fdd.js` | `f3a8628c9a08d8923db81be8a031b5660234a433af634011418cc7cb8732a043` |
| `main.eb127441.css` | `3139abbab6f00e0fc5fbe7e9562cd0e567eeb35a5f92cdc370308573c23c460a` |

O adaptador exige esses bytes do servidor antes de entregar a adaptação dessa captura. O programa adaptado tem 493.323 bytes; o estilo, 11.316 bytes. Não há compilação de JavaScript no aparelho.

## Verificações

- `testDebugUnitTest lintDebug assembleTvboxTest`: **114 testes**, zero falhas, erros ou ignorados. A matriz simulada existente cobre APIs 23, 25, 28, 34 e 35.
- Os 14 novos testes cobrem hashes, origens, métodos/arquivos ignorados, cache por URL completa, limite de verificações, perfil ausente/corrompido, tamanho, fechamento sem bloquear a interface, redirecionamentos e cabeçalhos.
- Lint: zero erros e os três avisos anteriores (`DiscouragedApi`, `DataExtractionRules`, `MergeRootFrame`).
- Acorn: original recusado como ES2016; programa adaptado aprovado.
- JSDOM com APIs removidas, HTTP/WebSocket em loopback: carga e mudança de letra por `refresh.live.0`, mudança de elemento por `element.update.0`, APIs restabelecidas e quatro bordas explícitas. Zero erros na fixture.
- Manifest: mínimo API 23, target API 36, ARM32/ARM64, `StartupActivity`, `1.4.6-tvbox`/11.
- Assinaturas v1/v2 válidas; mesmo certificado TVBox `6007ea0469cbe58451d689913869fcfd617e6e686967ed8a1e113091eac51421`.
- CRC do APK e hashes dos assets conferidos. As quatro bibliotecas nativas e `libndi_licenses.txt` são idênticos byte a byte à 1.4.5 verificada.
- `zipalign -c -P 16 4` aprovado: confere empacotamento, não execução num aparelho de páginas de 16 KB.

APK: 11.298.845 bytes; SHA-256 `b6cd3f85d10482ab7d06f7f43b3104108f26049ca330d03dd7de6830dfa75bc0`.

## Limites e próximo teste

Não houve acesso ao aparelho ou à LAN do mantenedor. Chromium nativo foi bloqueado por socket local neste ambiente. JSDOM não confirma pixels, codecs, vídeo, áudio ou o comportamento exato do Chrome 52 físico. Não é compatibilidade universal com HTML ou com motores abaixo de Chrome 52. Testar instalação sobre a 1.4.5, mesma fonte HTML, atualização ao vivo, enquadramento, mídia e reinício. Confira a linha `Compatibilidade HTML` no diagnóstico.

Esta revisão não muda o receptor NDI/áudio nem comprova o recebimento de áudio associado a letras. As regras de descoberta e a origem persistida são mantidas. Captura, programa adaptado, SDK, chaves e binários ficam fora do Git. Nenhum pacote foi publicado como release ou enviado à Google Play.
