# Validação 1.4.1 - última fonte e busca sob demanda

Revisão de teste de 21/10/2026, horário do Brasil. Pacote br.com.polegar.ndimonitor, versionName 1.4.1, versionCode 6.

## Comportamento ajustado

- Inicializa no modo da última fonte confirmada e conecta diretamente ao nome NDI ou URL HTML salvo, com o painel fechado.
- Salva modo e origem na mesma edição das preferências. Navegar por outro modo ainda sem fonte não substitui a última seleção persistida.
- Mantém as chaves de preferências existentes e recupera uma fonte salva válida quando o modo legado está incompleto.
- Selecionar uma fonte ou fechar a lista cancela as varreduras e libera a instância de descoberta NDI. Reabrir apenas o painel não inicia nova busca; atualizar solicita uma sessão de descoberta.
- Uma configuração sem fonte mantém a busca inicial. Durante uma sessão solicitada, a descoberta pode continuar até a seleção ou fechamento.
- Ao ir para segundo plano, encerra a recepção NDI e o renderizador HTML. Ao retornar, usa a mesma origem. Conexões NDI enfileiradas são protegidas por geração para descartar pedidos cancelados.
- A recuperação de sinal permanece direcionada à fonte escolhida; não seleciona automaticamente outra origem da rede.

## Verificações executadas

Comando:

```bash
sh gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Resultado: 48 testes aprovados, sem falhas, erros ou testes ignorados. Lint com zero erros e os mesmos três avisos da 1.4.0: orientação fixa (DiscouragedApi), regras modernas de backup (DataExtractionRules) e sugestão de layout (MergeRootFrame).

Os sete novos cenários de regressão cobrem restauração NDI e recriação da atividade sem enumerar fontes, pausa e retomada NDI, liberação do finder após seleção, menu aberto sem iniciar descoberta, restauração/repetição do mesmo endereço HTML, preservação da última seleção ao navegar por um modo incompleto e persistência de modo/URL para o próximo início. Também há recuperação de configurações legadas incompletas.

| Grupo | Testes aprovados |
| --- | --- |
| Catálogo NDI | 7 |
| Faixas IPv4 | 4 |
| HTTP com respostas controladas | 7 |
| Concorrência/cancelamento HTML | 2 |
| Catálogo HTML | 2 |
| Modos, persistência e ciclo de vida | 13 |
| Layout | 5 |
| Renderizador HTML | 4 |
| Validação de URL | 4 |

## APK e atualização

- APK debug ARM32/ARM64 gerado com o SDK NDI real integrado.
- As quatro bibliotecas nativas e os avisos do SDK são idênticos aos do APK 1.4.0. O código C++ não mudou nesta revisão.
- Assinatura do APK verificada pelo apksigner. O certificado é o mesmo do APK 1.4.0 entregue, permitindo atualização sobre essa instalação sem desinstalar, preservadas as regras normais do Android.
- Alinhamento do APK aprovado em zipalign -c -P 16 -v 4. As bibliotecas ARM64 preservam os mesmos bytes com alinhamento ELF de 16 KB já conferido na 1.4.0.
- Não foi gerado um novo AAB nesta revisão de teste. Nenhum pacote foi enviado à Play ou publicado como release do GitHub.

SHA-256 do APK 1.4.1:

```text
01079d0aaa0ee2b3b2f42baf8c34b986388ca2c21deab04440e5b2ba6893ad34
```

SHA-256 do certificado, igual ao da 1.4.0:

```text
00ea89d2a8aa31439cb4dd592dd535826bc8e8692ec4825fd4647f944543d1c1
```

## Limites da validação

Os testes de reinício e ciclo de vida usam Android simulado/Robolectric, com a recepção nativa substituída por uma implementação de teste. Não houve medição de CPU nem validação deste APK com transmissor, rede, celular ou TV Box físicos.

A confirmação final deve incluir instalar por cima da 1.4.0, escolher uma fonte em cada modo, fechar e reabrir o aplicativo, reiniciar o aparelho, testar ausência e retorno da fonte e abrir o menu sem solicitar novas buscas. A abertura após boot depende do firmware. O SDK NDI e a página HTML continuam realizando o tráfego necessário para a origem escolhida; a mudança interrompe a descoberta geral de outras fontes no aplicativo.

Use DOCUMENTACAO_COMPLETA.md para instalação, operação, arquitetura, compilação, assinatura e preparação para distribuição.
