# Validação — Monitor de Retorno 1.4.10-TVBox

Candidata de 26/10/2026, `versionCode 15`, baseada na 1.4.9 (`0baf06ddf9a3e513911ad70d6132962aab07ebf5`). Pacote `br.com.polegar.ndimonitor.tvboxtest`.

## Falha reproduzida e correção

As fotos mostram o navegador do PC acessando o mesmo IP/porta exibidos na TV. O envio termina em **Abra o endereço mostrado na TV**, uma resposta de recusa gerada pelo app. A conectividade com o servidor temporário existe; a configuração não é entregue à Activity.

Foi reproduzido com o `SpresenterPairingServer` real da 1.4.9, executado em Java/loopback, e envio do formulário por Chrome for Testing **133.0.6943.16**, usando Playwright **1.62.1** e uma credencial sintética. A resposta da página usa Referrer-Policy no-referrer, que faz o POST de formulário enviar **Origin null**. O app compara Origin ao endereço exato e recusa esse envio. Os testes HTTP anteriores escreviam Origin diretamente e não exercitavam essa decisão do navegador.

A correção troca somente a política da página para **same-origin**. O navegador passa a informar a origem real no envio da própria página. Host, Origin, código, validade, limites e envio único continuam sendo verificados; Origin null ou externo permanece recusado. Não abre CORS nem flexibiliza a autenticação para corrigir a falha.

O comportamento da política está definido no [Fetch Standard — append a request Origin header](https://fetch.spec.whatwg.org/#append-a-request-origin-header). O teste executável está em [tools/setup-browser-test](../../tools/setup-browser-test/README.md).

## Resultado do envio no navegador

| Classes do servidor | Área do navegador | Referrer-Policy | Origin emitido | Resposta | Conexão recebida |
| --- | --- | --- | --- | --- | --- |
| 1.4.9 | 1280 × 800 | no-referrer | null | 403 | Não |
| 1.4.9 | 390 × 844 | no-referrer | null | 403 | Não |
| 1.4.10 | 1280 × 800 | same-origin | Endereço exato do formulário | 200 | Sim |
| 1.4.10 | 390 × 844 | same-origin | Endereço exato do formulário | 200 | Sim |

O navegador preenche os campos e aciona o botão real. O servidor valida a chave sintética recebida, sem publicá-la no resultado. Também é verificada a ausência de excesso de largura na página. Não equivale à execução no Edge/PC ou na box do usuário.

## Verificações do app e pacote

- **194 testes Android aprovados**, sem falhas, erros ou ignorados, incluindo a matriz existente de APIs 23/25/28/34/35.
- Teste HTTP atualizado verifica a política compatível com o formulário e mantém recusa explícita de Origin null, externo e Host divergente. Validações anteriores de código, tamanho, duração, cancelamento e sessão permanecem aprovadas.
- `lintDebug`: zero erros e os três avisos anteriores (`DiscouragedApi`, `DataExtractionRules`, `MergeRootFrame`). `assembleTvboxTest` concluído.
- APK final contém a política same-origin no DEX; a política no-referrer não está presente. Manifest `1.4.10-tvbox`/15, mínimo API 23, destino API 36, ARM32/ARM64 e StartupActivity.
- CRC, assinaturas v1/v2 e certificado TVBox preservado: `6007ea0469cbe58451d689913869fcfd617e6e686967ed8a1e113091eac51421`.
- `zipalign -c -P 16 4` aprovado; isso não comprova execução em hardware com páginas de 16 KiB.
- Bibliotecas nativas, avisos NDI e todos os assets HTML ES5/Chrome 44–86 são idênticos byte a byte à 1.4.9. Sem mudança de áudio, reprodução, catálogo ou armazenamento das credenciais.

APK: **11.377.272 bytes**. SHA-256: `857f96307dae7637c6c17cf19a501edf229a91d7213332fa13dea1428ccd6170`.

## Conferir na box

Instale a 1.4.10 sobre a edição TVBox anterior, sem desinstalar. Feche a aba antiga no PC e abra uma nova sessão em **HTML → engrenagem → Saídas HTML do S-Present → Configurar pelo celular ou PC**. Abra no PC o endereço atual mostrado na TV, informe o novo código e cole o link completo de **Copiar link com token**. A porta e o código podem mudar a cada sessão.

Depois de enviar, confira a lista de saídas na TV e escolha a desejada. Esse teste físico de recebimento, consulta à API, armazenamento e reprodução permanece pendente. As fotos confirmaram a abertura da configuração e a resposta HTTP da box, não a conclusão do envio. O problema de áudio associado às letras continua fora desta correção. Nenhum merge, release ou envio à Play foi realizado.
