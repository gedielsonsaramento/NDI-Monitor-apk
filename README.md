# S-Retorno — Android 6+ para HTML; Android 7+ para NDI

Aplicativo Android independente para receber vídeo e áudio NDI® Standard full-bandwidth ou exibir um retorno HTML pela rede local em celulares e dispositivos Android de uso geral.

Versão atual: **1.4.11** (`versionCode 16`). Nome exibido: **S-Retorno**, anteriormente Monitor de Retorno. O pacote principal do projeto permanece `br.com.polegar.ndimonitor`.

## Downloads

| Arquivo | Link |
| --- | --- |
| Aplicativo Android 1.4.11 | [Baixar S-Retorno-1.4.11.apk](https://github.com/gedielsonsaramento/NDI-Monitor-apk/raw/refs/heads/downloads-1.4.11/S-Retorno-1.4.11.apk) |
| Código-fonte da versão 1.4.11 | [Baixar código-fonte ZIP](https://github.com/gedielsonsaramento/NDI-Monitor-apk/archive/refs/heads/versao-1.4.11.zip) |
| Instalação e integridade | [Instruções e SHA-256](DOWNLOADS.md) |

Instale o APK sobre a edição TVBox anterior, sem desinstalar, para manter as preferências. Android 6 utiliza HTML; a recepção NDI exige Android 7 ou superior e runtime compatível.

## Testes nos aparelhos do projeto

Em **29/10/2026**, o mantenedor informou que **todas as boxes testadas rodaram perfeitamente, assim como a TV Philips com Android**. O relato registra o funcionamento nos equipamentos usados pelo projeto; não discrimina os testes por modo ou função. [Registro dos testes em aparelhos](TESTES_EM_APARELHOS.md).

Comece pela [documentação completa do projeto](DOCUMENTACAO_COMPLETA.md), com instalação, uso no controle remoto, arquitetura, compilação, testes, assinatura e preparação para publicação.

Projeto independente. NDI® é marca registrada da Vizrt NDI AB. Conheça a tecnologia em [ndi.video](https://ndi.video/). Não há indicação de patrocínio ou certificação pela NDI/Vizrt.

## Instalação em TVs e boxes

A versão **1.4.11** usa o nome **S-Retorno** no celular, na TV Box, na tela inicial e no banner Android TV. O ícone tem desenho vetorial próprio, com faixas dobradas em azul e branco formando uma seta de retorno sobre uma base de monitor. Há versões tradicional, redonda, adaptativa e monocromática. [Identidade visual](artwork/README.md).

O APK `S-Retorno-1.4.11.apk` mantém o pacote `br.com.polegar.ndimonitor.tvboxtest` e o certificado das edições 1.4.3–1.4.10-TVBox para atualizar essas instalações preservando as preferências. O sufixo técnico do pacote é mantido somente para compatibilidade; o nome e a versão exibidos não contêm “teste” ou “TV Box”. A edição habitual, como 1.4.2, permanece separada. Veja [TVBOX.md](TVBOX.md) e a [validação da 1.4.11](VALIDACAO_1.4.11.md).

A **1.4.10** corrige o envio dessa configuração: a 1.4.9 podia mostrar **Abra o endereço mostrado na TV** no navegador após enviar, mesmo com IP/porta corretos. A correção foi reproduzida e verificada em Chromium com o formulário e o servidor Java reais, sem chave particular. [Validação](VALIDACAO_1.4.10.md).

A **1.4.9** permite **Configurar pelo celular ou PC**: na box, abra **HTML → engrenagem → Saídas HTML do S-Present** e escolha essa opção. Digite no navegador do PC/celular o endereço mostrado na TV, informe o código curto e cole o link completo do Editor remoto do S-Present. A TV recebe a configuração e busca as saídas, sem digitar o token no controle. [Passo a passo](SAIDAS_SPRESENTER.md).

A **1.4.8** acrescenta **engrenagem → Saídas HTML do S-Present**. Uma conexão de API com permissão de leitura `advanced:read` lista todos os destinos HTML habilitados do PC, atualiza nomes e descobre novos caminhos após atualizar. O endpoint `/output/` da captura retorna `Cannot GET`; a busca anterior só tentava nomes conhecidos. A consulta autenticada usa os registros reais do servidor, preserva a fonte escolhida e mantém a adaptação Chrome 44 da 1.4.7. [Configurar a listagem de saídas](SAIDAS_SPRESENTER.md).

Android 6 abre HTML sem carregar as bibliotecas NDI, compiladas para API 24. Android 7 ou superior mantém o receptor NDI quando as bibliotecas carregam corretamente. A opção **16:9 — compatibilidade TV Box** usa a densidade real do motor web; é o padrão inicial em API 23–25. Não acrescenta recursos de CSS ausentes no WebView da box.

O relatório real da HTV-6H com Android 7.1.2/API 25 localizou o fechamento na leitura de `fontFamily` ao criar o botão NDI/HTML. A versão 1.4.5 substituiu a fonte textual global por `android:typeface` sem serifa, armazenado como inteiro. As fotos seguintes confirmaram a abertura do menu/diagnóstico na HTV, com biblioteca NDI inicializada; isso não confirma vídeo/áudio NDI. O relatório antigo da 1.4.4 permanece como histórico.

A HTV informou Chrome 52 e erro `Unexpected token function`. A foto seguinte da **BTV B9**, Android **6.0.1/API 23**, confirma o menu da **1.4.6** aberto, mas registra Chrome **44.0.2403.119** e `Unexpected token )` em `main.ff832fdd.js:2`. A **1.4.7** prepara os dois arquivos conhecidos dessa captura como **ES5**, com fallbacks de APIs/CSS, e estende o perfil para **Chrome 44–86**. Ela só substitui um recurso depois de verificar seu SHA-256 no servidor da fonte selecionada. Páginas novas, arquivos diferentes e motores atuais mantêm o carregamento original. Veja [como preparar a captura](tools/html-compat/README.md) e os [resultados e limites](VALIDACAO_1.4.7.md). O relato posterior de funcionamento nas boxes está no [registro de 29/10/2026](TESTES_EM_APARELHOS.md).

## Recursos

- Dois modos no mesmo aplicativo: **NDI** e **HTML**, com modo e fonte confirmada salvos por aparelho e restaurados ao iniciar.
- Retorno HTML com exibição de monitor 16:9: área de referência de 1920 × 1080 ajustada inteira à tela, texto em 100% e opção de layout original da página na engrenagem. Endereço e escolha de exibição salvos, recarga manual e novas tentativas após falhas de rede. [Como configurar](RETORNO_HTML.md).
- Catálogo HTML do PC configurado pela API: nomes atuais, novos destinos, várias saídas por computador e remoção de destinos desabilitados/excluídos após uma resposta válida. Configura um PC por instalação, com chave de leitura restrita à origem informada. [Como configurar](SAIDAS_SPRESENTER.md).
- Busca HTML anterior em Wi-Fi/Ethernet IPv4 para PCs sem conexão de API: tenta a porta 5050 com os nomes `saida-html-1`, `retorno` e `retono`, além do nome salvo elegível. É uma busca de caminhos conhecidos, sem enumeração de nomes arbitrários. Aceita o redirecionamento Stage no mesmo servidor. Durante a busca solicitada, repete 30 s depois de cada ciclo com o painel aberto.
- A busca começa na primeira configuração sem fonte ou pelo botão atualizar. Depois de selecionar, os descobridores são parados e o finder NDI é liberado. Reabrir apenas o painel não reinicia a busca; use atualizar para procurar outra fonte.
- Ao iniciar com uma fonte salva, o aplicativo abre a mesma origem diretamente e mantém o painel fechado. A recuperação de sinal continua direcionada a essa origem.
- Atualização manual sem apagar a lista: tolerância de ausência de 10 s para NDI e 90 s para HTML; o endereço HTML salvo permanece disponível.
- Painel compacto no celular, com ícones menores e área de toque de pelo menos 48 dp, lista com rolagem e controles maiores em telas grandes.
- Recepção de vídeo por JNI/C++ e reprodução de áudio por `AudioTrack`.
- Tela cheia horizontal, com preservação da proporção da imagem.
- Memória da última fonte e espera pela reconexão do receptor; navegar por um modo ainda sem fonte não substitui a última configuração confirmada.
- Menu e status ocultos durante a exibição; ao revelar os controles, eles se ocultam após 4 segundos com o painel fechado.
- Operação por toque, mouse ou controle remoto.
- Opção de abertura após iniciar o Android e de seleção como tela inicial da TV Box.
- Opções de sistema, escolha da tela inicial e avisos de licença reunidos na engrenagem, deixando a lista livre.

## Código-fonte e APK

Este repositório disponibiliza o código próprio do projeto sob a [licença MIT](LICENSE). Os headers, as bibliotecas e o instalador do SDK NDI **não estão incluídos**.

O APK disponível na seção Downloads é o mesmo **1.4.11 (16)** entregue e verificado, com a assinatura de compatibilidade das edições TVBox anteriores. É uma compilação de desenvolvimento, separada do arquivo ZIP do código-fonte; não é uma publicação na Play. O binário inclui o runtime NDI e seus avisos, mas não o instalador nem os headers do SDK. A distribuição com `libndi.so` segue os termos próprios do componente, a atribuição e os avisos de terceiros. O escopo é somente recepção Standard, software instalado em aparelhos de uso geral dos usuários, sem HX/Advanced nem hardware dedicado ou bloqueado fornecido com o app. A licença MIT do projeto não concede direitos sobre componentes NDI. Leia [LICENCA_NDI_IMPORTANTE.md](LICENCA_NDI_IMPORTANTE.md).

Sem o SDK, o projeto pode ser compilado em **modo de preparação**. Nesse modo, o aplicativo informa que o SDK está ausente e não recebe fontes NDI.

## Requisitos de compilação

| Item | Configuração do projeto |
| --- | --- |
| Android mínimo | Android 6.0 / API 23 para HTML; Android 7.0 / API 24 para NDI |
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
2. Na primeira configuração, selecione uma origem na lista. A busca para após a seleção. Para trocar depois, abra o painel e pressione **↻**; durante essa busca, novas fontes aparecem sem apagar imediatamente as anteriores.
3. Toque no vídeo para revelar os controles; toque no menu para abrir o painel.
4. No controle remoto, **OK/Enter/Menu** revela os controles e um novo acionamento abre o painel. **Voltar** abre ou fecha o painel.

Para HTML, escolha **HTML — retornos na rede**, aguarde os candidatos e selecione um deles. Com endereço já salvo, ele é reaberto. **Informar endereço** fica como alternativa; o botão ao lado recarrega a página salva. O ícone do cabeçalho solicita uma busca por outras fontes.

Na engrenagem **Configurações**, a opção **Escolher tela inicial** permite escolher ou restaurar o launcher do aparelho. Essa opção é voluntária e não impede acesso a outros aplicativos. Não há bloqueio de hardware nesta versão.

Leia [QUIOSQUE.md](QUIOSQUE.md) antes de mudar a tela inicial.

## Limitações conhecidas

- Sem a conexão de API, a busca HTML testa os caminhos conhecidos e não enumera nomes novos. O catálogo autenticado lista os destinos HTML habilitados de um PC configurado por instalação. Em redes maiores que /24, a varredura anterior usa somente a faixa /24 do aparelho. IPv6, nomes de host e links personalizados continuam disponíveis pelo endereço manual. Um HTML encontrado é um candidato, cuja exibição precisa ser conferida.
- O áudio no modo HTML depende do conteúdo que a página fornece. A reprodução de vídeos, o áudio, o tema e a atualização do retorno devem ser validados com o Spresenter real. É necessário um componente WebView funcional no aparelho.
- A opção de tela inicial é um **launcher opcional**, não um quiosque gerenciado completo. Não bloqueia outros aplicativos, não encerra serviços de terceiros e não garante ganho de desempenho.
- A abertura após o boot depende do firmware e das restrições de [início de atividades em segundo plano do Android](https://developer.android.com/guide/components/activities/background-starts). Teste no aparelho escolhido; não há garantia para todo dispositivo Android 7+.
- O CMake e o NDK foram atualizados para alinhamento de 16 KB. Além de conferir o ELF e o empacotamento do APK/AAB, teste vídeo, áudio e reconexão em aparelho com páginas de 16 KB; só a configuração de compilação não confirma o funcionamento no dispositivo. Consulte a [documentação Android](https://developer.android.com/guide/practices/page-sizes).
- Não há versão x86/x86_64 nem matriz de compatibilidade com todas as variantes de fontes NDI.
- Os testes relatados pelo mantenedor não substituem uma validação de estabilidade, vídeo, áudio, reconexão e reinício no equipamento de uso final.

## Documentação

- [Downloads, instalação e SHA-256 da 1.4.11](DOWNLOADS.md)
- [Testes nos aparelhos relatados pelo mantenedor](TESTES_EM_APARELHOS.md)
- [Resultados e limites da validação 1.4.11](VALIDACAO_1.4.11.md)
- [Manual completo do projeto 1.4.10](DOCUMENTACAO_COMPLETA.md)
- [Listagem dos destinos HTML do S-Present pela API](SAIDAS_SPRESENTER.md)
- [Compatibilidade HTML de uma captura com motores antigos](tools/html-compat/README.md)
- [Resultados e limites da validação 1.4.10](VALIDACAO_1.4.10.md)
- [Histórico da validação 1.4.7](VALIDACAO_1.4.7.md)
- [Histórico da validação 1.4.6](VALIDACAO_1.4.6.md)
- [Modo HTML e troca entre fontes](RETORNO_HTML.md)
- [Guia de instalação e compilação](GUIA_PASSO_A_PASSO.md)
- [Tela inicial opcional e como desativar](QUIOSQUE.md)
- [Licença e cuidados com o SDK NDI](LICENCA_NDI_IMPORTANTE.md)
- [Histórico de alterações](CHANGELOG.md)
- [Resultados e limites da validação 1.2.0](VALIDACAO_1.2.md)
- [Resultados e limites da validação 1.3.0](VALIDACAO_1.3.md)
- [Resultados e limites da validação 1.4.0](VALIDACAO_1.4.md)
- [Resultados e limites da validação 1.4.3](VALIDACAO_1.4.3.md)
- [Resultados e limites da validação 1.4.4](VALIDACAO_1.4.4.md)
- [Resultados e limites da validação 1.4.5](VALIDACAO_1.4.5.md)

## Verificações de desenvolvimento

- `sh tools/test-native-discovery.sh`: regressões do descobridor com uma implementação simulada da API pública NDI, sem redistribuir o SDK.
- `./gradlew :app:testDebugUnitTest :app:lintDebug`: URLs, catálogos, faixas IPv4, HTTP, cancelamento, seleção e layout. Os testes usam servidores controlados e Android simulado; não substituem a rede e o transmissor reais.

Para a Play, ainda são necessários termos para usuários e política de privacidade aplicáveis, ficha da loja, assinatura de produção, AAB e validação em aparelho. O build release verifica a presença dos componentes locais e avisos, mas não substitui essas etapas.
