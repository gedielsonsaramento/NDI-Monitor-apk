# Validação 1.4.4 — início e recuperação de TV Box

Revisão candidata de 25/10/2026 sobre o commit `7e568ccf3d1babe1b2c820e326af9c6ffa273ac6` (1.4.3). Relato: H6+ anuncia Android 7.1.2 e fecha antes do menu. Sem log do aparelho, não há causa confirmada.

## Mudança

- HTML não inicializa NDI; primeira configuração em API 23–25 abre HTML.
- Fonte NDI já selecionada continua sendo restaurada sem buscar outras fontes.
- Entrada independente por `StartupActivity`, inclusive launcher, Home e boot, sem referência tipada à Activity principal.
- Checkpoints de abertura, inicialização e primeira descoberta NDI; exceções Java são registradas antes de delegar ao tratamento do Android.
- Recuperação oferece relatório, HTML temporário, nova tentativa e configurações, preservando a fonte NDI salva até confirmar uma nova seleção.
- Tipos de recorte/insets das APIs 28/30 são isolados dos caminhos verificados em Android antigo.
- Pacote de teste e certificado da 1.4.3-TVBox mantidos. C++ e renderização/áudio não foram alterados.

## Verificação executada

Com JDK 17.0.16, Gradle 8.13, AGP 8.13.2, compile/target 36 e quatro bibliotecas nativas do APK 1.4.3 previamente verificado:

```bash
gradle --no-daemon --console=plain --max-workers=3 -PmonitorPrebuiltNativeDir=/caminho/nativo testDebugUnitTest lintDebug assembleTvboxTest
```

- **96 testes, zero falhas, zero erros, zero ignorados.** Robolectric em APIs 23/25/35 conforme o caso; HTTP simulado pelo MockWebServer.
- Casos novos: HTML sem chamadas nativas, inclusive superfície/limpeza; primeira configuração antiga; modo HTML temporário sem apagar NDI salvo; restauração NDI em API 25; abertura interrompida; encaminhamento do launcher; relatório da exceção; delegação ao tratamento de crash e retorno a uma tarefa existente sem falso startup pendente.
- Lint: **zero erros, três avisos anteriores** (`DiscouragedApi`, `DataExtractionRules`, `MergeRootFrame`). Os checkpoints usam `commit()` intencionalmente para chegar ao disco antes da execução nativa/encerramento; não são escritos por quadro/poll.
- APK: CRC do ZIP válido, pacote `br.com.polegar.ndimonitor.tvboxtest`, `versionCode 9`, `versionName 1.4.4-tvbox`, mínimo API 23, destino API 36, ARM32 e ARM64.
- Manifest compilado confere `MonitorApplication`, launcher/Home por `StartupActivity` e Activity principal interna.
- Assinatura v1/v2 válida. Certificado SHA-256 igual ao da edição 1.4.3-TVBox: `6007ea0469cbe58451d689913869fcfd617e6e686967ed8a1e113091eac51421`.
- `zipalign -c -P 16 4`: aprovado. Isso verifica o empacotamento, sem substituir teste em aparelho com páginas de 16 KB.
- As quatro bibliotecas `.so` e `assets/libndi_licenses.txt` são byte a byte iguais aos do APK 1.4.3-TVBox de SHA-256 `f3cde6339c37bd2593262b7d8d5cf3c6cf02dc17d976539622470490af6af8e8`. Nenhum arquivo do SDK ou chave foi incluído no Git.

## Artefato entregue

- `Monitor-de-Retorno-1.4.4-TVBox-teste.apk`: **11157572 bytes**.
- SHA-256: `11ce122f3da3bd50a78eceffe181845d343356e6b1929d381b17b67a58e562f2`.
- ZIP alternativo contém o mesmo APK e instruções, com CRC e igualdade do APK conferidos.
- Certificado de desenvolvimento; não é assinatura de produção e não foi enviado à Play ou publicado como release.

## Limites

Não houve acesso físico à H6+, às demais boxes, ao motor Chromium real ou à rede do usuário. Os testes simulam exceção/interrupção; não reproduzem o fechamento relatado. A revisão pode contornar a inicialização e permitir obter evidência, mas ainda não comprova NDI funcional nesse firmware, enquadramento HTML ou áudio associado a letras. `SIGSEGV`/`SIGILL` não são convertidos em exceções Java: um checkpoint pode indicar apenas a etapa interrompida, inclusive por desligamento, e a causa nativa completa requer Logcat/tombstone.

Procedimento: [TVBOX.md](TVBOX.md). Manual: [DOCUMENTACAO_COMPLETA.md](DOCUMENTACAO_COMPLETA.md).
