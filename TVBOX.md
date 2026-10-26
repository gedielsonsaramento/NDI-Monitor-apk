# Teste em TV Box — Monitor de Retorno 1.4.6

Revisão candidata de 26/10/2026. As fotos confirmam que a **1.4.5** abriu o menu/diagnóstico na **Amlogic HTV-6H**, Android **7.1.2 / API 25**. O erro Java da 1.4.4 permanece como histórico. O problema seguinte do HTML informa Chrome 52 e `Unexpected token function`. A 1.4.6 adapta o programa/estilo dessa captura para esse motor; ainda precisa ser testada fisicamente.

## Confirmar a adaptação HTML

Instale a 1.4.6-TVBox sobre a 1.4.5, mantendo as preferências, e abra a mesma fonte HTML. Na HTV, use **Exibição HTML → Monitor 16:9 — compatibilidade TV Box**. Faça uma mudança de letra ou visitante no computador e confira a atualização na tela.

Em **Diagnóstico do aparelho**, o início deve identificar **1.4.6-tvbox (11)**. No final, procure **Compatibilidade HTML: adaptação aplicada a 2 de 2 arquivos (Chrome 52)**. Se ficar preto ou deformado, fotografe essa parte, a mensagem de erro e a linha **Arquivo**, se houver. Feche/reabra para conferir a restauração sem busca geral.

O perfil foi preparado de `main.ff832fdd.js` e `main.eb127441.css` de `Captura-Retorno.zip`. Em Chrome 52–86, verifica nome, origem e SHA-256 dos bytes atuais do servidor antes de usar a adaptação. Outros arquivos/builds, perfis ausentes ou motores atuais mantêm a página original. O WebSocket e a mídia continuam na fonte selecionada; não exige serviço extra no PC nem substitui o WebView. Consulte [preparo de outra captura](tools/html-compat/README.md).

## Instalar e abrir

1. Use `Monitor-de-Retorno-1.4.6-TVBox-teste.apk`.
2. Abra o ícone **Monitor de Retorno — TV Box**. O pacote `br.com.polegar.ndimonitor.tvboxtest` e o certificado da 1.4.3-TVBox foram mantidos para atualizar essa edição. A instalação habitual, como 1.4.2, permanece separada.
3. Sem configuração anterior, API 23–25 entra em HTML sem carregar NDI. Uma fonte NDI já selecionada é restaurada normalmente.
4. Se aparecer a recuperação com o relatório antigo da 1.4.4, escolha **Abrir apenas HTML** para testar a nova interface. Fotografe um novo relatório se houver outra falha. Queda de energia ou encerramento também podem interromper a abertura; somente um checkpoint não comprova uma falha nativa.
5. Escolha a saída HTML ou informe uma vez o endereço, por exemplo `http://192.168.2.60:5050/output/retono`. O nome `retono` é literal, informado pelo usuário.
6. Após confirmar uma fonte, a busca para. Ao reabrir, o modo, a origem e o enquadramento salvos são retomados.
7. Para testar NDI em API 24+, abra o seletor **HTML → NDI**. Se fechar nessa etapa, abra o app novamente e fotografe o relatório da recuperação.

A recuperação HTML não apaga a origem NDI. Uma nova fonte confirmada salva o novo modo/origem. Nenhum relatório é enviado automaticamente.

## Diagnóstico e enquadramento

Abra **engrenagem → Diagnóstico do aparelho** e fotografe o conteúdo. Inclui versão/pacote, fabricante/modelo, Android Release/API, ABIs, densidade, estado NDI, motor WebView, área CSS, suporte a Grid/variáveis/clamp e eventual relatório da abertura anterior.

**Exibição HTML → Monitor 16:9 — compatibilidade TV Box** continua sendo o padrão inicial em API 23–25. Preserva a densidade do firmware e ajusta o canvas de 1920 × 1080 CSS proporcionalmente. As alternativas de ajuste 16:9 e página original continuam disponíveis. Um WebView antigo pode não implementar o CSS/JavaScript do tema.

## Se continuar fechando antes de mostrar o relatório

Com ADB configurado no PC e a depuração autorizada na box:

```powershell
adb install -r Monitor-de-Retorno-1.4.6-TVBox-teste.apk
adb shell am start -n br.com.polegar.ndimonitor.tvboxtest/br.com.polegar.ndimonitor.StartupActivity
adb logcat -d -b crash -b main -v threadtime AndroidRuntime:E libc:F DEBUG:F NDIMonitorNative:V '*:S' > tvbox-log.txt
```

Não limpe os buffers antes da coleta: podem conter a causa da falha anterior. Os comandos suportados variam conforme o firmware. Confira o arquivo antes de compartilhar e remova dados pessoais/tokens.

## Limites e compilação

HTML exige API 23 e WebView funcional; NDI exige API 24, ARM32/ARM64 e runtime compatível com o aparelho. O registro Java não intercepta SIGSEGV/SIGILL. Uma operação interrompida pode ser identificada pelo checkpoint, mas a análise nativa completa depende do Logcat/tombstone.

Não houve acesso às boxes ou à LAN do usuário. As fotos confirmaram a abertura da 1.4.5 na HTV, mas a revisão 1.4.6 ainda exige teste físico de HTML, enquadramento e mídia. Não comprova o recebimento do áudio associado a letras. Nenhum pacote foi enviado à Play ou publicado como release.

```bash
./gradlew testDebugUnitTest lintDebug assembleTvboxTest
```

O reuso opcional `-PmonitorPrebuiltNativeDir=/caminho/nativo` continua restrito a depuração, exige as quatro bibliotecas ARM32/ARM64 e desabilita release. São as mesmas bibliotecas/avisos do APK 1.4.5-TVBox verificado. Veja [VALIDACAO_1.4.6.md](VALIDACAO_1.4.6.md).

## Aparelhos observados nas fotos

| Aparelho | Android / API informados | Evidência disponível |
| --- | --- | --- |
| Amlogic HTV-6H | 7.1.2 / 25 | 1.4.5 abriu menu/diagnóstico; HTML Chrome 52 falha no programa original |
| B9 | 6.0.1 / API ainda não lida no app | Fechamento fotografado; versão instalada/causa não confirmadas |
| BTV E10 | 9 / 28 | 1.4.3 abre; WebView Chrome 66, clamp ausente, página original |
| TV Philco/Skyworth | 14 / 34 | 1.4.3 abre e mostra diagnóstico; modo 16:9 |

Esses registros não confirmam vídeo/áudio NDI, áudio HTML ou estabilidade em todos os aparelhos. A escolha **Monitor 16:9 — compatibilidade TV Box** continua sendo o teste de enquadramento proposto para a BTV.
