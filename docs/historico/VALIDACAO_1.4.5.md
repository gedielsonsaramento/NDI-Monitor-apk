# Validação 1.4.5 — fonte do tema e abertura da HTV

Revisão candidata de 25/10/2026 sobre `3e63e3580908e577f66f3c9826316c66380dfdd6` (1.4.4). A correção funcional altera somente a fonte no tema; a versão e a matriz dos testes também são atualizadas.

## Evidência do aparelho e do APK anterior

O relatório fotografado na Amlogic HTV-6H confirma Android 7.1.2/API 25, ARM64/ARM32 e app 1.4.4-TVBox. A abertura falha ao inflar `activity_main.xml`, linha 91, botão NDI/HTML. Causa final: `ArrayIndexOutOfBoundsException: length=49; index=74`, em `StringBlock.get`, `XmlBlock.Parser.getPooledString`, `TypedArray.getString`, `TextView.java:1100`.

A linha 1100 do [TextView no AOSP Android 7.1.2](https://github.com/aosp-mirror/platform_frameworks_base/blob/android-7.1.2_r39/core/java/android/widget/TextView.java#L1099-L1101) lê `fontFamily`. A inspeção binária do APK 1.4.4 entregue (SHA-256 `11ce122f3da3bd50a78eceffe181845d343356e6b1929d381b17b67a58e562f2`) encontrou:

- 49 strings no pool do XML de `activity_main`;
- 75 strings no pool global de `resources.arsc`;
- índice global **74 = `sans`**, correspondente ao literal global do tema.

Essa correspondência direciona a correção da leitura da fonte ao bloco de strings inadequado no Android antigo. Não é evidência de falha NDI, WebView ou rede: a exceção ocorre em `setContentView`, antes da configuração desses caminhos.

## Correção

O tema substitui `<item name="android:fontFamily">sans</item>` por `<item name="android:typeface">sans</item>`. O novo valor é enumeração inteira. O dump de recursos do APK 1.4.5 confirma `0x01010096=1` no AppTheme e ausência da definição global de `fontFamily` (`0x010103ac`). São preservados os widgets, suas ações, as fontes salvas e a recuperação da 1.4.4. Nenhum Java/C++ de produção foi alterado.

## Verificações executadas

- JDK 17.0.16, Gradle 8.13, AGP 8.13.2, compile/target API 36.
- `testDebugUnitTest lintDebug assembleTvboxTest`, com reuso das quatro bibliotecas verificadas da 1.4.4.
- **100 testes, zero falhas, zero erros, zero ignorados.** APIs simuladas 23/25/28/34/35 conforme o teste. Os testes existentes de compatibilidade são ampliados para API 28 (BTV) e 34 (Philco).
- Lint: zero erros e os três avisos anteriores: DiscouragedApi, DataExtractionRules, MergeRootFrame.
- APK: pacote `br.com.polegar.ndimonitor.tvboxtest`, versão `1.4.5-tvbox`, código **10**, min API 23, target 36, ARM32/ARM64 e launcher de recuperação.
- Assinatura v1/v2 válida. Certificado SHA-256 `6007ea0469cbe58451d689913869fcfd617e6e686967ed8a1e113091eac51421`, igual às edições 1.4.3/1.4.4-TVBox.
- CRC do APK/ZIP válido; `zipalign -c -P 16 4` aprovado. Isso verifica empacotamento, sem confirmar execução em aparelho com páginas de 16 KB.
- Quatro `.so` e `assets/libndi_licenses.txt` byte a byte iguais aos do APK 1.4.4 verificado. Nenhum SDK ou chave foi incluído no Git.

## Artefato

- `Monitor-de-Retorno-1.4.5-TVBox-teste.apk`: 11107645 bytes.
- SHA-256: `064657170e1695dccae0b2b3708a58b471852b4f32f583cf5fe9e1ad0bf937de`.
- ZIP alternativo contém o mesmo APK e instruções, incluindo a recuperação com relatório anterior.
- Nenhum APK/AAB foi publicado como release ou enviado à Play.

## Limites e próximo teste

Robolectric não reproduz todo o carregador de recursos do firmware da HTV e os testes anteriores também não haviam detectado esse crash. A correção agora é dirigida ao relatório e à análise binária, mas a abertura real ainda precisa ser confirmada na HTV. Não houve acesso ao aparelho, ao WebView real ou à LAN.

O relatório anterior da 1.4.4 é mantido após atualizar. Se aparecer, escolher **Abrir apenas HTML** para testar a nova tela. Em caso de nova falha, fotografar o último `Caused by` e conferir a versão registrada. A causa na B9 permanece sem relatório confirmado. Não há correção adicional do áudio associado a letras ou do CSS HTML nesta revisão.

[Procedimento de teste](../TVBOX.md) · [Manual completo](../DOCUMENTACAO_COMPLETA.md).
