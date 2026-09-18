# NDI® Monitor — Android 7+

Aplicativo Android para descobrir fontes de vídeo e áudio NDI na rede local e usar um celular ou uma TV Box como monitor.

Versão do projeto: **1.1.0** (`versionCode 2`). Pacote Android: `br.com.polegar.ndimonitor`.

Projeto independente. NDI® é marca registrada da Vizrt NDI AB. Conheça a tecnologia em [ndi.video](https://ndi.video/). Não há indicação de patrocínio ou certificação pela NDI/Vizrt.

## Recursos

- Busca e seleção de fontes na rede local.
- Recepção de vídeo por JNI/C++ e reprodução de áudio por `AudioTrack`.
- Tela cheia horizontal, com preservação da proporção da imagem.
- Memória da última fonte e espera pela reconexão do receptor.
- Menu e status ocultos durante a exibição; ao revelar os controles, eles se ocultam após 4 segundos com o painel fechado.
- Operação por toque, mouse ou controle remoto.
- Opção de abertura após iniciar o Android e de seleção como tela inicial da TV Box.
- Acesso às configurações do Android e à escolha do launcher original.
- Ícone de monitor e saída da compilação com o nome `NDI Monitor.apk`.

## Código-fonte e APK

Este repositório disponibiliza o código próprio do projeto sob a [licença MIT](LICENSE). Os headers, as bibliotecas e o instalador do SDK NDI **não estão incluídos**.

O APK que inclui `libndi.so` ainda não é disponibilizado aqui: sua publicação depende da confirmação das condições aplicáveis de distribuição do SDK. A licença MIT do projeto não concede direitos sobre componentes NDI. Leia [LICENCA_NDI_IMPORTANTE.md](LICENCA_NDI_IMPORTANTE.md).

Sem o SDK, o projeto pode ser compilado em **modo de preparação**. Nesse modo, o aplicativo informa que o SDK está ausente e não recebe fontes NDI.

## Requisitos de compilação

| Item | Configuração do projeto |
| --- | --- |
| Android mínimo | Android 7.0 / API 24 |
| Arquiteturas | ARM32 `armeabi-v7a` e ARM64 `arm64-v8a` |
| SDK de compilação / destino | API 34 / API 34 |
| JDK | 17 |
| Gradle / Android Gradle Plugin | 8.7 / 8.5.2 |
| Código nativo | NDK Android e CMake 3.22.1 |
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

Os scripts copiam os arquivos do SDK e removem os caches gerados `app/.cxx` e `app/build` para que a próxima compilação detecte a integração. Eles não baixam o SDK nem aceitam sua licença.

Sincronize o Gradle. Para gerar o APK de teste, use **Build > Generate App Bundles or APKs > Generate APKs**, conforme a versão do Android Studio, ou execute na raiz:

```powershell
.\gradlew.bat :app:assembleDebug
```

```bash
sh ./gradlew :app:assembleDebug
```

Saída esperada: `app/build/outputs/apk/debug/NDI Monitor.apk`.

Essa compilação é de **teste (debug)**. Para atualizar uma instalação existente, use a mesma chave de assinatura. Não envie chaves de assinatura ao repositório.

Veja o [guia passo a passo](GUIA_PASSO_A_PASSO.md) para a instalação do SDK e a solução de dificuldades comuns.

## Usar no aparelho

1. Instale seu APK de teste e mantenha o aparelho e o transmissor na mesma rede local.
2. Abra o painel de fontes, toque em **Buscar fontes** e selecione uma origem.
3. Toque no vídeo para revelar os controles; toque no menu para abrir o painel.
4. No controle remoto, **OK/Enter/Menu** revela os controles e um novo acionamento abre o painel. **Voltar** abre ou fecha o painel.

Para escolher o aplicativo como tela inicial da TV Box, use **Definir tela inicial / sair do quiosque**. Para voltar ao uso normal, selecione o launcher original nesse mesmo local. No celular, mantenha o launcher habitual se estiver apenas testando.

Leia [QUIOSQUE.md](QUIOSQUE.md) antes de mudar a tela inicial.

## Limitações conhecidas

- A opção de tela inicial é um **launcher opcional**, não um quiosque gerenciado completo. Não bloqueia outros aplicativos, não encerra serviços de terceiros e não garante ganho de desempenho.
- A abertura após o boot depende do firmware e das restrições de [início de atividades em segundo plano do Android](https://developer.android.com/guide/components/activities/background-starts). Teste no aparelho escolhido; não há garantia para todo dispositivo Android 7+.
- O APK testado apresentou aviso de alinhamento de 4 KB na biblioteca nativa. Esta revisão **não declara suporte a aparelhos configurados com páginas de 16 KB**. Essa compatibilidade exige validar a compilação nativa e todas as bibliotecas do SDK; consulte a [documentação Android](https://developer.android.com/guide/practices/page-sizes).
- Não há versão x86/x86_64 nem matriz de compatibilidade com todas as variantes de fontes NDI.
- Os testes relatados pelo mantenedor não substituem uma validação de estabilidade, vídeo, áudio, reconexão e reinício no equipamento de uso final.

## Documentação

- [Guia de instalação e compilação](GUIA_PASSO_A_PASSO.md)
- [Tela inicial opcional e como desativar](QUIOSQUE.md)
- [Licença e cuidados com o SDK NDI](LICENCA_NDI_IMPORTANTE.md)
- [Histórico de alterações](CHANGELOG.md)
