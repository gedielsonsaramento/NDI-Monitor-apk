# Validação 1.4.3 — TV Box

Data: 22/10/2026, horário do Brasil. Revisão candidata para o relato de HTML comprimido dentro do app 1.4.2 em aparelho informado como Android 6, além de erro de instalação e fechamento em boxes ainda sem identificação de sistema por aparelho.

## Resultado

- Build `testDebugUnitTest lintDebug assembleTvboxTest`: aprovado.
- **78 testes**, zero falhas, erros ou testes ignorados.
- Lint: zero erros; três avisos existentes: DiscouragedApi, DataExtractionRules e MergeRootFrame.
- APK: `br.com.polegar.ndimonitor.tvboxtest`, versão `1.4.3-tvbox`, código 8, mínimo API 23 e destino/compilação API 36.
- Rótulo: Monitor de Retorno — TV Box. Pacote paralelo, com preferências próprias.
- Assinaturas v1 e v2 válidas. Certificado de desenvolvimento RSA 2048, SHA-256 `6007ea0469cbe58451d689913869fcfd617e6e686967ed8a1e113091eac51421`.
- `zipalign -c -P 16 4`: aprovado.
- Quatro bibliotecas ARM32/ARM64 e avisos NDI idênticos em bytes aos do APK 1.4.2; o código C++ não foi recompilado nesta verificação.
- APK com 11155724 bytes, SHA-256 `f3cde6339c37bd2593262b7d8d5cf3c6cf02dc17d976539622470490af6af8e8`.

## Cobertura

| Suíte | Casos |
| --- | ---: |
| HtmlDiscoverySessionTest | 2 |
| HtmlHttpProbeTest | 19 |
| HtmlMonitorTest | 6 |
| HtmlPageDiagnosticsTest | 2 |
| HtmlSourceCatalogTest | 2 |
| HtmlSourceUrlTest | 6 |
| HtmlStageViewportTest | 5 |
| LanIpv4SubnetTest | 4 |
| MainActivityModeTest | 15 |
| SourceCatalogTest | 7 |
| SourcePanelLayoutTest | 5 |
| TvBoxCompatibilityTest | 5 |

A cobertura adicional verifica restauração HTML e abertura do diagnóstico em API 23 e 25 com biblioteca nativa simulada como indisponível, nenhuma chamada nativa em API 23 inclusive para superfície e limpeza, navegação pelo callback antigo do WebView, densidade do firmware, persistência do modo de compatibilidade sem reiniciar busca e leitura defensiva dos dados CSS. O canvas também é conferido em escala de tela maior que 1.

O script de diagnóstico usa sintaxe ES5 e foi executado em contexto JavaScript isolado com CSS.supports presente, ausente e sem suporte às propriedades. Ele é de leitura, não consulta rede e não reescreve a página.

A análise estática identificou `ConcurrentHashMap.newKeySet` e `Map.putIfAbsent` incompatíveis com API 23. Foram substituídos por `Collections.newSetFromMap(new ConcurrentHashMap<>())` e montagem sequencial do mapa com `containsKey`/`put`, mantendo limites e cancelamento da busca. Nenhuma supressão de erro NewApi foi usada para esses casos.

## Ferramentas e reuso nativo

JDK 17.0.16+8, Gradle 8.13, AGP 8.13.2 e plataforma Android 36. O AGP usou seu build-tools padrão 35; a inspeção final de manifest, assinatura e alinhamento usou build-tools 36. Bibliotecas recuperadas do APK 1.4.2 de SHA-256 `2449ee838f926a27a613dc4055061940865df32a26821c747d7d220aa9c086c8`, com comparação SHA-256 individual e avisos do SDK preservados. O build opcional com `monitorPrebuiltNativeDir` desabilita release e verifica a presença das quatro bibliotecas; a origem e os hashes foram validados externamente nesta execução. A compilação normal por SDK/CMake continua disponível.

O certificado da edição de teste é diferente do das edições 1.4.0–1.4.2. O sufixo `.tvboxtest` evita a tentativa de atualizar essas instalações e o respectivo conflito de certificados. O novo nome permite selecionar o ícone correto na box.

## Limites da conclusão

- Robolectric testa código e componentes Android; não executa o Chromium real da box nem o CSS do tema do Spresenter.
- Não houve acesso às boxes, à rede local do usuário, ao seu WebView ou à resposta real da URL do retorno.
- A opção de densidade do firmware é uma alternativa de exibição a validar; não comprova correção da imagem fotografada. Não acrescenta CSS Grid, variáveis CSS, clamp ou JavaScript ausentes em motores antigos.
- Não houve teste físico de recepção NDI, áudio, controle remoto, boot, desempenho ou memória nesta revisão. Os binários nativos mantêm o limite API 24; NDI não é habilitado em API 23.
- LinkageError é protegido; falhas de execução nativa (SIGSEGV/SIGILL) ainda exigem log.
- A instalação paralela precisa configurar sua fonte uma vez. A restauração passa a usar suas próprias preferências.
- Nenhum APK/AAB foi publicado no GitHub Releases ou enviado à Google Play; não foi gerado AAB de produção.

Próximo teste: instalar a edição TV Box, selecionar HTML, conferir a opção 16:9 de compatibilidade e fotografar **Diagnóstico do aparelho** em cada box. Isso esclarecerá a diferença entre Android Release e API e o suporte de CSS do motor.
