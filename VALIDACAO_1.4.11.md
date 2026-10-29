# Validação — S-Retorno 1.4.11

Revisão de 27/10/2026, `versionCode 16`, baseada na 1.4.10
(`6405959cfe65aab2295baba662fe51d3900e563c`). Pacote de atualização:
`br.com.polegar.ndimonitor.tvboxtest`.

## Identidade do aplicativo

- Nome exibido **S-Retorno** nos recursos principal e TVBox, no launcher, no
  instalador, na opção Home e no título da recuperação.
- Nome de versão **1.4.11**, sem o sufixo `-tvbox`. O arquivo entregue chama-se
  `S-Retorno-1.4.11.apk`.
- Novo símbolo vetorial de duas faixas dobradas em azul e branco, apontando para
  a esquerda sobre uma base de monitor. A geometria foi criada para este app;
  os gráficos do S-Present usados como referência não são distribuídos.
- Ícones tradicional/redondo para Android antigo, adaptativo para Android 8+
  e camada monocromática para launchers com ícones temáticos.
- Banner Android TV com **S-Retorno / Monitor de palco / NDI · HTML**. Letras
  em curvas com precisão reduzida e caminhos divididos por palavra, sem fonte
  embutida ou dependência da fonte do firmware.
- Título da página de configuração por PC/celular atualizado para S-Retorno.

O pacote interno e a chave permanecem iguais aos das edições TVBox 1.4.3–1.4.10,
permitindo atualização com as preferências existentes. O identificador técnico
`.tvboxtest` não é o nome mostrado ao usuário e foi mantido para compatibilidade.

## Verificações

- **194 testes Android existentes aprovados**, zero falhas, erros ou ignorados.
  Incluem as configurações simuladas de APIs 23/25/28/34/35 já utilizadas pelo
  projeto. Não foram acrescentados testes que apenas repetem o nome ou a arte.
- **lintDebug e lintTvboxTest:** zero erros, somente os três avisos anteriores
  (`DiscouragedApi`, `DataExtractionRules`, `MergeRootFrame`).
- `assembleTvboxTest` concluído. Manifesto do APK: nome S-Retorno, versão
  `1.4.11 (16)`, mínimo API 23, destino API 36, ARM32/ARM64 e StartupActivity.
- CRC, assinaturas v1/v2 e certificado de atualização conferidos:
  `6007ea0469cbe58451d689913869fcfd617e6e686967ed8a1e113091eac51421`.
- `zipalign -c -P 16 4` aprovado. As quatro bibliotecas nativas, os avisos NDI
  e todos os recursos do perfil HTML são idênticos byte a byte à 1.4.10.
- Ícone SVG rasterizado e inspecionado a 32/48/64/96/512 px; máscara circular
  adaptativa inspecionada a 256 px. Banner inspecionado a 640 × 360 px. Essas
  prévias verificam a arte; não simulam todos os launchers dos fabricantes.
- Formulário executado com o servidor Java real em Chrome for Testing
  **133.0.6943.16**, usando Playwright **1.62.1** e credencial sintética.
  Áreas 1280 × 800 e 390 × 844: HTTP 200, Origin correto, conexão recebida.
  A política same-origin e a validação estrita continuam funcionando.

APK: **11.383.523 bytes**. SHA-256:
`7c04963730e4d182b0e402e5ac8f97c914a890e6d81d1aec6a0b20484d825b6d`.

## Escopo e testes em aparelhos — atualização de 29/10/2026

A mudança é visual e de identificação. Bibliotecas NDI, perfis HTML e estado
salvo são preservados. Na preparação de 27/10/2026, a validação física ainda
estava pendente.

Em **29/10/2026**, o mantenedor informou que **todas as boxes testadas rodaram
perfeitamente e a TV Philips com Android também**. Esse relato posterior está
registrado em [TESTES_EM_APARELHOS.md](TESTES_EM_APARELHOS.md). A marca da TV
segue o relato atual; seu modelo exato não foi informado.

Os testes físicos foram realizados pelo mantenedor. As verificações de
desenvolvimento acima continuam separadas desse relato: não houve acesso
direto à LAN ou aos aparelhos, e o relato não detalha uma matriz por função,
como áudio associado a letras, reconexão ou abertura após o boot.

O APK disponibilizado no GitHub é o mesmo arquivo verificado acima, sem
recompilação nesta atualização de documentação.

Instale sobre a edição TVBox anterior, sem desinstalar. Confira o nome e o ícone
**S-Retorno**, a versão **1.4.11 (16)** no diagnóstico e a retomada da fonte
salva. O launcher do fabricante pode manter o ícone antigo em cache até voltar
à tela inicial ou reiniciar o aparelho.
