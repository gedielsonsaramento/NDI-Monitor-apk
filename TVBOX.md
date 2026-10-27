# Instalação em TVs e boxes — S-Retorno 1.4.11

A versão **1.4.11 (16)** troca o nome e o ícone do aplicativo. Na lista de apps, na tela inicial e no instalador, procure **S-Retorno**. O APK é `S-Retorno-1.4.11.apk`; a versão exibida também é `1.4.11`, sem o sufixo `-tvbox`. A assinatura e o pacote são os mesmos das edições TVBox anteriores, permitindo atualizar sem desinstalar e manter modo, fonte, enquadramento e configuração da API. Esta revisão mantém o comportamento de recepção e conexão da 1.4.10.

## Listar destinos novos do S-Present

Instale a **1.4.11 (16)** sobre a edição TVBox anterior, sem desinstalar. Em HTML, use **engrenagem → Saídas HTML do S-Present → Configurar pelo celular ou PC**. A TV mostra seu próprio endereço e um código de seis dígitos. No navegador do PC ou celular da mesma rede, abra esse endereço, informe o código e cole o link completo de **Copiar link com token** do Editor remoto do S-Present. Clique em **Enviar para esta TV** e selecione a saída na lista da box. Não é necessário digitar o token com o controle.

A 1.4.10 corrige a recusa do formulário da 1.4.9. Após atualizar, feche a aba antiga no navegador e abra uma nova sessão na TV. Use o endereço e o código atuais exibidos nessa nova sessão; a porta anterior pode mudar.

O código expira em cinco minutos; fechar a tela/sair do app encerra a sessão. Se a página não abrir, confira a mesma rede e possível isolamento de clientes. A configuração detalhada, as permissões do link e a alternativa manual estão em [SAIDAS_SPRESENTER.md](SAIDAS_SPRESENTER.md). O IP do S-Present observado é 192.168.2.53, diferente do IP da box mostrado na sessão temporária.

Depois de salvar, confira todos os destinos HTML habilitados do PC. Renomeie um destino e crie outro no S-Present; pressione **↻** e confira nome/caminho atualizados. Desabilite um destino não selecionado e confira sua remoção após atualizar. Se alterar o caminho da fonte escolhida, selecione a nova entrada; o último endereço salvo é preservado como histórico. Feche/reabra o app para conferir a restauração sem busca geral. A captura `Erro.html` confirma que `/output/` não fornece uma listagem nessa instalação.

Revisão candidata de 26/10/2026. As fotos confirmam a abertura do menu/diagnóstico da **1.4.5** na **Amlogic HTV-6H**, Android **7.1.2 / API 25**, e da **1.4.6** na **BTV B9**, Android **6.0.1 / API 23**. A HTV informa Chrome 52 e a B9, Chrome 44. A 1.4.7 adapta o programa/estilo da captura conhecida para ES5 e Chrome 44–86; a exibição após a adaptação ainda precisa de teste físico.

## Confirmar a adaptação HTML

Instale a **1.4.11 sobre a edição TVBox anterior**, mantendo as preferências, e abra a mesma fonte HTML. Na B9/HTV, use **Exibição HTML → Monitor 16:9 — compatibilidade TV Box**. Faça uma mudança de letra ou visitante no computador e confira a atualização na tela.

Em **Diagnóstico do aparelho**, o início deve identificar **1.4.11 (16)**. No final, procure **Compatibilidade HTML: adaptação aplicada a 2 de 2 arquivos (Chrome 44)** na B9, ou Chrome 52 na HTV. A resposta pode levar até cinco segundos. Se ficar preto ou deformado, fotografe essa parte, a mensagem de erro e a linha **Arquivo**, se houver. Feche/reabra para conferir a restauração sem busca geral.

O perfil foi preparado de `main.ff832fdd.js` e `main.eb127441.css` de `Captura-Retorno.zip`. Em Chrome 44–86, verifica nome, origem e SHA-256 dos bytes atuais do servidor antes de usar a adaptação. Outros arquivos/builds, perfis ausentes ou motores atuais mantêm a página original. O WebSocket e a mídia continuam na fonte selecionada; não exige serviço extra no PC nem substitui o WebView. Consulte [preparo de outra captura](tools/html-compat/README.md).

## Instalar e abrir

1. Use `S-Retorno-1.4.11.apk`.
2. Abra o ícone **S-Retorno**. O pacote `br.com.polegar.ndimonitor.tvboxtest` e o certificado da 1.4.3-TVBox foram mantidos para atualizar essa edição. A instalação habitual, como 1.4.2, permanece separada.
3. Sem configuração anterior, API 23–25 entra em HTML sem carregar NDI. Uma fonte NDI já selecionada é restaurada normalmente.
4. Se aparecer a recuperação com um relatório anterior, escolha **Abrir apenas HTML** para testar a nova interface. Fotografe um novo relatório se houver outra falha. Queda de energia ou encerramento também podem interromper a abertura; somente um checkpoint não comprova uma falha nativa.
5. Escolha a mesma saída HTML ou informe uma vez o endereço, por exemplo `http://192.168.2.52:5050/output/retorno`. Use o IP e nome de saída atuais do PC.
6. Após confirmar uma fonte, a busca para. Ao reabrir, o modo, a origem e o enquadramento salvos são retomados.
7. Para testar NDI em API 24+, abra o seletor **HTML → NDI**. Se fechar nessa etapa, abra o app novamente e fotografe o relatório da recuperação.

A recuperação HTML não apaga a origem NDI. Uma nova fonte confirmada salva o novo modo/origem. Nenhum relatório é enviado automaticamente.

## Diagnóstico e enquadramento

Abra **engrenagem → Diagnóstico do aparelho** e fotografe o conteúdo. Inclui versão/pacote, fabricante/modelo, Android Release/API, ABIs, densidade, estado NDI, motor WebView, área CSS, suporte a Grid/variáveis/clamp e eventual relatório da abertura anterior.

**Exibição HTML → Monitor 16:9 — compatibilidade TV Box** continua sendo o padrão inicial em API 23–25. Preserva a densidade do firmware e ajusta o canvas de 1920 × 1080 CSS proporcionalmente. As alternativas de ajuste 16:9 e página original continuam disponíveis. Um WebView antigo pode não implementar o CSS/JavaScript do tema.

## Se continuar fechando antes de mostrar o relatório

Com ADB configurado no PC e a depuração autorizada na box:

```powershell
adb install -r S-Retorno-1.4.11.apk
adb shell am start -n br.com.polegar.ndimonitor.tvboxtest/br.com.polegar.ndimonitor.StartupActivity
adb logcat -d -b crash -b main -v threadtime AndroidRuntime:E libc:F DEBUG:F NDIMonitorNative:V '*:S' > tvbox-log.txt
```

Não limpe os buffers antes da coleta: podem conter a causa da falha anterior. Os comandos suportados variam conforme o firmware. Confira o arquivo antes de compartilhar e remova dados pessoais/tokens.

## Limites e compilação

HTML exige API 23 e WebView funcional; NDI exige API 24, ARM32/ARM64 e runtime compatível com o aparelho. O registro Java não intercepta SIGSEGV/SIGILL. Uma operação interrompida pode ser identificada pelo checkpoint, mas a análise nativa completa depende do Logcat/tombstone.

Não houve acesso às boxes ou à LAN do usuário. As fotos anteriores confirmaram a abertura da 1.4.5 na HTV e da 1.4.6 na B9. As novas fotos confirmam a tela de configuração pelo PC/celular e o acesso HTTP do PC à box, seguido da recusa do envio corrigida na 1.4.10. A nova revisão ainda exige teste físico de recebimento, API, HTML, enquadramento e mídia. Não comprova o recebimento do áudio associado a letras. Android 6 continua somente em HTML; a indicação NDI indisponível/API 24 é esperada. Nenhum pacote foi enviado à Play ou publicado como release.

```bash
./gradlew testDebugUnitTest lintDebug assembleTvboxTest
```

O reuso opcional `-PmonitorPrebuiltNativeDir=/caminho/nativo` continua restrito a depuração, exige as quatro bibliotecas ARM32/ARM64 e desabilita release. São as mesmas bibliotecas/avisos do APK 1.4.8-TVBox verificado. Veja [VALIDACAO_1.4.10.md](VALIDACAO_1.4.10.md).

## Aparelhos observados nas fotos

| Aparelho | Android / API informados | Evidência disponível |
| --- | --- | --- |
| Amlogic HTV-6H | 7.1.2 / 25 | 1.4.5 abriu menu/diagnóstico; HTML Chrome 52 falha no programa original |
| BTV B9 | 6.0.1 / 23 | 1.4.6 abriu menu/diagnóstico; ARM32; Chrome 44; `Unexpected token )` no programa original; histórico sem exceção Java |
| BTV E10 | 9 / 28 | 1.4.3 abre; WebView Chrome 66, clamp ausente, página original |
| TV Philco/Skyworth | 14 / 34 | 1.4.3 abre e mostra diagnóstico; modo 16:9 |

Esses registros não confirmam vídeo/áudio NDI, áudio HTML ou estabilidade em todos os aparelhos. A escolha **Monitor 16:9 — compatibilidade TV Box** continua sendo o teste de enquadramento proposto para a BTV.
