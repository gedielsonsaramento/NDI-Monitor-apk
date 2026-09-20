# Monitor de Retorno — preparação para Android / Google Play

Aplicativo independente que descobre e recebe fontes de vídeo e áudio compatíveis com NDI® na rede local. Esta proposta (versão de código 1.2.0, `versionCode 3`) é dedicada a **celulares Android**. Não oferece função de tela inicial, abertura após o boot nem entrada na launcher de TV.

O código próprio está sob a [licença MIT](LICENSE). **Este repositório não contém o SDK NDI, `libndi.so`, APK ou AAB.** A licença MIT não concede licença para redistribuir o SDK ou usar a marca NDI no nome do produto. NDI® é marca registrada da Vizrt NDI AB; [saiba mais sobre NDI](https://ndi.video/). Não há patrocínio ou certificação pela Vizrt/NDI.

## Situação da publicação

Esta ramificação é uma preparação **reversível**, não uma autorização de publicação nem um binário testado. O nome exibido foi temporariamente alterado para “Monitor de Retorno”, mantendo o identificador de pacote `br.com.polegar.ndimonitor` para preservar a possibilidade de atualizar instalações existentes. O produto ainda usa a tecnologia e a atribuição NDI® em sua interface; isso não dispensa os termos do SDK.

A [documentação oficial de licenciamento NDI](https://docs.ndi.video/all/developing-with-ndi/sdk/licensing) e o contrato incluído no SDK devem ser lidos juntos; o manual do SDK para Android descreve uma permissão para produtos móveis distribuídos em lojas de aplicativos Android, sujeita aos seus termos. Isso **não resolve automaticamente** a publicação de um APK com `libndi.so` no GitHub, o uso em aparelhos dedicados/TV Box, nem a autorização da marca no nome do produto. Veja [LICENCA_NDI_IMPORTANTE.md](LICENCA_NDI_IMPORTANTE.md).

## Compilação local

| Item | Projeto |
| --- | --- |
| Android mínimo | API 24 |
| Android compile / target | API 36 |
| JDK / Gradle / AGP | 17 / 8.13 / 8.13.2 |
| Código nativo | NDK r28+ recomendado; CMake 3.22.1 |
| ABIs | ARM32 `armeabi-v7a` e ARM64 `arm64-v8a` |
| Recepção real | SDK NDI para Android instalado separadamente |

1. Abra a **raiz do projeto** (a pasta de `settings.gradle`) no Android Studio e instale Android API 36, CMake e NDK.
2. Obtenha o [SDK NDI oficial](https://ndi.video/for-developers/ndi-sdk/), leia e aceite por conta própria seus termos. Instale sua cópia local antes de usar os scripts abaixo.
3. Integre os arquivos locais. O script copia os headers, bibliotecas ARM e avisos de licenças de terceiros para o projeto; essas cópias estão ignoradas pelo Git.

PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\instalar_ndi.ps1 -SdkDir "C:\CAMINHO\NDI SDK for Android"
```

Linux/macOS/Git Bash:

```bash
sh ./tools/instalar_ndi.sh "/caminho/NDI SDK for Android"
```

4. Sincronize o Gradle. Use `./gradlew :app:assembleDebug` (ou `.\gradlew.bat :app:assembleDebug` no Windows) **somente para teste privado**. Sem o SDK, a compilação debug é um modo de preparação e não recebe fontes; uma release falha propositalmente se os headers, ambas as bibliotecas e os avisos de terceiros estiverem ausentes.

## Antes de criar uma release

Leia o [guia de preparação para Google Play](GUIA_PASSO_A_PASSO.md). Ainda faltam uma aceitação dos termos e da atribuição aplicáveis, termos de usuário compatíveis com o SDK, política de privacidade pública acessível pelo aplicativo e pela ficha, assinatura de produção, teste em celular e validação do AAB e de **todas** as bibliotecas 64-bit em dispositivo de páginas de 16 KB. Uma alteração de Gradle/CMake, sozinha, não confirma compatibilidade.

Não inclua SDK, binário pronto, chaves de assinatura ou dados pessoais neste repositório. A publicação de binários no GitHub e o nome antigo “NDI Monitor” permanecem dependentes de esclarecimento com a NDI.
