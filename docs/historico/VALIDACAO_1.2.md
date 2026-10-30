# Validação da revisão de teste 1.2.0

Verificações realizadas em 21/10/2026, com NDI SDK para Android 6.3.2.0, JDK 17, Android API 36, AGP 8.13.2, Gradle 8.13, NDK 28.2.13676358 e CMake 3.22.1.

## Resultados

- APK debug compilado com recepção real para ARM32 e ARM64; nome “Monitor de Retorno”, pacote preservado e `versionCode 3`.
- 10 testes Java passaram: 7 do catálogo (atualização incremental, pausa/retorno, ordem estável, duplicações e expiração) e 3 de layout simulado (640×320 dp, fonte ampliada e 1280×720 dp).
- Teste C++ do descobridor passou com uma simulação da interface pública NDI: mesma instância entre atualizações, resultados imediatos do cache, espera limitada e encerramento seguro. Não é um teste de descoberta em rede real.
- Lint: **0 erros**, 4 avisos sobre orientação, configuração de backup, estrutura do layout e recurso antigo sem uso.
- `zipalign -c -P 16 4` aprovou o APK.
- `libndi.so` e `libndimonitor.so` ARM64 do APK e do AAB têm segmentos LOAD alinhados a 16 KB e fim de RELRO alinhado.
- `bundletool dump config` confirmou `PAGE_ALIGNMENT_16K` no AAB local. O AAB permanece **sem assinatura de produção**.
- Avisos `libndi_licenses.txt` incluídos no APK/AAB, acessíveis pela engrenagem.

Comandos principais:

```sh
sh tools/test-native-discovery.sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:bundleRelease
```

Os testes de layout usam Android simulado API 35 com renderização nativa. As prévias são geradas em `app/build/reports/source-panel-previews/`.

## Instalação e testes no aparelho

O APK de teste tem uma chave debug diferente da usada no APK 1.1.0 anterior. Para atualizar a instalação anterior mantendo dados e assinatura, compile este código no Android Studio original com a mesma chave. Uma instalação direta do APK produzido em outra máquina pode ser recusada por conflito de assinatura.

Ainda é necessário testar no celular/TV Box do usuário: fontes que surgem em momentos diferentes, atualização manual, seleção por toque/controle, vídeo, áudio, reconexão, Voltar e retomada do segundo plano. O tempo inicial de descoberta continua dependendo das respostas mDNS da rede. Não foi feito teste com transmissor real nem aparelho de páginas de 16 KB nesta validação.

Para produção na Play, faltam assinatura de produção, termos/política de privacidade aplicáveis, ficha da loja e validação final. Nenhum APK ou AAB foi enviado à Play ou a Releases do GitHub nesta revisão.
