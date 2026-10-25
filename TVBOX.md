# Teste em TV Box — Monitor de Retorno 1.4.4

Revisão candidata de 25/10/2026. A foto identifica modelo **H6+**, Android anunciado **7.1.2**, compilação **NHG47L.20200724 dev-keys**, kernel 3.14.29. O app 1.4.3-TVBox fecha antes do menu. Ainda não há stack trace nem confirmação da API/ABI efetiva dessa box; a foto não prova a causa do fechamento.

## Instalar e abrir

1. Use `Monitor-de-Retorno-1.4.4-TVBox-teste.apk`.
2. Abra o ícone **Monitor de Retorno — TV Box**. O pacote `br.com.polegar.ndimonitor.tvboxtest` e o certificado da 1.4.3-TVBox foram mantidos para atualizar essa edição. A instalação habitual, como 1.4.2, permanece separada.
3. Sem configuração anterior, API 23–25 entra em HTML sem carregar NDI. Uma fonte NDI já selecionada é restaurada normalmente.
4. Se aparecer a recuperação, fotografe o relatório antes de continuar e escolha **Abrir apenas HTML**. Queda de energia ou encerramento também podem interromper a abertura; somente um checkpoint não comprova uma falha nativa.
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
adb install -r Monitor-de-Retorno-1.4.4-TVBox-teste.apk
adb shell am start -n br.com.polegar.ndimonitor.tvboxtest/br.com.polegar.ndimonitor.StartupActivity
adb logcat -d -b crash -b main -v threadtime AndroidRuntime:E libc:F DEBUG:F NDIMonitorNative:V '*:S' > tvbox-log.txt
```

Não limpe os buffers antes da coleta: podem conter a causa da falha anterior. Os comandos suportados variam conforme o firmware. Confira o arquivo antes de compartilhar e remova dados pessoais/tokens.

## Limites e compilação

HTML exige API 23 e WebView funcional; NDI exige API 24, ARM32/ARM64 e runtime compatível com o aparelho. O registro Java não intercepta SIGSEGV/SIGILL. Uma operação interrompida pode ser identificada pelo checkpoint, mas a análise nativa completa depende do Logcat/tombstone.

Não houve acesso à H6+, às outras boxes ou à LAN do usuário. A revisão não comprova a solução do fechamento, do enquadramento ou do áudio associado a letras. Nenhum pacote foi enviado à Play ou publicado como release.

```bash
./gradlew testDebugUnitTest lintDebug assembleTvboxTest
```

O reuso opcional `-PmonitorPrebuiltNativeDir=/caminho/nativo` continua restrito a depuração, exige as quatro bibliotecas ARM32/ARM64 e desabilita release. São as mesmas bibliotecas/avisos do APK 1.4.3-TVBox verificado. Veja [VALIDACAO_1.4.4.md](VALIDACAO_1.4.4.md).
