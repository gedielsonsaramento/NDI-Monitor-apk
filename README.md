# Monitor de Retorno — Android 7+

Aplicativo Android independente para receber vídeo e áudio NDI® Standard full-bandwidth ou exibir um retorno HTML pela rede local em celulares e dispositivos Android de uso geral.

Versão de teste: **1.3.0** (`versionCode 4`). Nome provisório: **Monitor de Retorno**. Pacote Android preservado: `br.com.polegar.ndimonitor`.

Projeto independente. NDI® é marca registrada da Vizrt NDI AB. Conheça a tecnologia em [ndi.video](https://ndi.video/). Não há indicação de patrocínio ou certificação pela NDI/Vizrt.

## Recursos

- Dois modos no mesmo aplicativo: **NDI** e **HTML**, com a escolha salva por aparelho.
- Retorno HTML em tela cheia, endereço editável e salvo, recarga manual e novas tentativas de carregamento após falhas de rede. [Como configurar](RETORNO_HTML.md).
- Descoberta contínua enquanto a lista está aberta: novas fontes aparecem sem precisar reiniciar a busca.
- Atualização manual sem apagar a lista; fontes ausentes de sucessivas atualizações são removidas após 10 segundos de tolerância.
- Painel compacto no celular, com ícones menores e área de toque de pelo menos 48 dp, lista com rolagem e controles maiores em telas grandes.
- Recepção de vídeo por JNI/C++ e reprodução de áudio por `AudioTrack`.
- Tela cheia horizontal, com preservação da proporção da imagem.
- Memória da última fonte e espera pela reconexão do receptor.
- Menu e status ocultos durante a exibição; ao revelar os controles, eles se ocultam após 4 segundos com o painel fechado.
- Operação por toque, mouse ou controle remoto.
- Opção de abertura após iniciar o Android e de seleção como tela inicial da TV Box.
- Opções de sistema, escolha da tela inicial e avisos de licença reunidos na engrenagem, deixando a lista livre.

## Código-fonte e APK

Este repositório disponibiliza o código próprio do projeto sob a [licença MIT](LICENSE). Os headers, as bibliotecas e o instalador do SDK NDI **não estão incluídos**.

Esta revisão prepara uma versão de teste. A distribuição com `libndi.so` deve seguir os termos de redistribuição Standard, a atribuição e os avisos de terceiros. O escopo é somente recepção Standard, software instalado em aparelhos de uso geral dos usuários, sem HX/Advanced nem hardware dedicado ou bloqueado fornecido com o app. A licença MIT do projeto não concede direitos sobre componentes NDI. Leia [LICENCA_NDI_IMPORTANTE.md](LICENCA_NDI_IMPORTANTE.md).

Sem o SDK, o projeto pode ser compilado em **modo de preparação**. Nesse modo, o aplicativo informa que o SDK está ausente e não recebe fontes NDI.

## Requisitos de compilação

| Item | Configuração do projeto |
| --- | --- |
| Android mínimo | Android 7.0 / API 24 |
| Arquiteturas | ARM32 `armeabi-v7a` e ARM64 `arm64-v8a` |
| SDK de compilação / destino | API 36 / API 36 |
| JDK | 17 |
| Gradle / Android Gradle Plugin | 8.13 / 8.13.2 |
| Código nativo | NDK 28.2.13676358 e CMake 3.22.1 |
| Recepção real | SDK NDI para Android, instalado separadamente pelo usuário |

## Compilar

1. Baixe o código em **Code > Download ZIP** e extraia, ou clone este repositório.
2. No Android Studio, abra a **raiz que contém `settings.gradle`**, não somente a pasta `app`.
3. Configure o JDK e instale os componentes Android indicados acima.
4. Para recepção real, obtenha o [SDK oficial NDI](https://ndi.video/for-developers/ndi-sdk/), confira seus termos e execute o script de integração na raiz do projeto.

No Windows / PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\instalar_ndi.ps1 -SdkDir "C:\CAMINHO\NDI SDK for Android"
```

No Linux, macOS ou Git Bash:

```bash
sh ./tools/instalar_ndi.sh "/caminho/NDI SDK for Android"
```

Os scripts copiam os arquivos do SDK, inclusive `licenses/libndi_licenses.txt` para os assets do aplicativo, e removem os caches gerados `app/.cxx` e `app/build` para que a próxima compilação detecte a integração. Eles não baixam o SDK nem aceitam sua licença.

Sincronize o Gradle. Para gerar o APK de teste, use **Build > Generate App Bundles or APKs > Generate APKs**, conforme a versão do Android Studio, ou execute na raiz:

```powershell
.\gradlew.bat :app:assembleDebug
```

```bash
sh ./gradlew :app:assembleDebug
```

Saída esperada: `app/build/outputs/apk/debug/app-debug.apk`.

Essa compilação é de **teste (debug)**. Para atualizar uma instalação existente, use a mesma chave de assinatura. Não envie chaves de assinatura ao repositório.

Veja o [guia passo a passo](GUIA_PASSO_A_PASSO.md) para a instalação do SDK e a solução de dificuldades comuns.

## Usar no aparelho

1. Instale seu APK de teste e mantenha o aparelho e o transmissor na mesma rede local.
2. Abra o painel de fontes e selecione uma origem. A busca segue ativa depois da primeira fonte; **↻** consulta a descoberta atual sem limpar a lista.
3. Toque no vídeo para revelar os controles; toque no menu para abrir o painel.
4. No controle remoto, **OK/Enter/Menu** revela os controles e um novo acionamento abre o painel. **Voltar** abre ou fecha o painel.

Na engrenagem **Configurações**, a opção **Escolher tela inicial** permite escolher ou restaurar o launcher do aparelho. Essa opção é voluntária e não impede acesso a outros aplicativos. Não há bloqueio de hardware nesta versão.

Leia [QUIOSQUE.md](QUIOSQUE.md) antes de mudar a tela inicial.

## Limitações conhecidas

- O áudio no modo HTML depende do conteúdo que a página fornece. A reprodução de vídeos, o áudio, o tema e a atualização do retorno devem ser validados com o Spresenter real. É necessário um componente WebView funcional no aparelho.
- A opção de tela inicial é um **launcher opcional**, não um quiosque gerenciado completo. Não bloqueia outros aplicativos, não encerra serviços de terceiros e não garante ganho de desempenho.
- A abertura após o boot depende do firmware e das restrições de [início de atividades em segundo plano do Android](https://developer.android.com/guide/components/activities/background-starts). Teste no aparelho escolhido; não há garantia para todo dispositivo Android 7+.
- O CMake e o NDK foram atualizados para alinhamento de 16 KB. Além de conferir o ELF e o empacotamento do APK/AAB, teste vídeo, áudio e reconexão em aparelho com páginas de 16 KB; só a configuração de compilação não confirma o funcionamento no dispositivo. Consulte a [documentação Android](https://developer.android.com/guide/practices/page-sizes).
- Não há versão x86/x86_64 nem matriz de compatibilidade com todas as variantes de fontes NDI.
- Os testes relatados pelo mantenedor não substituem uma validação de estabilidade, vídeo, áudio, reconexão e reinício no equipamento de uso final.

## Documentação

- [Modo HTML e troca entre fontes](RETORNO_HTML.md)
- [Guia de instalação e compilação](GUIA_PASSO_A_PASSO.md)
- [Tela inicial opcional e como desativar](QUIOSQUE.md)
- [Licença e cuidados com o SDK NDI](LICENCA_NDI_IMPORTANTE.md)
- [Histórico de alterações](CHANGELOG.md)
- [Resultados e limites da validação 1.2.0](VALIDACAO_1.2.md)
- [Resultados e limites da validação 1.3.0](VALIDACAO_1.3.md)

## Verificações de desenvolvimento

- `sh tools/test-native-discovery.sh`: regressões do descobridor com uma implementação simulada da API pública NDI, sem redistribuir o SDK.
- `./gradlew :app:testDebugUnitTest :app:lintDebug`: catálogo de fontes e layout em telas pequenas/grandes. Os testes de layout usam Android simulado, não substituem o teste com rede e transmissor reais.

Para a Play, ainda são necessários termos para usuários e política de privacidade aplicáveis, ficha da loja, assinatura de produção, AAB e validação em aparelho. O build release verifica a presença dos componentes locais e avisos, mas não substitui essas etapas.
