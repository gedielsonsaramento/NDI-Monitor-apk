# Monitor de Retorno

## Manual completo do projeto - versão 1.4.0

Aplicativo Android para receber NDI® Standard full-bandwidth ou abrir um retorno HTML pela rede local. Nome de produto provisório: Monitor de Retorno. Mantenedor: Gedielson Saramento. Documentação técnica revisada em 21/10/2026, horário do Brasil.

NDI® is a registered trademark of Vizrt NDI AB. https://ndi.video/

Esta revisão é uma versão de teste. O resultado de compilação, os testes executados e o que ainda precisa de equipamento real estão em VALIDACAO_1.4.md. A existência de um APK ou AAB compilado não significa publicação ou aprovação na Google Play.

Repositório: https://github.com/gedielsonsaramento/NDI-Monitor-apk

### Conteúdo deste manual

1. Finalidade e escopo
2. Requisitos e compatibilidade
3. Instalação e atualização
4. Primeiro uso do retorno HTML
5. Primeiro uso do NDI
6. Controle remoto, toque e tela inicial
7. Rede e funcionamento da busca HTML
8. Endereços personalizados e limites da descoberta
9. Preferências, dados e permissões
10. Arquitetura e arquivos do código
11. Preparar e compilar o projeto
12. Testes e diagnóstico
13. Assinatura e pacotes de produção
14. Preparação para GitHub e Google Play
15. Licenças e atribuição
16. Solução de dificuldades
17. Validação no equipamento final
18. Manutenção e referências

## 1. Finalidade e escopo

O aplicativo permite usar um celular, uma TV Box ou uma Android TV de uso geral como monitor de retorno. Cada aparelho escolhe seu próprio modo e sua própria fonte. Uma TV pode receber NDI e outra abrir o retorno HTML do mesmo computador. A exibição usa um modo por vez em cada aparelho.

No modo NDI, o vídeo é recebido pelo SDK NDI e exibido em uma superfície nativa. O áudio é reproduzido pelo Android. O projeto usa somente o escopo Standard full-bandwidth: não oferece NDI HX, HX2, HX3 nem recursos do Advanced SDK.

No modo HTML, o aplicativo abre a página fornecida pelo computador dentro de um WebView. A imagem, as informações e os recursos de áudio dependem dessa página. A busca automática acrescentada na versão 1.4 procura o endereço do padrão observado no retorno Stage do Spresenter.

O aplicativo não cria a apresentação, não controla os slides e não substitui o Spresenter. Ele também não depende do servidor Samsung/Termux ou do Stage Companion para sua instalação. Se esses sistemas alimentarem informações da página de retorno, essas informações serão exibidas como parte da própria página.

## 2. Requisitos e compatibilidade

| Item | Requisito ou configuração |
| --- | --- |
| Sistema mínimo | Android 7.0, API 24 |
| Processadores do APK | ARM32 armeabi-v7a e ARM64 arm64-v8a |
| Rede | Wi-Fi ou Ethernet com acesso ao computador transmissor |
| Busca HTML automática | Rede local com IPv4 privado; padrão TCP 5050 |
| Exibição HTML | Android System WebView funcional e compatível com a página |
| NDI real | SDK NDI para Android integrado; fonte Standard compatível |
| Orientação | Horizontal, com proporção preservada no vídeo NDI |
| Operação | Toque, mouse ou controle remoto com setas e OK/Enter |
| Pacote Android | br.com.polegar.ndimonitor |
| Versão | versionName 1.4.0, versionCode 5 |

O Android 5.1 do servidor Samsung não é compatível com este APK. Não há pacote x86/x86_64 nesta revisão. O funcionamento de vídeo, áudio, boot e WebView varia conforme o hardware e o firmware: a versão mínima não comprova compatibilidade com todas as TV Boxes.

Uma rede local funcionando é suficiente para o uso habitual de fontes locais. O conteúdo de uma página HTML pode exigir internet se carregar fontes, imagens, scripts ou outros recursos externos.

## 3. Instalação e atualização

Transfira o APK de teste ao aparelho, abra o arquivo e permita a instalação pelo aplicativo usado para abri-lo, quando o Android solicitar. Depois, abra Monitor de Retorno e conecte o aparelho à mesma rede do computador.

Atualizações exigem o mesmo identificador de pacote, um versionCode adequado e uma assinatura compatível com a instalação anterior. Um APK de teste assinado com outra chave não substitui a versão instalada, mesmo que seu nome e número de versão sejam maiores.

Se aparecer conflito de assinatura, a preferência é gerar a atualização com a chave anterior. Uma alternativa de teste é anotar o modo, o endereço e as opções do aparelho, desinstalar a versão antiga e instalar a nova. A desinstalação apaga as preferências locais; esta versão não oferece exportação dessas configurações. Não é necessário desinstalar quando a assinatura é compatível.

Os APKs de teste usam assinatura debug. Eles não constituem a chave de produção que deve acompanhar as futuras atualizações públicas. Guarde a chave de produção e seus dados de recuperação fora do repositório.

## 4. Primeiro uso do retorno HTML

No computador, ative a saída HTML de retorno/Stage e deixe o Spresenter aberto. Confira o endereço mostrado por ele. O padrão usado nesta revisão é:

```text
http://IP_DO_COMPUTADOR:5050/output/saida-html-1
```

No Android, abra o painel de fontes. Acione o seletor do cabeçalho e escolha HTML - retornos na rede. A busca começa automaticamente. Os endereços encontrados entram na lista à medida que respondem; não é necessário esperar o ciclo terminar.

Use as setas e OK do controle remoto para selecionar o retorno, ou toque na linha. O aplicativo salva o endereço nesse aparelho, fecha o painel e abre a página. Na próxima abertura, ele restaura o modo HTML e esse endereço. Não escolhe sozinho um servidor novo descoberto na rede.

Cada linha mostra o título recebido da página e o endereço do servidor, sem os parâmetros da URL. O título é apenas uma identificação fornecida pelo servidor; confira o conteúdo ao abrir. Encontrar HTML não comprova que a página contém o retorno correto ou que seu conteúdo continua sendo atualizado.

O ícone de atualização no cabeçalho refaz a busca sem apagar imediatamente as fontes conhecidas. O botão de recarga ao lado de Informar endereço recarrega a página já escolhida. As duas ações têm funções diferentes.

Para usar outro endereço, acione Informar endereço. Cole o link completo ou digite apenas o IP. Um IP sem protocolo é completado com a porta 5050 e o caminho /output/saida-html-1. Um link completo HTTP ou HTTPS preserva a porta, o caminho, os parâmetros e o fragmento.

## 5. Primeiro uso do NDI

Ative a fonte NDI Standard no computador e mantenha o Android em uma rede que permita sua descoberta e conexão. Abra o painel, selecione NDI e aguarde as fontes aparecerem. Selecione a origem desejada para iniciar a recepção e salvar sua identificação nesse aparelho.

A descoberta permanece ativa enquanto a lista está aberta. Encontrar a primeira fonte não encerra a busca. Atualizar consulta o descobridor existente e preserva as linhas; uma origem que permanece ausente é retirada após a tolerância de 10 segundos. A ordem das fontes já apresentadas é mantida enquanto continuam no catálogo.

O vídeo usa sua proporção original, com áreas pretas quando necessário. O receptor entrega até dois canais de áudio ao Android. O volume depende do aparelho e da saída de áudio selecionada no sistema.

Ao perder o sinal, o receptor informa que aguarda reconexão. Esse comportamento ainda precisa ser validado com o transmissor e a rede reais, inclusive quando o computador muda de IP ou nome. Não há suporte confirmado para todas as variantes de fontes NDI.

Ao trocar para HTML, a conexão NDI é encerrada. Ao trocar para NDI, o WebView é destruído para encerrar a mídia da página anterior. NDI e HTML mantêm suas memórias de fonte separadas.

## 6. Controle remoto, toque e tela inicial

| Ação | Resultado |
| --- | --- |
| Tocar na imagem | Revela o menu e o estado da exibição |
| OK/Enter/Menu com o painel fechado | Revela os controles; um novo acionamento abre o painel |
| Voltar | Abre ou fecha o painel de fontes |
| Setas na lista e OK | Permitem escolher uma fonte |
| Seletor NDI/HTML | Troca o modo deste aparelho |
| Atualizar no cabeçalho | Refaz a busca do modo atual |
| Informar endereço, no modo HTML | Abre o formulário manual |
| Recarga ao lado do formulário manual | Reabre a página HTML selecionada |
| Engrenagem | Mostra opções do monitor, Android e licenças |

Com o painel fechado, os controles se ocultam após quatro segundos. Os ícones são compactos, com áreas de toque de pelo menos 48 dp. Telas grandes usam controles maiores. A lista tem rolagem e o campo manual não ocupa a área principal das fontes.

A opção Escolher tela inicial abre as configurações do Android para selecionar Monitor de Retorno como Home. Para desfazer, abra a engrenagem e selecione o launcher original. Mantenha acesso às configurações do aparelho durante os testes.

Iniciar automaticamente com o Android é uma preferência do aplicativo. A abertura após o boot depende das restrições do Android e do firmware. O launcher é opcional: esta versão não é um quiosque gerenciado, não bloqueia outros apps e não encerra serviços de terceiros.

## 7. Rede e funcionamento da busca HTML

A busca HTML é independente da descoberta NDI. Ela não usa a API de controle do Spresenter, não requer chave de API e não altera apresentações. Envia requisições GET ao endereço de visualização configurado pelo projeto.

O aplicativo consulta as redes Wi-Fi e Ethernet expostas pelo Android. Não usa rede móvel como origem da varredura nem enumera o transporte VPN. As requisições de descoberta são vinculadas à rede local escolhida, mesmo quando o Android usa dados móveis como conexão padrão.

Em cada interface IPv4 elegível, gera os endereços da sub-rede. Em redes maiores que /24, limita a varredura à faixa /24 que contém o próprio aparelho. Por exemplo, um Android 192.168.2.20/24 procura os outros endereços utilizáveis de 192.168.2.1 a 192.168.2.254. Não testa o próprio IP nem os endereços de rede/broadcast dessa faixa.

| Controle | Valor nesta versão |
| --- | --- |
| Porta padrão | TCP 5050 |
| Caminho padrão | /output/saida-html-1 |
| Redes usadas por ciclo | Até duas redes Wi-Fi/Ethernet |
| Limite global de alvos | 512 URLs por ciclo |
| Requisições simultâneas | Até 12 |
| Tempo limite de conexão | 650 ms por tentativa |
| Espera de leitura | Até 800 ms por leitura |
| Limite de leitura do corpo | 64 KiB por resposta |
| Prazo de leitura da tentativa | 2,5 s, verificado durante a leitura |
| Nova busca | 30 s depois de terminar o ciclo anterior, com painel aberto |
| Tolerância para HTML ausente | 90 s; o endereço salvo é preservado |

A duração de um ciclo depende de quantos endereços não respondem e da latência. Um servidor pode aparecer antes do término; não há promessa de descoberta instantânea. A varredura é finita e não percorre todas as portas da rede.

Um candidato é listado quando responde HTTP 200, informa conteúdo text/html ou application/xhtml+xml e o trecho lido contém uma estrutura HTML reconhecível. A descoberta lê o título; não executa scripts nem carrega imagens da página. Falhas, JSON, respostas de autorização e páginas de erro HTTP não entram como novos retornos.

Um redirecionamento de barra final pode ser aceito no mesmo endereço, protocolo, porta, caminho e parâmetros. Redirecionamentos para outro servidor, login ou outro caminho não são seguidos pela busca. O WebView, depois da seleção pelo usuário, tem seu próprio comportamento de navegação HTTP/HTTPS.

Abrir o painel inicia a busca. Fechar o painel, abrir a configuração manual ou de modo, sair da atividade ou trocar de modo cancela o ciclo e a repetição. Respostas de ciclos cancelados não podem modificar a lista atual. A varredura não fica rodando em segundo plano durante a apresentação em tela cheia.

## 8. Endereços personalizados e limites da descoberta

Esta implementação cobre o padrão do exemplo de Stage; não descobre qualquer site HTML existente na LAN. O nome de uma saída é parte da URL, e nem todo servidor publica uma lista dessas saídas sem autenticação.

Se a instalação usar outra porta ou outro nome de saída, informe o link completo uma vez. A busca também pode verificar o endereço salvo, com sua porta e parâmetros, quando o host é um IPv4 numérico pertencente à rede conectada. Esse endereço recebe prioridade em sua interface. Parâmetros do link salvo não são copiados para outros computadores pesquisados.

Um nome como pc.local continua válido para abrir manualmente e para restaurar a página. Esta revisão não faz resolução de nomes para expandir a busca automática do endereço salvo. Também não oferece descoberta HTML automática em IPv6, sub-redes remotas, faixas públicas ou todas as VLANs da instalação.

Em uma rede /16, um servidor fora da faixa /24 do Android pode não aparecer automaticamente. Um endereço numérico salvo em outra parte da mesma sub-rede conectada pode ser verificado diretamente. Para instalações com redes maiores, use um link salvo ou planeje um protocolo de anúncio específico antes de ampliar a varredura.

Outro servidor que entregue HTML no mesmo caminho/porta pode entrar como candidato. A descoberta não autentica a marca do servidor nem comprova que uma rota de aplicativo web existe semanticamente: alguns servidores entregam a mesma página para qualquer caminho. A confirmação final é abrir e conferir o retorno. Não foi presumido um endpoint público para enumerar todas as saídas do Spresenter.

O IP salvo não é atualizado automaticamente quando o DHCP muda o endereço do computador. Reabra o painel para procurar novamente e selecione o endereço novo. Uma reserva DHCP no roteador pode tornar o endereço mais estável.

## 9. Preferências, dados e permissões

As configurações ficam em SharedPreferences, no arquivo privado ndi_monitor_preferences. Não há sincronização dessas preferências entre aparelhos nem conta de usuário do aplicativo.

| Chave | Finalidade |
| --- | --- |
| display_mode | Modo ndi ou html |
| html_url | Último endereço HTML escolhido |
| last_source | Identificação da última fonte NDI |
| start_on_boot | Preferência de abertura após iniciar o Android |

O catálogo descoberto é temporário, em memória. A URL HTML pode conter parâmetros sensíveis e fica salva no espaço privado do app; ela não aparece por inteiro no rótulo da lista. O WebView usa armazenamento web para funcionamento da página. Limpar os dados do app ou desinstalá-lo apaga as preferências e dados correspondentes.

As permissões declaradas são INTERNET, ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE, CHANGE_WIFI_MULTICAST_STATE, RECEIVE_BOOT_COMPLETED e WAKE_LOCK. O código próprio não solicita câmera, microfone, contatos ou localização. O acesso à rede é necessário para descoberta, recepção e exibição de páginas.

Com target API 36, a documentação Android consultada mantém o acesso LAN implícito em INTERNET. Ao migrar para target 37 ou superior, deve-se implementar a permissão de rede local exigida nesse alvo, com tratamento de recusa. Não basta aumentar o número do target no Gradle.

HTTP é permitido para o servidor local. Esse tráfego não é criptografado. No renderizador, arquivos locais e content URLs ficam desabilitados, não existe ponte JavaScript para o código nativo e certificados HTTPS inválidos são recusados. A busca não controla a confiabilidade de páginas escolhidas pelo usuário.

O código próprio não implementa conta, anúncios, analytics ou envio de logs a um servidor do projeto. Isso não substitui a avaliação de privacidade do SDK NDI, do WebView e dos recursos externos da página. A política de privacidade e a ficha Segurança dos dados da Play precisam refletir o pacote efetivamente distribuído.

## 10. Arquitetura e arquivos do código

| Componente | Responsabilidade |
| --- | --- |
| MainActivity.java | Interface, modo, preferências, seleção, ciclo de vida, áudio e controles |
| LanHtmlDiscovery.java | Seleção de Wi-Fi/Ethernet, vínculo de rede e entrega de eventos à interface |
| LanIpv4Subnet.java | Cálculo e limite dos candidatos IPv4, sem consultas DNS |
| HtmlDiscoverySession.java | Até 12 trabalhadores, cancelamento e eventos incrementais |
| HtmlHttpProbe.java | GET limitado, validação HTTP/HTML e leitura do título |
| HtmlSource.java | Endereço, título e rótulo sem parâmetros sensíveis |
| HtmlSourceCatalog.java | Ordem estável, tolerância de ausência e endereço lembrado |
| HtmlSourceUrl.java | Validação e normalização de endereços HTTP/HTTPS |
| HtmlMonitor.java | WebView, mídia, estados, recarga e tentativas de carregamento |
| NativeBridge.java | Interface JNI do receptor e da descoberta NDI |
| ndi_bridge.cpp | Integração NDI, superfície de vídeo e conversão de áudio |
| ndi_source_finder.h | Instância persistente de descoberta NDI |
| SourceCatalog.java | Catálogo NDI com tolerância de 10 s |
| BootReceiver.java | Solicitação de abertura após BOOT_COMPLETED |

O diretório app/src/main/res contém layouts, ícones, dimensões e textos. activity_main.xml define o painel, a lista e os controles. dialog_html_address.xml contém o endereço manual, e dialog_monitor_settings.xml contém as opções. values-sw600dp ajusta dimensões para telas maiores.

As filas estáticas CAPTURE_WORKER e DISCOVERY_WORKER serializam operações NDI. A descoberta HTML usa seu próprio conjunto limitado de trabalhadores. Atualizações de interface voltam ao thread principal; números de geração descartam eventos de operações canceladas.

onStop pausa e destrói o renderizador HTML, libera o áudio Android e cancela a descoberta. A captura NDI é encerrada ao trocar para HTML ou destruir a atividade; esta revisão não promete interromper toda recepção NDI apenas por a atividade perder visibilidade. onDestroy limpa os componentes e agenda a liberação nativa.

As tentativas de carregamento HTML usam intervalo crescente até 30 s para falhas recuperáveis. Elas tratam a abertura da página e erros do renderizador. Uma página que abriu mas parou de receber seus próprios eventos internos precisa implementar sua reconexão; o aplicativo não detecta conteúdo congelado por inspeção visual.

## 11. Preparar e compilar o projeto

| Ferramenta | Versão do projeto |
| --- | --- |
| JDK | 17 |
| Gradle | 8.13 |
| Android Gradle Plugin | 8.13.2 |
| compileSdk / targetSdk | 36 / 36 |
| Android NDK | 28.2.13676358 |
| CMake | 3.22.1 |
| SDK NDI usado na validação | Android 6.3.2.0 |

Obtenha o código do repositório e abra no Android Studio a raiz que contém settings.gradle. Abrir somente app como projeto pode deixar a sincronização sem as tarefas corretas. Configure o Gradle JDK 17 e instale os componentes Android indicados.

O SDK NDI proprietário não acompanha o código público. Obtenha sua cópia pelos canais oficiais, leia os termos e extraia a distribuição Android. Os scripts abaixo copiam headers, bibliotecas ARM e avisos de terceiros para os locais usados pela compilação.

Windows/PowerShell, na raiz do projeto:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\instalar_ndi.ps1 -SdkDir "C:\CAMINHO\NDI SDK for Android"
.\gradlew.bat :app:assembleDebug
```

Linux, macOS ou Git Bash:

```bash
sh tools/instalar_ndi.sh "/caminho/NDI SDK for Android"
sh gradlew :app:assembleDebug
```

Os scripts não baixam o SDK e não aceitam seus termos. Eles removem caches de compilação nativa para detectar a integração na próxima compilação. Headers ficam em app/src/main/cpp/include, libndi.so nas pastas de arquitetura em app/src/main/jniLibs e libndi_licenses.txt em app/src/main/assets.

A saída debug é app/build/outputs/apk/debug/app-debug.apk. Pelo Android Studio, use Generate App Bundles or APKs > Generate APKs, conforme o menu da versão instalada. Não é necessário conectar um aparelho para gerar esse arquivo; o botão Run precisa de um dispositivo e tem outra finalidade.

Sem o SDK, o CMake gera um receptor de preparação que informa a ausência do NDI. O modo HTML pode funcionar nesse cenário, mas não há recepção NDI real. O build release exige os componentes locais e os avisos necessários; essa verificação não substitui a licença nem a validação do conteúdo das bibliotecas.

## 12. Testes e diagnóstico

Execute na raiz, depois de preparar o ambiente:

```bash
sh gradlew :app:testDebugUnitTest :app:lintDebug
sh tools/test-native-discovery.sh
```

No Windows, use gradlew.bat para o primeiro comando. O teste C++ pode rodar em Linux/WSL com um compilador C++17. Ele usa uma implementação simulada da API NDI, sem redistribuir os headers do fabricante.

Os testes Java cobrem normalização de URL, catálogos, intervalos de busca, cancelamento, seleção sem digitar, troca de modo e geometria do painel. Os testes HTTP usam MockWebServer somente no ambiente de teste, com respostas controladas. Esse servidor e o Robolectric não são dependências do APK distribuído.

O Robolectric usa Android simulado, SDK 35 nos testes de interface; compileSdk/targetSdk do app continuam 36. A primeira execução pode baixar dependências de teste. Relatórios ficam em app/build/reports/tests e app/build/reports/lint-results-debug.html. Imagens de conferência de layout ficam em app/build/reports/source-panel-previews.

Para diagnóstico no aparelho, use Android Studio Logcat e registre versão do APK, Android, tipo de rede, endereço da saída e o passo que falhou. Remova tokens de URLs e dados pessoais antes de compartilhar logs. Um teste HTTP local comprova o cliente HTTP, não a descoberta de uma TV Box na rede do culto.

## 13. Assinatura e pacotes de produção

Para preparar os artefatos locais:

```bash
sh gradlew :app:assembleRelease :app:bundleRelease
```

Sem configuração de assinatura, essas saídas release não ficam prontas para distribuição pública. O Android App Bundle fica em app/build/outputs/bundle/release/app-release.aab. AAB é um formato de publicação; não se instala diretamente tocando no arquivo como um APK.

No Android Studio, use Generate Signed App Bundle or APK. Selecione Android App Bundle para preparar a Play. Escolha a chave adequada ou crie uma chave de produção local, anote o alias e guarde backups protegidos. Para o APK público do GitHub, gere e assine um APK release, com a identidade de atualização planejada.

Play App Signing distingue a chave de assinatura do aplicativo da chave de upload. Para permitir atualização entre instalações feitas pela Play e pelo APK do GitHub, planeje certificados compatíveis antes do primeiro lançamento. Uma chave de upload não passa a ser automaticamente a assinatura instalada da Play.

Nunca inclua keystores, senhas ou SDK completo no Git. O .gitignore exclui extensões de chaves, configurações privadas e artefatos gerados. Confirme a lista de arquivos do commit; um arquivo já rastreado não deixa de ser público apenas por entrar no .gitignore.

O código nativo é configurado para alinhamento de 16 KB. Verifique o ELF de todas as bibliotecas ARM64, o empacotamento do APK e o bundle antes de distribuir. Exemplo com as ferramentas do SDK:

```bash
apksigner verify --verbose --print-certs app-debug.apk
zipalign -c -P 16 -v 4 app-debug.apk
```

Esses comandos verificam aspectos do pacote. O teste de recepção, áudio e reconexão em um ambiente Android com páginas de 16 KB continua necessário. Não corrija uma biblioteca proprietária apenas alterando o zip: seu alinhamento ELF também precisa ser compatível.

## 14. Preparação para GitHub e Google Play

No GitHub, mantenha código próprio, documentação, histórico e licença. Distribua um APK release assinado apenas depois da validação e dos termos aplicáveis. O repositório de código não precisa conter o SDK completo nem o projeto com caches do Android Studio. A versão do código e o certificado do APK devem ser identificáveis nas notas do lançamento.

Na consulta oficial de 21/10/2026, novos apps e atualizações móveis da Play precisam de target API 36 desde 31/08/2026, com exceções por formato. O projeto já configura API 36. A página oficial de compatibilidade também exige suporte a páginas de 16 KB em apps com target 35 ou superior nos aparelhos de 64 bits e informa bloqueio de atualizações incompatíveis a partir de 01/02/2027. Reconfirme a política e o diagnóstico do Play Console no envio; esse prazo não é uma dispensa de compatibilidade.

A preparação inclui AAB release assinado, Play App Signing, nome definitivo, descrição, ícones e imagens reais, política de privacidade acessível, Segurança dos dados, classificação de conteúdo, público-alvo e informações de acesso para revisão. Se a função exigir um transmissor na LAN, explique como o revisor pode avaliar o aplicativo e quais funções dependem desse ambiente. Não declare ausência de coleta apenas com base no código próprio: avalie os componentes incluídos.

Para contas pessoais criadas após 13/11/2023, a orientação oficial consultada exige teste fechado com pelo menos 12 participantes inscritos continuamente por 14 dias antes de solicitar acesso à produção. A exigência concreta depende da conta e deve ser conferida no Console.

Novos apps da Play e apps de TV usam AAB conforme a orientação oficial. O suporte ao launcher Leanback e a ausência de touchscreen obrigatório ajudam a instalação em TV, mas não confirmam aprovação: valide navegação completa por D-pad, foco visível, texto legível, estabilidade e os critérios de qualidade Android TV.

Nenhum comando deste manual envia artefatos à Play ou cria uma release pública. A revisão de código, a validação em aparelhos e a aprovação final do mantenedor continuam separadas da compilação local.

## 15. Licenças e atribuição

O código próprio está sob a licença MIT do repositório. Ela permite uso, alteração e distribuição desse código com as condições do texto LICENSE. Não relicencia o SDK, as bibliotecas ou a marca NDI, nem componentes de terceiros.

O escopo deste projeto é um aplicativo de recepção Standard full-bandwidth, distribuído como software em aparelhos de uso geral dos usuários. Não inclui HX/Advanced, venda de hardware dedicado com o aplicativo ou bloqueio gerenciado de equipamentos. Mudanças desse escopo exigem nova conferência dos termos pertinentes com a NDI.

A distribuição de libndi.so dentro de APK/AAB deve seguir o contrato que acompanha o SDK e suas condições de redistribuição. O aplicativo mantém a atribuição NDI e o link ndi.video, além da tela Licenças de terceiros com o arquivo de avisos do SDK. O nome de produto provisório evita NDI no título; o identificador técnico antigo do pacote é preservado para continuidade das atualizações.

Leia LICENCA_NDI_IMPORTANTE.md e o contrato da cópia do SDK. O SDK usado na validação é o Android 6.3.2.0. A documentação pública de redistribuição não torna o SDK inteiro open source. Não publique instaladores ou chaves com o código por conveniência.

Spresenter é software separado. O aplicativo abre a saída fornecida pela instalação do usuário; não redistribui o software Spresenter e não representa patrocínio ou certificação por seus desenvolvedores. NDI é marca registrada da Vizrt NDI AB.

## 16. Solução de dificuldades

| Situação | Verificação e próximo passo |
| --- | --- |
| Nenhum HTML encontrado | Confirme a saída ligada no PC, porta/caminho padrão, IPv4, rede e firewall local |
| PC e Android no mesmo Wi-Fi, sem acesso | Verifique rede de convidados e isolamento entre clientes no roteador |
| Retorno usa outro nome ou porta | Use Informar endereço com o link completo; ele ficará salvo no aparelho |
| Rede maior que /24 | Teste o IP salvo ou coloque os aparelhos na mesma faixa; a busca não cobre toda a rede grande |
| HTML listado abre conteúdo errado | Confira o destino: um servidor pode responder HTML em uma rota genérica |
| Página abre, mas não atualiza | Teste a própria página em navegador; sua conexão interna precisa estar funcionando |
| Página sem áudio | Confirme se a saída HTML fornece áudio, além de volume, WebView e codec do aparelho |
| Descoberta encontra, WebView não abre | Teste o link no navegador; confira rotas de rede, dados móveis, WebView e servidor |
| Mensagem de WebView indisponível | Verifique o componente web e a atualização suportada pelo fabricante do aparelho |
| HTTPS com certificado inválido | Corrija o certificado ou use o endereço local válido fornecido pela instalação |
| Nenhuma fonte NDI | Confirme fonte Standard ativa, conectividade, descoberta multicast e SDK integrado |
| SDK NDI ausente | Integre headers, bibliotecas das duas arquiteturas e avisos; compile novamente |
| App não abre após ligar | Teste a opção de Home e as restrições de boot/segundo plano do firmware |
| APK não atualiza o anterior | Compare pacote, versionCode e assinatura; gere com a chave compatível |
| Android Studio mostra No Devices | Gere o APK pelo menu de build; Run exige aparelho/emulador |
| Gradle não encontra tarefas do projeto | Abra a pasta raiz com settings.gradle e sincronize novamente |

## 17. Validação no equipamento final

Antes de usar em um culto, registre o modelo do aparelho, versão do Android/WebView, tipo de rede e versão do transmissor. Execute os cenários abaixo e anote os resultados.

1. Buscar o retorno HTML padrão, selecionar com setas/OK e abrir sem digitar.
2. Abrir dois computadores transmissores e conferir identificação e seleção de cada retorno.
3. Repetir a busca sem perder as fontes já exibidas nem deslocar a seleção.
4. Fechar o painel e confirmar que uma busca antiga não reabre ou altera a exibição.
5. Reiniciar o aplicativo e confirmar modo, fonte e endereço salvos por aparelho.
6. Desligar e religar o transmissor; conferir descoberta e carregamento após a volta.
7. Testar a página real com letras, avisos, relógios, vídeos e áudio que ela forneça.
8. Alternar NDI/HTML e confirmar que a mídia do modo anterior para.
9. Testar fonte NDI Standard com imagem, áudio, proporção e interrupção do sinal.
10. Conferir toque, mouse, controle remoto, foco e saída para as configurações do Android.
11. Testar Home/boot e restaurar o launcher original.
12. Conferir atualização com a chave correta e registrar se as preferências foram mantidas.
13. Validar ARM64 em ambiente de páginas de 16 KB, além dos checks de alinhamento.
14. Fazer um ensaio prolongado no tempo e na rede previstos para o uso real.

A validação automatizada desta revisão está em VALIDACAO_1.4.md. Ela não substitui esses cenários. Não há teste real concluído com todas as TV Boxes, todas as saídas Spresenter nem todos os formatos de fonte NDI.

## 18. Manutenção e referências

Ao alterar descoberta, preserve seus limites, cancelamento e a ausência de requisições de controle. Para suportar novos servidores, prefira um contrato de anúncio ou uma rota identificável. Acrescentar portas e caminhos indiscriminadamente aumenta o tráfego e os falsos candidatos.

Ao alterar o layout, confira celular em paisagem com altura pequena, fonte ampliada e TV com controle remoto. Ao atualizar SDK NDI, NDK ou AGP, repita os testes nativos e as verificações de 16 KB. Ao aumentar targetSdk, reveja as mudanças de comportamento e a permissão LAN antes da publicação.

Histórico resumido: a revisão 1.2 melhorou descoberta NDI e painel; a 1.3 acrescentou HTML por endereço salvo; a 1.4 acrescenta busca HTML limitada, seleção pela lista e este manual. Consulte CHANGELOG.md para o histórico do projeto.

Referências oficiais consultadas:

- Spresenter, saídas web: https://spresenter.com/pt
- API Spresenter, endpoints e autenticação: https://github.com/sonext-software/spresenter-api
- Android, estado de rede: https://developer.android.com/develop/connectivity/network-ops/reading-network-state
- Android, permissão LAN: https://developer.android.com/privacy-and-security/local-network-permission
- Android, WebView: https://developer.android.com/develop/ui/views/layout/webapps/webview
- Android, assinatura: https://developer.android.com/studio/publish/app-signing
- Android, App Bundles: https://developer.android.com/guide/app-bundle
- Play, target API: https://support.google.com/googleplay/android-developer/answer/11926878
- Android, páginas de 16 KB: https://developer.android.com/guide/practices/page-sizes
- Play, teste de contas pessoais: https://support.google.com/googleplay/android-developer/answer/14151465
- NDI, licenciamento: https://docs.ndi.video/all/developing-with-ndi/sdk/licensing
- NDI, distribuição: https://docs.ndi.video/all/developing-with-ndi/sdk/software-distribution

As fontes externas podem mudar depois desta revisão. O código e os valores configurados nesta versão são a referência para seu comportamento implementado; os termos de terceiros e as regras vigentes da loja devem ser conferidos no lançamento.
