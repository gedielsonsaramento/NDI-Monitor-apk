# Validação 1.4.0 - busca de retornos HTML

Revisão de teste preparada em 21/10/2026, horário do Brasil. Pacote br.com.polegar.ndimonitor, versionName 1.4.0, versionCode 5.

## Compilação e testes

- APK debug compilado para armeabi-v7a e arm64-v8a com o SDK NDI Android 6.3.2.0 real.
- A ponte JNI/C++ foi recompilada pelo NDK 28.2.13676358 e CMake 3.22.1 nas duas arquiteturas; não foi usado um receptor de preparação nem apenas reaproveitado o binário da ponte da versão 1.3.
- AAB release gerado e validado localmente pelo bundletool 1.18.3, sem assinatura de produção e sem envio à Play.
- 41 testes Java/Robolectric aprovados, sem falhas, erros ou testes ignorados.
- Regressão C++ do descobridor NDI aprovada com implementação simulada da API.
- Lint debug: zero erros e três avisos preexistentes: orientação fixa (DiscouragedApi), política de backup/transferência moderna (DataExtractionRules) e possível simplificação de layout (MergeRootFrame).
- Prévia visual do painel HTML em celular horizontal conferida. A geometria também foi verificada com fonte ampliada e tela grande. As prévias são renderizações de teste, não capturas de uma TV Box real.

| Grupo | Testes aprovados |
| --- | --- |
| Catálogo NDI | 7 |
| Planejamento de faixas IPv4 | 4 |
| Cliente HTTP com servidor local controlado | 7 |
| Concorrência e cancelamento da busca | 2 |
| Catálogo HTML e persistência do endereço lembrado | 2 |
| Seleção, recarga da busca, modo e ciclo de vida | 6 |
| Layout de celular, fonte ampliada e tela grande | 5 |
| Renderizador HTML | 4 |
| Validação e normalização de URL | 4 |

Os cenários HTTP cobrem resposta HTML válida e título, ausência de execução de scripts pelo cliente de descoberta, erro HTTP, autorização, JSON, conteúdo incompatível, redirecionamentos, cancelamento, corpo grande e resposta lenta. A seleção pela lista salva o endereço sem usar o campo manual. Eventos de buscas canceladas ou de uma atividade parada são ignorados nos testes de interface.

Comandos executados na raiz:

```bash
sh gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:bundleRelease
sh tools/test-native-discovery.sh
```

## Bibliotecas, assinatura e páginas de 16 KB

- As duas libndi.so são idênticas às presentes no APK de teste 1.3.0; os avisos de terceiros também foram preservados byte a byte.
- O APK foi verificado pelo apksigner: assinatura debug válida no esquema APK v2.
- zipalign -c -P 16 -v 4 aprovou o pacote final.
- Os segmentos LOAD das duas bibliotecas ARM64 usam alinhamento 0x4000. Seus intervalos GNU_RELRO terminam alinhados a 16 KB.
- O AAB declara PAGE_ALIGNMENT_16K e passou em bundletool validate.
- O certificado debug desta revisão difere do certificado do APK 1.3.0. Para atualização preservando os dados, gere com a chave anterior. Desinstalar a versão anterior apaga as preferências; a alternativa de reinstalação exige anotar e restaurar modo/endereço.

SHA-256 do APK de teste:

```text
07a87f3428a1f3ebc8c034728fd8c2b8145bf0d3209857b2966b3b7f9dbf5bc0
```

SHA-256 do certificado de teste 1.4.0:

```text
00ea89d2a8aa31439cb4dd592dd535826bc8e8692ec4825fd4647f944543d1c1
```

SHA-256 da libndi.so ARM64:

```text
09848dc0a66c79fb4918193517b65ad374af9a4e393db6bfb0c4f4c93ee4fccb
```

## Limites e pendências

Não houve acesso ao Spresenter ou à LAN do usuário nem teste deste APK em celular, TV Box ou Android TV físicos. Os testes de HTTP usam loopback e respostas controladas; os testes de interface usam Android simulado no SDK 35, enquanto o aplicativo é compilado e direcionado à API 36.

A busca automática verifica o padrão TCP 5050 com caminho /output/saida-html-1 e, quando aplicável, o endereço numérico salvo na rede conectada. Não enumera todas as saídas, portas, sites, sub-redes ou IPv6. Uma página HTML acessível é um candidato: servidores com rotas genéricas podem responder a um caminho que não representa um retorno válido.

Continuam pendentes no equipamento final: descoberta real em Wi-Fi/Ethernet, foco e seleção pelo controle remoto, conteúdo e atualização do Stage, vídeos, áudio fornecido pela página, reconexão, boot/Home, uso prolongado e recepção NDI Standard. Os checks de ELF/zip/AAB não substituem um teste funcional em aparelho com páginas de 16 KB.

O AAB local é de preparação e não foi entregue como arquivo pronto para loja. São necessários a chave de produção, os termos aplicáveis, a ficha da loja e as etapas de validação do mantenedor. Nenhum APK/AAB foi publicado em release do GitHub ou enviado ao Google Play.

Consulte DOCUMENTACAO_COMPLETA.md para o manual integral, incluindo uso, arquitetura, rede, compilação, assinatura, licenças e publicação.
