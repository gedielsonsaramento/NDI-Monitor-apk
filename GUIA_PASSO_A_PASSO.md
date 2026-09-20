# Preparação de lançamento — celulares Android

Este guia se refere à proposta 1.2.0 no ramo de preparação, **não** a uma versão aprovada para publicação. Não envie APK/AAB enquanto as condições de licença e as verificações de produção estiverem pendentes.

## Abrir e testar localmente

1. Abra no Android Studio a pasta que contém `settings.gradle` — não só `app`. Use JDK 17 e instale Android SDK 36, Android NDK (preferencialmente r28+) e CMake 3.22.1.
2. Obtenha o SDK NDI para Android na fonte oficial. Execute você mesmo o instalador e leia o contrato incluído. Depois use `tools/instalar_ndi.ps1` no Windows ou `tools/instalar_ndi.sh` no Linux/macOS/Git Bash, passando a pasta extraída `NDI SDK for Android`. Os scripts copiam headers, `libndi.so` das arquiteturas ARM e `licenses/libndi_licenses.txt`; não distribua essas cópias no GitHub.
3. Execute `./gradlew :app:assembleDebug` (`.\gradlew.bat :app:assembleDebug` no Windows) para teste pessoal. Mantenha celular e fonte de vídeo na mesma rede e teste descoberta, vídeo, áudio, reconexão e retorno ao app. Caso apareça “SDK NDI ausente”, confira as pastas `app/src/main/cpp/include` e `app/src/main/jniLibs`, depois sincronize novamente.

## Condições para preparar o Android App Bundle (AAB)

- Confira **o contrato da versão instalada do SDK**; obtenha esclarecimento escrito da NDI onde houver dúvida sobre distribuição, uso da marca, codecs e termos para usuário final. O uso de NDI no nome do produto ou APK público no GitHub permanece pendente.
- Inclua no aplicativo e no site os links e atribuições exigidos pela [NDI](https://docs.ndi.video/all/developing-with-ndi/sdk/licensing). O botão “Licenças de terceiros” lê o aviso copiado do SDK; verifique também as demais cláusulas do contrato, termos para usuários, uma política de privacidade publicamente acessível e a ficha de Segurança dos dados da Play.
- Use o nome de produto provisório “Monitor de Retorno” enquanto não houver confirmação sobre “NDI Monitor” como marca. **Não altere o applicationId sem planejar migração de atualizações.**
- Gere sua chave de upload privada e ative/valide a assinatura pela Google Play em sua conta. Guarde a chave e os dados de recuperação em lugar seguro, nunca no Git. Gere um **AAB assinado de release**, não o APK de debug, em Android Studio > Build > Generate Signed Bundle / APK > Android App Bundle.
- O projeto agora define `targetSdk 36`; o [Google Play exige API 36 para aplicativos móveis novos desde 31/08/2026](https://support.google.com/googleplay/android-developer/answer/11926878). O AGP/Gradle foram elevados para uma combinação com suporte a API 36. Valide em seu Android Studio qualquer alteração de interface, permissões ou comportamento introduzida pelo novo target.
- Teste explicitamente o gesto/botão **Voltar** no Android 16: essa versão usa callbacks modernos para apps com target 36; este ramo migra a navegação do painel, mas ainda não foi exercitado em aparelho físico.
- Verifique `libndimonitor.so` e **a cópia exata** de `libndi.so` que entrará no AAB: todos os segmentos LOAD ARM64 precisam de alinhamento ELF de 16 KB; valide também a saída do `bundletool dump config --bundle=SEU.aab` (esperado `PAGE_ALIGNMENT_16K`), APKs gerados pela Play/bundletool com `zipalign -c -P 16 -v 4`, e o funcionamento num aparelho/emulador com `adb shell getconf PAGE_SIZE` retornando `16384`. Consulte as [instruções Android para 16 KB](https://developer.android.com/guide/practices/page-sizes). A configuração CMake ativa flags de 16 KB, mas **não substitui essas verificações**.
- Se sua conta Play pessoal foi criada após 13/11/2023, talvez seja exigido [teste fechado com pelo menos 12 pessoas por 14 dias contínuos](https://support.google.com/googleplay/android-developer/answer/14151465) antes do acesso à produção.

Somente após revisar os termos e concluir as verificações, envie o AAB assinado e a ficha completa pela Play Console. Esta proposta não concede permissão de distribuir um APK com o SDK em Releases do GitHub.
