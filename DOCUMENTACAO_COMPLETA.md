# Monitor de Retorno

## Manual completo do projeto - versão 1.4.7

Aplicativo Android para receber NDI® Standard full-bandwidth ou abrir um retorno HTML pela rede local. Nome de produto provisório: Monitor de Retorno. Mantenedor: Gedielson Saramento. Documentação técnica revisada em 26/10/2026, horário do Brasil.

NDI® is a registered trademark of Vizrt NDI AB. https://ndi.video/

Esta revisão é uma versão de teste. O resultado de compilação, os testes executados e o que ainda precisa de equipamento real estão em VALIDACAO_1.4.7.md. A existência de um APK ou AAB compilado não significa publicação ou aprovação na Google Play.

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
| Sistema mínimo | HTML: Android 6.0 / API 23; NDI: Android 7.0 / API 24 |
| Processadores do APK | ARM32 armeabi-v7a e ARM64 arm64-v8a |
| Rede | Wi-Fi ou Ethernet com acesso ao computador transmissor |
| Busca HTML automática | Rede local com IPv4 privado; padrão TCP 5050 |
| Exibição HTML | Android System WebView funcional e compatível com a página |
| NDI real | SDK NDI para Android integrado; fonte Standard compatível |
| Orientação | Horizontal, com proporção preservada no vídeo NDI |
| Operação | Toque, mouse ou controle remoto com setas e OK/Enter |
| Pacote Android | br.com.polegar.ndimonitor |
| Versão | versionName 1.4.7, versionCode 12; teste de box: 1.4.7-tvbox |

O Android 5.1 do servidor Samsung não é compatível com este APK. Não há pacote x86/x86_64 nesta revisão. O funcionamento de vídeo, áudio, boot e WebView varia conforme o hardware e o firmware: a versão mínima não comprova compatibilidade com todas as TV Boxes.

Uma rede local funcionando é suficiente para o uso habitual de fontes locais. O conteúdo de uma página HTML pode exigir internet se carregar fontes, imagens, scripts ou outros recursos externos.

### Compatibilidade HTML na revisão 1.4.7

A foto da BTV B9 identifica Android 6.0.1/API 23, ARM32 e app 1.4.6-TVBox com o menu/diagnóstico aberto. O WebView informa Chrome 44.0.2403.119, abaixo do limite do perfil 1.4.6, e erro `Unexpected token )` em `main.ff832fdd.js:2`. O registro de abertura interrompida não contém exceção Java e, sozinho, não comprova falha nativa. A string desktop Linux x86_64 do user agent não altera a arquitetura ARM informada pelo Android.

A 1.4.7 transforma a mesma captura para ES5, validado por parser, e permite o perfil em Chrome 44–86. Babel usa alvo IE 11 para forçar a conversão de sintaxe, sem declarar suporte ao navegador IE. Antes da conversão, os bindings let/const recebem nomes próprios por escopo, evitando que var de um laço oculte o carregador externo do programa minificado. Polyfills ECMAScript são carregados antes dos módulos de APIs web; fetch, AbortController, URLSearchParams e ResizeObserver recebem fallbacks quando ausentes. O estilo é preparado para Chrome 44 e mantém a conversão de inset em quatro bordas.

As verificações de origem/hash e os limites de rede descritos abaixo permanecem. O APK contém o perfil ES5 para os dois arquivos conhecidos; os arquivos gerados e a captura ficam fora do Git. O diagnóstico manual passa a aguardar até cinco segundos pela resposta da página. Nenhuma consulta periódica é adicionada. Instalação, enquadramento e atualizações de letras devem ser testados na B9 física conforme [TVBOX.md](TVBOX.md); JSDOM não comprova pixels, mídia ou firmware real.

### Histórico: compatibilidade HTML na revisão 1.4.6

A HTV já abriu a interface 1.4.5. O diagnóstico HTML seguinte mostrou Chrome 52 e `Unexpected token function`. A captura recebida contém `main.ff832fdd.js` e `main.eb127441.css`. O programa usa 34 funções assíncronas, 88 spreads e 36 blocos catch sem parâmetro, além de APIs posteriores e do atalho CSS `inset`. Grid/clamp não aparecem nesses dois arquivos; sua ausência no diagnóstico não prova sozinha a causa.

`tools/html-compat/prepare.cjs` adapta essa captura durante a compilação: Babel para Chrome 52, polyfills e conversão de 24 objetos `inset: 0` e uma atribuição `style.inset` em quatro bordas explícitas. O CSS recebe fallbacks. Não há compilador JavaScript executando na box.

`HtmlLegacyCompat` atua somente em Chrome 52–86 com um perfil local preparado. Identifica os arquivos pelo nome e verifica o SHA-256 dos bytes que o servidor entrega e do asset adaptado local. Só aceita GET de subrecursos da mesma origem, sem redirecionamento, com limite de 2 MiB, tempos de conexão/leitura e até quatro verificações por renderizador. A cache em memória vale para a URL completa verificada. Fechar a página não espera a verificação terminar na thread da interface.

Se arquivo, host ou pacote local não corresponder, o carregamento original continua. O adaptador não substitui o WebView nem oferece compatibilidade universal com qualquer HTML. Servidor, WebSocket e mídia continuam na origem selecionada. Não captura o áudio do computador. O diagnóstico informa quantos arquivos receberam a adaptação e inclui arquivo/linha de novos erros de JavaScript.

O adaptador e as ferramentas estão no Git; captura e programa de terceiros gerado ficam fora dele. Veja o [preparo reproduzível](tools/html-compat/README.md). O APK de teste inclui a adaptação da captura recebida e conserva pacote, certificado e preferências TVBox. A validação simulada não confirma pixels, mídia ou Chrome 52 físico: repita o teste de [TVBOX.md](TVBOX.md).

### Correção de fonte na revisão 1.4.5

O relatório da HTV-6H confirma Android 7.1.2/API 25, ARM64 e ARM32 e app 1.4.4-TVBox. O fechamento ocorre em `setContentView`, no botão seletor NDI/HTML: `ArrayIndexOutOfBoundsException: length=49; index=74`, seguido por `StringBlock.get`, `XmlBlock.Parser.getPooledString`, `TypedArray.getString` e `TextView.java:1100`.

No código AOSP do Android 7.1.2, essa linha lê `fontFamily`. A análise do APK 1.4.4 entregue encontrou 49 strings no pool do layout e o literal `sans` no índice 74 do pool global, correspondendo ao relatório. O tema passa a usar `android:typeface` com enumeração sem serifa, lida como inteiro, removendo a definição textual global. Não troca a classe dos botões nem altera o código de reprodução.

A validação simulada foi ampliada para APIs 28 e 34, correspondentes às telas de diagnóstico da BTV E10 e da TV Philco. As fotos posteriores confirmaram a abertura do menu/diagnóstico da 1.4.5 na HTV. A atualização mantém o relatório anterior: um texto que identifica app 1.4.4 pertence à falha histórica. Confira a versão atual no início de **Diagnóstico do aparelho** e os novos erros HTML no final.

### Compatibilidade de TV Box na revisão 1.4.4

Na primeira configuração em API 23–25, sem modo ou fonte NDI salvos, a tela abre em HTML. O NDI só é inicializado ao entrar no modo NDI. Os callbacks de superfície, definição de destino e limpeza deixam de carregar a biblioteca por conta própria. Uma seleção NDI já confirmada continua sendo restaurada sem buscar outras fontes. Abrir HTML em qualquer Android também deixa de inicializar NDI desnecessariamente.

`StartupActivity` é a entrada do launcher, da tela inicial opcional e do boot. Não depende do receptor ou do WebView. Normalmente encaminha diretamente ao monitor; após uma abertura interrompida, mostra o relatório e oferece **Abrir apenas HTML**, nova tentativa e configurações do Android. A opção HTML de recuperação não apaga a última fonte NDI nem muda o modo persistido até uma nova fonte ser efetivamente escolhida.

`StartupJournal` grava poucos checkpoints locais de início/fim da abertura, inicialização do NDI e primeira descoberta NDI. Não grava a cada quadro ou a cada pesquisa. `MonitorApplication` guarda a exceção Java, quando houver, e mantém o tratamento de falha do Android. Um sinal nativo pode deixar somente o checkpoint: ele não é apresentado como diagnóstico confirmado. Queda de energia ou encerramento durante a abertura também podem gerar recuperação. Não há envio automático de relatórios.

No Android 6, somente HTML é habilitado na inicialização: todas as chamadas à ponte NDI passam por `NdiReceiver`, que verifica API 24 antes de inicializar `NativeBridge`. Falhas de ligação de biblioteca (`LinkageError`) deixam HTML e o menu acessíveis. Isso não intercepta falhas nativas de execução, como SIGSEGV; essas ainda exigem log do aparelho. Não há promessa de NDI no Android 6.

O modo **16:9 — compatibilidade TV Box** preserva a densidade de tela fornecida ao WebView pelo próprio firmware. Mede o canvas de 1920 × 1080 CSS nessa mesma densidade e o encaixa proporcionalmente na tela; não força software nem altera o CSS do tema. É escolhido por padrão em API 23–25 quando ainda não existe uma preferência explícita. As alternativas anteriores continuam disponíveis na engrenagem.

**Diagnóstico do aparelho** mostra versão/código e pacote do app, fabricante/modelo, Android Release e API, ABIs, densidade, disponibilidade NDI, user agent do WebView, área nativa/CSS e suporte a Grid, variáveis CSS e `clamp()`. A leitura ocorre somente ao abrir essa tela. Não envia dados, faz busca de fontes nem altera o HTML. Na 1.4.7, uma resposta ausente ao JavaScript recebe um limite de cinco segundos (antes: 1,5 s). Consulte [TVBOX.md](TVBOX.md).

O APK `tvboxTest` usa `.tvboxtest` e nome próprio para instalação paralela. As fontes e preferências precisam ser configuradas uma vez nessa edição e ficam salvas nela. A assinatura é de desenvolvimento; não serve para a Play Store. Para atualizações da edição habitual, continuam valendo as regras de certificado descritas a seguir.

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

Na primeira configuração, abra o painel de fontes. Acione o seletor do cabeçalho e escolha HTML - retornos na rede. Sem endereço salvo, a busca começa automaticamente. Os endereços encontrados entram na lista à medida que respondem; não é necessário esperar o ciclo terminar. Com endereço já salvo, o aplicativo reabre essa página diretamente.

Use as setas e OK do controle remoto para selecionar o retorno, ou toque na linha. O aplicativo salva o endereço nesse aparelho, fecha o painel e abre a página. Na próxima abertura, ele restaura o modo HTML e esse endereço. Não escolhe sozinho um servidor novo descoberto na rede.

O modo e o endereço são persistidos juntos ao confirmar uma fonte. Navegar por outro modo que ainda não tem origem configurada preserva a última seleção para o próximo início. Depois de selecionar, a varredura e os temporizadores param. Reabrir apenas o painel mostra a lista conhecida sem iniciar novas buscas; use atualizar para procurar outra fonte.

Cada linha mostra o título recebido da página e o endereço do servidor, sem os parâmetros da URL. O título é apenas uma identificação fornecida pelo servidor; confira o conteúdo ao abrir. Encontrar HTML não comprova que a página contém o retorno correto ou que seu conteúdo continua sendo atualizado.

O ícone de atualização no cabeçalho refaz a busca sem apagar imediatamente as fontes conhecidas. O botão de recarga ao lado de Informar endereço recarrega a página já escolhida. As duas ações têm funções diferentes.

Para usar outro endereço, acione Informar endereço. Cole o link completo ou digite apenas o IP. Um IP sem protocolo é completado com a porta 5050 e o caminho /output/saida-html-1. Um link completo HTTP ou HTTPS preserva a porta, o caminho, os parâmetros e o fragmento.

### Ajuste da imagem HTML

O padrão Monitor 16:9 usa uma área de referência de 1920 × 1080 e a ajusta inteira ao espaço disponível, preservando a proporção. Telas com outra proporção recebem faixas pretas. A renderização usa escala de texto de 100% e identificação de navegador de computador. Uma configuração de densidade 1x é aplicada somente ao contexto do WebView, evitando multiplicar a área virtual pela densidade do celular; os controles do aplicativo preservam a escala do sistema.

Na engrenagem do modo HTML, Exibição HTML permite escolher Monitor 16:9 - ajustar à tela ou Layout original da página. A escolha é salva e restaura-se ao reiniciar. A troca recria o renderizador com a mesma URL e encerra a mídia anterior. O layout original usa a área disponível e o viewport definido pela página. O modo monitor define o enquadramento do aplicativo; ele não reescreve o CSS do tema nem garante corrigir um corte interno do conteúdo do servidor.

## 5. Primeiro uso do NDI

Ative a fonte NDI Standard no computador e mantenha o Android em uma rede que permita sua descoberta e conexão. Abra o painel, selecione NDI e aguarde as fontes aparecerem. Selecione a origem desejada para iniciar a recepção e salvar sua identificação nesse aparelho.

A descoberta começa na configuração sem fonte ou ao solicitar atualizar. Enquanto essa sessão de busca está ativa e a lista está aberta, encontrar a primeira fonte não encerra a descoberta. A seleção ou o fechamento da lista interrompe as consultas e libera a instância de descoberta NDI. Atualizar inicia uma nova sessão e preserva as linhas; uma origem que permanece ausente é retirada após a tolerância de 10 segundos. A ordem das fontes já apresentadas é mantida enquanto continuam no catálogo.

Com fonte salva, a inicialização conecta diretamente a essa identificação e mantém o painel fechado. Abrir apenas o painel não reinicia o descobridor. A reconexão continua direcionada à fonte salva; o SDK troca as mensagens necessárias para resolver e receber essa origem. O aplicativo não substitui a escolha por outra fonte que aparecer na rede.

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
| Atualizar no cabeçalho | Solicita uma sessão de busca no modo atual |
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
| Caminhos conhecidos | /output/saida-html-1, /output/retorno e /output/retono |
| Redes usadas por ciclo | Até duas redes Wi-Fi/Ethernet |
| Limite global de alvos | 512 alvos por ciclo; nomes alternativos usam o mesmo trabalhador |
| Requisições simultâneas | Até 12 |
| Tempo limite de conexão | 650 ms por tentativa |
| Espera de leitura | Até 800 ms por leitura |
| Limite de leitura do corpo | 64 KiB por resposta |
| Orçamento do alvo | 2,5 s compartilhados entre nomes e redirecionamentos; leituras e conexões são limitadas |
| Nova busca | 30 s após o ciclo anterior, somente durante uma sessão de busca com painel aberto |
| Tolerância para HTML ausente | 90 s; o endereço salvo é preservado |

A duração de um ciclo depende de quantos endereços não respondem e da latência. Um servidor pode aparecer antes do término; não há promessa de descoberta instantânea. A varredura é finita e não percorre todas as portas da rede.

Um candidato é listado quando responde HTTP 200, informa conteúdo text/html ou application/xhtml+xml e o trecho lido contém uma estrutura HTML reconhecível. A descoberta lê o título; não executa scripts nem carrega imagens da página. Falhas, JSON, respostas de autorização e páginas de erro HTTP não entram como novos retornos.

A busca aceita até dois redirecionamentos por nome de saída: normalização de barra final no mesmo endereço, ou mudança de `/output/NOME` sem parâmetros para `/slide/?output=stage...` no mesmo protocolo, host e porta. O seletor output deve ser único e igual a stage. Não segue login, APIs de controle, outro servidor ou mudança de parâmetros de um endereço personalizado para outra rota. A escolha na lista conserva o endereço curto de entrada, sem salvar o outputId temporário do destino. O WebView, depois da seleção pelo usuário, tem seu próprio comportamento de navegação HTTP/HTTPS.

Em cada alvo, o primeiro HTML válido é listado. Somente respostas HTTP 404/410 permitem tentar o próximo nome conhecido; falhas de conexão, autorização ou tipos de conteúdo diferentes encerram esse alvo. Assim, um endereço sem servidor não recebe tentativas de conexão para cada nome. A busca não enumera todas as saídas de um mesmo servidor.

A configuração sem uma fonte inicia a busca. Com fonte salva, abrir apenas o painel mantém a descoberta parada; o botão atualizar inicia uma sessão para procurar outras origens. Selecionar uma fonte ou fechar o painel encerra a sessão. Abrir a configuração manual ou de modo, sair da atividade ou trocar de modo cancela o ciclo em andamento. Respostas canceladas não podem modificar a lista atual. A varredura não fica rodando durante a apresentação em tela cheia.

## 8. Endereços personalizados e limites da descoberta

Esta implementação cobre os nomes conhecidos saida-html-1, retorno e retono na porta 5050; não descobre qualquer site HTML existente na LAN. Um link salvo sem parâmetros, na porta 5050 e com caminho simples /output/NOME pode fornecer o primeiro nome tentado em outros endereços, permitindo reencontrá-lo após mudança de IP. Caminhos fora de /output/NOME, portas diferentes e parâmetros ficam restritos ao endereço completo salvo. O nome de uma saída é parte da URL, e nem todo servidor publica uma lista dessas saídas sem autenticação.

Se a instalação usar outra porta ou outro nome de saída, informe o link completo uma vez. A busca também pode verificar o endereço salvo, com sua porta e parâmetros, quando o host é um IPv4 numérico pertencente à rede conectada. Esse endereço recebe prioridade em sua interface. Parâmetros do link salvo não são copiados para outros computadores pesquisados.

Um nome como pc.local continua válido para abrir manualmente e para restaurar a página. Esta revisão não faz resolução de nomes para expandir a busca automática do endereço salvo. Também não oferece descoberta HTML automática em IPv6, sub-redes remotas, faixas públicas ou todas as VLANs da instalação.

Em uma rede /16, um servidor fora da faixa /24 do Android pode não aparecer automaticamente. Um endereço numérico salvo em outra parte da mesma sub-rede conectada pode ser verificado diretamente. Para instalações com redes maiores, use um link salvo ou planeje um protocolo de anúncio específico antes de ampliar a varredura.

Outro servidor que entregue HTML no mesmo caminho/porta pode entrar como candidato. A descoberta não autentica a marca do servidor nem comprova que uma rota de aplicativo web existe semanticamente: alguns servidores entregam a mesma página para qualquer caminho. A confirmação final é abrir e conferir o retorno. Não foi presumido um endpoint público para enumerar todas as saídas do Spresenter.

O IP salvo não é atualizado automaticamente quando o DHCP muda o endereço do computador. Reabra o painel para procurar novamente e selecione o endereço novo. Uma reserva DHCP no roteador pode tornar o endereço mais estável.

## 9. Preferências, dados e permissões

As configurações ficam em SharedPreferences, no arquivo privado ndi_monitor_preferences. Não há sincronização dessas preferências entre aparelhos nem conta de usuário do aplicativo.

| Chave | Finalidade |
| --- | --- |
| display_mode | Modo ndi ou html da última fonte confirmada |
| html_url | Último endereço HTML escolhido |
| html_stage_layout | Exibição HTML como monitor 16:9 ou página original |
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
| StartupActivity.java / StartupJournal.java / MonitorApplication.java | Entrada independente, checkpoints e relatório local após abertura interrompida |
| ControlInsets.java | Isolamento de tipos das APIs 28/30 fora da Activity carregada em Android antigo |
| LanHtmlDiscovery.java | Seleção de Wi-Fi/Ethernet, vínculo de rede e entrega de eventos à interface |
| LanIpv4Subnet.java | Cálculo e limite dos candidatos IPv4, sem consultas DNS |
| HtmlDiscoverySession.java | Até 12 trabalhadores, cancelamento e eventos incrementais |
| HtmlHttpProbe.java | GET limitado, validação HTTP/HTML e leitura do título |
| HtmlSource.java | Endereço, título e rótulo sem parâmetros sensíveis |
| HtmlSourceCatalog.java | Ordem estável, tolerância de ausência e endereço lembrado |
| HtmlSourceUrl.java | Validação e normalização de endereços HTTP/HTTPS |
| HtmlMonitor.java | WebView, mídia, estados, recarga e tentativas de carregamento |
| HtmlStageViewport.java | Área de monitor e ajuste proporcional da cena inteira |
| NdiReceiver.java / NativeBridge.java | Limite de API, proteção de carregamento e interface JNI do receptor |
| BackNavigation.java | Tipos de navegação API 33 isolados dos Androids antigos |
| DeviceDiagnostics.java / HtmlPageDiagnostics.java | Diagnóstico local de sistema e renderizador, sem telemetria |
| ndi_bridge.cpp | Integração NDI, superfície de vídeo e conversão de áudio |
| ndi_source_finder.h | Instância persistente de descoberta NDI |
| SourceCatalog.java | Catálogo NDI com tolerância de 10 s |
| BootReceiver.java | Solicitação de abertura após BOOT_COMPLETED |

O diretório app/src/main/res contém layouts, ícones, dimensões e textos. activity_main.xml define o painel, a lista e os controles. dialog_html_address.xml contém o endereço manual, e dialog_monitor_settings.xml contém as opções. values-sw600dp ajusta dimensões para telas maiores.

As filas estáticas CAPTURE_WORKER e DISCOVERY_WORKER serializam operações NDI. A descoberta HTML usa seu próprio conjunto limitado de trabalhadores. Atualizações de interface voltam ao thread principal; números de geração descartam eventos de operações canceladas.

onStop pausa e destrói o renderizador HTML, libera o áudio Android, cancela a descoberta e agenda o encerramento da captura NDI. Ao voltar a onStart, a recepção usa a mesma fonte salva, sem uma busca geral. Conexões enfileiradas canceladas são descartadas por geração. onDestroy limpa os componentes; a liberação do finder acompanha o encerramento da busca, inclusive ao selecionar ou fechar o painel.

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

O Robolectric usa Android simulado, APIs 23/25/28/34/35 conforme o teste; compileSdk/targetSdk do app continuam 36. A primeira execução pode baixar dependências de teste. Relatórios ficam em app/build/reports/tests e app/build/reports/lint-results-debug.html. Imagens de conferência de layout ficam em app/build/reports/source-panel-previews.

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

Na consulta oficial de 22/10/2026, novos apps e atualizações móveis da Play precisam de target API 36 desde 31/08/2026, com exceções por formato. O projeto já configura API 36. A página oficial de compatibilidade também exige suporte a páginas de 16 KB em apps com target 35 ou superior nos aparelhos de 64 bits e informa bloqueio de atualizações incompatíveis a partir de 01/02/2027. Reconfirme a política e o diagnóstico do Play Console no envio; esse prazo não é uma dispensa de compatibilidade.

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
| Nenhum HTML encontrado | Confirme a saída ligada, porta 5050, nome de saída elegível, IPv4, rede e firewall local |
| HTML com letras maiores ou cortes no celular | Use engrenagem, Exibição HTML, Monitor 16:9; confira também o CSS do tema no servidor |
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

1. Buscar os retornos conhecidos, inclusive /output/retono, selecionar com setas/OK e abrir sem digitar; conferir redirecionamento de Stage.
2. Conferir visitantes, Harpa, oportunidade e imagem na exibição Monitor 16:9; comparar a opção de página original e restaurar a escolha após reiniciar.
3. Abrir dois computadores transmissores e conferir identificação e seleção de cada retorno.
4. Repetir a busca solicitada sem perder as fontes já exibidas nem deslocar a seleção.
5. Fechar o painel e confirmar que uma busca antiga não reabre ou altera a exibição.
6. Reiniciar o aplicativo e confirmar modo, fonte e endereço salvos por aparelho.
7. Desligar e religar o transmissor; conferir descoberta e carregamento após a volta.
8. Testar a página real com letras, avisos, relógios, vídeos e áudio que ela forneça.
9. Alternar NDI/HTML e confirmar que a mídia do modo anterior para.
10. Testar fonte NDI Standard com imagem, áudio, proporção e interrupção do sinal.
11. Conferir toque, mouse, controle remoto, foco e saída para as configurações do Android.
12. Testar Home/boot e restaurar o launcher original.
13. Conferir atualização com a chave correta e registrar se as preferências foram mantidas.
14. Validar ARM64 em ambiente de páginas de 16 KB, além dos checks de alinhamento.
15. Fazer um ensaio prolongado no tempo e na rede previstos para o uso real.
Confira também que abrir apenas o menu não inicia buscas depois de selecionar. Volte do segundo plano e confirme a mesma origem. Se a fonte estiver fora do ar, confirme que as tentativas continuam direcionadas a ela, sem selecionar outra automaticamente.

A validação automatizada desta revisão está em VALIDACAO_1.4.4.md. Ela não substitui esses cenários. Não há teste real concluído com todas as TV Boxes, todas as saídas Spresenter nem todos os formatos de fonte NDI.

## 18. Manutenção e referências

Ao alterar descoberta, preserve seus limites, cancelamento e a ausência de requisições de controle. Para suportar novos servidores, prefira um contrato de anúncio ou uma rota identificável. Acrescentar portas e caminhos indiscriminadamente aumenta o tráfego e os falsos candidatos.

Ao alterar o layout, confira celular em paisagem com altura pequena, fonte ampliada e TV com controle remoto. Ao atualizar SDK NDI, NDK ou AGP, repita os testes nativos e as verificações de 16 KB. Ao aumentar targetSdk, reveja as mudanças de comportamento e a permissão LAN antes da publicação.

Histórico resumido: a revisão 1.2 melhorou descoberta NDI e painel; a 1.3 acrescentou HTML por endereço salvo; a 1.4 acrescentou busca HTML limitada, seleção pela lista e este manual. A 1.4.1 restaura a última configuração confirmada e encerra a descoberta depois de selecionar, inclusive liberando o finder NDI. A 1.4.2 acrescenta nomes alternativos de saída, redirecionamento para Stage e enquadramento HTML de monitor 16:9. A 1.4.3 acrescenta HTML em API 23, proteção de carregamento nativo, modo de densidade do firmware e diagnóstico sob demanda. Consulte CHANGELOG.md para o histórico do projeto.

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
