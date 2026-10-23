# Teste em TV Box — Monitor de Retorno 1.4.3

Revisão candidata de 22/10/2026. Foram relatados erro de análise/instalação, fechamento contínuo e HTML comprimido no canto. O usuário identificou a última imagem como HTML dentro do app 1.4.2 e informou Android 6. O APK 1.4.2 entregue declara mínimo API 24 e não pode ser instalado normalmente em API 23. A edição de teste acrescenta leitura do Android Release e do nível de API para esclarecer essa diferença no próprio aparelho, sem presumir modelo ou firmware a partir do nome comercial da box.

## Instalar a edição de teste

1. Use `Monitor-de-Retorno-1.4.3-TVBox-teste.apk`.
2. O ícone se chama **Monitor de Retorno — TV Box**. O pacote é `br.com.polegar.ndimonitor.tvboxtest`; instala em paralelo à edição habitual e usa preferências próprias.
3. Abra esse ícone. Em Android 6 / API 23, o app inicia no modo HTML e não inicializa as bibliotecas NDI de API 24.
4. Selecione a saída HTML descoberta. Se necessário, informe uma vez o endereço exibido no Spresenter. O endereço informado neste teste foi `http://192.168.2.60:5050/output/retono`; o trecho `retono` é literal, conforme o destino do usuário.
5. Depois da seleção, a busca para. Ao reabrir, a mesma origem e a escolha de exibição são restauradas.

## Conferir a exibição

No modo HTML, abra **engrenagem → Exibição HTML → Monitor 16:9 — compatibilidade TV Box**. Essa opção é o padrão inicial em API 23–25. Mantém a densidade real do WebView e escala o canvas de 1920 × 1080 CSS proporcionalmente até caber na tela, sem forçar desenho por CPU.

As opções **Monitor 16:9 — ajustar à tela** e **Layout original da página** continuam disponíveis. A escolha fica salva. Trocar a opção reabre somente o mesmo endereço, sem começar uma nova busca de rede.

Se as letras continuarem comprimidas, abra **engrenagem → Diagnóstico do aparelho** e fotografe o conteúdo (role a caixa para mostrar o restante, se necessário). Abra essa tela em cada box com problema. Ela mostra:

- versão/pacote do app, modelo, Android Release e API;
- arquiteturas e densidade informadas pelo firmware;
- estado de carregamento da biblioteca NDI;
- identificação do motor WebView e área nativa/CSS da página;
- suporte do motor a CSS Grid, variáveis CSS e `clamp()` e eventual último erro JavaScript.

O diagnóstico é local, sob demanda e de leitura: não transmite dados, muda a página ou pesquisa fontes. Um motor antigo pode não entender o CSS/JavaScript do tema, mesmo quando o endereço abre. O modo de compatibilidade não acrescenta recursos ausentes no motor. O diagnóstico permite decidir se o ajuste deve ser no app, no componente web da box ou no tema do retorno.

## Limites

- HTML: mínimo API 23. NDI: mínimo API 24, ARM32/ARM64 e biblioteca carregável nesse aparelho.
- API mínima não comprova desempenho ou compatibilidade com todo firmware.
- `LinkageError` é tratado; falhas nativas como SIGSEGV/SIGILL ainda precisam de log.
- Não houve acesso remoto à rede local ou às boxes. A correção da imagem real ainda precisa ser confirmada nelas.
- Essa edição de desenvolvimento não é um pacote de produção para a Play Store.

## Se o app fechar antes do diagnóstico

Com ADB já configurado no PC e a depuração autorizada na box, a saída da instalação ajuda a separar versão mínima, certificado, arquitetura e arquivo inválido:

```powershell
adb install -r Monitor-de-Retorno-1.4.3-TVBox-teste.apk
adb logcat -d -v threadtime AndroidRuntime:E libc:F DEBUG:F NDIMonitor:V '*:S' > tvbox-log.txt
```

Não é preciso limpar dados ou remover a edição habitual para usar o pacote de teste em paralelo.

## Compilação

Com SDK NDI e ferramentas nativas instalados normalmente:

```bash
./gradlew testDebugUnitTest lintDebug assembleTvboxTest
```

O build opcional `-PmonitorPrebuiltNativeDir=/caminho/nativo` permite depuração com as mesmas quatro bibliotecas de um APK previamente verificado. Não reconstrói o código C++, exige ARM32/ARM64 completos e desabilita a variante release. Nessa validação, as bibliotecas foram recuperadas do APK 1.4.2 de SHA-256 `2449ee838f926a27a613dc4055061940865df32a26821c747d7d220aa9c086c8`, sem alteração de bytes. Os avisos do SDK também são preservados.

Veja [VALIDACAO_1.4.3.md](VALIDACAO_1.4.3.md) para os resultados finais.
