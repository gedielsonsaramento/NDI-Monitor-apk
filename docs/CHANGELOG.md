# 1.5.2 + Central 0.1.2 — Interface e busca HTML (31/10/2026)

- Integra o fonte completo da 1.5.1, incluindo a Central e o tratamento de desligamento diário.
- Retira diagnósticos, relatórios e detalhes técnicos da interface da box; os registros internos continuam disponíveis para o painel.
- Mantém até oito PCs no catálogo HTML, com migração da configuração anterior e chave protegida por origem.
- Busca os catálogos dos PCs cadastrados, inclusive em outras faixas quando a rede permite o acesso.
- Acrescenta busca opcional em outra faixa IPv4 local, sem enviar credenciais na varredura pública.
- Preserva a fonte em reprodução ao cadastrar, atualizar e remover PCs e conserva os catálogos fixados pela Central.
- Corrige o contador de fontes para considerar toda a lista HTML.
- Ao mudar a rede ou abrir a lista HTML, procura novos endereços sem remover os PCs anteriores, sem trocar a fonte e sem enviar tokens na busca pública.
- Corrige a ponte da Central conforme o envelope do SDK, com feedback de tentativa, progresso e erro dentro da janela de pareamento.
- Inclui novamente os arquivos do perfil de compatibilidade HTML, ausentes no APK 1.5.1; o empacotamento verifica presença e hashes.
- Corrige o nome visível para **Spresenter** no app, no plugin e na configuração pelo navegador.
- Compila a atualização com o mesmo pacote e certificado da 1.5.1 e preserva suas bibliotecas nativas.
- [Validação desta revisão](historico/VALIDACAO_1.5.2.md).

# 1.5.1 + Central 0.1.1 — Desligamento diário da igreja (30/10/2026)

- Ausência de comunicação com uma box desligada permanece neutra: não vira relatório nem falha ativa.
- Marcadores de sessão incompleta não produzem falhas automáticas; a atualização também descarta interrupções não confirmadas pendentes ou antigas.
- Ausência de fonte HTML/NDI é um estado de espera; falhas confirmadas e relatos manuais continuam disponíveis.
- Gráficos reiniciam ao detectar uma nova execução ou reconexão após intervalo. Identidade, vínculo e fonte salvos são preservados.
- Corrigida a normalização dos nomes para conservar letras e acentos.
- [Política para a rotina dos cultos](../central-retornos/README.md#desligamento-após-os-cultos).

# 1.5.0 — Central de Retornos local (30/10/2026)

- App com pareamento explícito e API local autenticada, disponível somente no primeiro plano.
- Plugin Central de Retornos 0.1.0 para fonte, estado, gráficos, relatos e manutenção confirmada.
- Catálogo HTML configurado no PC e distribuído sem a chave de API às boxes; última configuração conservada no app.
- Captura, descarte e fila NDI instrumentados no JNI; `libndi.so` preservado.
- Relatos e falhas locais limitados e separados do suporte remoto, que continua desativado.
- [Instalação e limites](../central-retornos/README.md) · [Validação](historico/VALIDACAO_1.5.0.md).

# 1.4.12 — Relatar problemas e preparar suporte com métricas (30/10/2026)

- Acrescenta **Relatar problema** nas configurações e na tela de recuperação, com categoria, medidas de memória/CPU/rede do próprio app e cópia manual do relatório.
- Prepara envio manual e compartilhamento automático opcional por HTTPS para o servidor próprio futuro. O APK disponibilizado tem destino vazio: não há envio remoto nem amostragem automática enquanto o servidor não estiver configurado.
- Usa identificador aleatório de instalação, versão e falhas com campos limitados. Exclui fontes, tokens, conteúdo, identificadores de hardware e mensagens completas das exceções.
- Limita a frequência a um batimento por minuto em primeiro plano, com fila de falhas limitada, espera crescente em falhas de comunicação e interrupção do envio ao desativar.
- Android 11+ pode identificar falhas nativas/ANRs pelo motivo de saída do próprio processo. Android antigo registra interrupções como não confirmadas; não confunde falta de comunicação com crash.
- Mantém pacote, certificado de atualização, bibliotecas NDI, perfil HTML e permissões anteriores. Servidor, painel e testes físicos desta versão ainda pendentes.
- [Funcionamento e contrato](SUPORTE_METRICAS.md) · [Validação](historico/VALIDACAO_1.4.12.md).

# Documentação — página simplificada para instalar e usar (30/10/2026)

- Destaca o download do APK pronto, com instalação e atualização em passos curtos.
- Explica a configuração das saídas HTML e a busca de novas fontes pelo botão do painel.
- Resume a compatibilidade: Android 6 para HTML; Android 7 ou superior para HTML e NDI.
- Informa a rotina de testes e atualizações semanais do projeto.
- Reúne documentos técnicos e históricos em `docs/`, preservando o código-fonte e o APK 1.4.11.

# 1.4.11 — S-Retorno: nome, ícone e disponibilização no GitHub

- Em 27/10/2026, o aplicativo passou a se chamar **S-Retorno**, com ícones vetoriais tradicional, redondo, adaptativo e monocromático, além de novo banner para Android TV.
- Mantém o pacote e a assinatura das edições TVBox anteriores para atualização sem desinstalar. A versão exibida é **1.4.11 (16)**, sem o sufixo `-tvbox`.
- Preserva as bibliotecas NDI, a adaptação HTML e o funcionamento de conexão da 1.4.10. Foram aprovados 194 testes existentes, lint sem erros, compilação e conferência do APK. [Validação](historico/VALIDACAO_1.4.11.md).
- Em **29/10/2026**, o mantenedor relatou que **todas as boxes testadas e a TV Philips com Android rodaram perfeitamente**. [Registro dos testes em aparelhos](TESTES_EM_APARELHOS.md).
- Disponibiliza no GitHub o APK já verificado e o código-fonte ZIP da versão, com instruções de atualização e SHA-256. [Downloads](DOWNLOADS.md).

# 1.4.10 — Corrigir o envio da configuração pelo navegador (26/10/2026)

- Corrige a recusa **Abra o endereço mostrado na TV** ao enviar o formulário no PC, mesmo com o endereço correto. A política no-referrer da 1.4.9 fazia o navegador enviar Origin null; o app recusava esse POST.
- Usa Referrer-Policy same-origin e mantém a validação estrita de Host/Origin, código e sessão. Origin null ou externo continua recusado.
- Acrescenta teste em Chromium com formulário real, servidor Java de produção e credencial sintética, em áreas de desktop/celular. Reproduz a recusa nas classes da 1.4.9 e verifica recebimento nas classes corrigidas.
- Mantém pacote, certificado, preferências, catálogo HTML, código curto, duração da sessão, bibliotecas nativas e áudio. [Validação](historico/VALIDACAO_1.4.10.md).

# 1.4.9 — Configurar a box pelo celular ou PC (26/10/2026)

- Acrescenta **Configurar pelo celular ou PC**: a TV mostra endereço temporário e código de seis dígitos. No navegador da mesma rede, o usuário cola o link completo do Editor remoto do S-Present. Não precisa digitar a chave no controle.
- Configuração por solicitação, um envio, cinco minutos, cinco códigos incorretos; fecha ao cancelar ou sair do app. Valida tamanho, tempo, origem e código, sem guardar a chave em URL de reprodução ou resposta da página.
- Aceita o link completo no próprio aparelho e mantém endereço/chave em **Configuração manual**. Preserva a fonte escolhida e exige selecionar a saída após buscar.
- Corrige a orientação da 1.4.8: a criação manual de chaves da interface S-Present 0.3.50 omite advanced:read. O link do editor oferece essa permissão e também escrita; o Monitor continua somente consultando.
- Mantém certificado/pacote TVBox, armazenamento protegido, perfil HTML Chrome 44–86 e bibliotecas/áudio da 1.4.8. Teste em rede e boxes reais ainda é necessário. [Configuração](SAIDAS_SPRESENTER.md) e [validação](historico/VALIDACAO_1.4.9.md).

# 1.4.8 — Catálogo dos destinos HTML do S-Present (26/10/2026)

- Acrescenta configuração de um PC e chave de leitura advanced:read para consultar GET /api/v1/advanced. Lista todos os destinos network habilitados, com nomes/URLs atuais, incluindo slugs personalizados e gerados.
- Uma resposta válida atualiza nomes, acrescenta destinos e remove entradas anteriores excluídas/desabilitadas. Preserva a última URL escolhida e exige seleção explícita de outra fonte; não migra a chave para novos IPs.
- Prioriza a consulta na rede Wi-Fi/Ethernet do PC configurado, dentro do limite de alvos/trabalhadores/cancelamento. Outros PCs mantêm a busca anterior de caminhos conhecidos.
- Chave somente no cabeçalho da origem configurada; sem redirecionamentos ou comandos de escrita. Limites de 1 MiB, cinco segundos e 512 registros; falhas deixam o catálogo anterior disponível e mostram o motivo.
- Armazenamento AES/GCM/AndroidKeyStore vinculado à origem; firmware sem suporte usa apenas a sessão e informa essa condição. Remover a conexão preserva a seleção HTML.
- Mantém a adaptação Chrome 44–86, pacote/certificado TVBox, bibliotecas nativas e áudio da 1.4.7. A captura confirmou Cannot GET /output/ no servidor, sem listagem nesse caminho.
- [Configuração](SAIDAS_SPRESENTER.md) e [validação](historico/VALIDACAO_1.4.8.md). API e firmware reais ainda precisam ser testados. Nenhum release ou envio à Play.

# 1.4.7 — Adaptação ES5 para a BTV B9 com Chrome 44 (26/10/2026)

- Estende o perfil da captura conhecida para Chrome 44–86 após a foto da B9/API 23 registrar `Unexpected token )` em `main.ff832fdd.js:2`. A 1.4.6 já abriu o menu nesse aparelho.
- Converte todo o programa empacotado para ES5 e acrescenta a base ECMAScript antes dos módulos de APIs web, incluindo fallback de fetch. CSS preparado para Chrome 44.
- Renomeia bindings let/const por escopo antes da conversão: evita que o var de um laço do programa minificado oculte o carregador de módulos externo. Preserva chaves públicas e valores capturados por closures.
- Diagnóstico manual aguarda até cinco segundos por resposta do motor antigo; não acrescenta consultas periódicas.
- Mantém verificações de origem/hash, limites de rede/cache, pacote/certificado TVBox e bibliotecas nativas. Android 6 continua em HTML, sem NDI.
- 117 testes Android aprovados, lint sem erros, cinco fixtures de semântica ES5 e fixture HTTP/WebSocket de letras/elementos aprovadas. JSDOM não comprova pixels, mídia ou execução no Chrome 44 físico; uma execução inicial da fixture expirou sem erro JavaScript e a repetição passou.
- APK de teste 1.4.7-tvbox (12); [validação detalhada](historico/VALIDACAO_1.4.7.md). Esta revisão não corrige o áudio associado a letras.

# 1.4.6 — Adaptação da captura HTML para a HTV com Chrome 52 (26/10/2026)

- Adapta o JavaScript/CSS de uma captura fornecida pelo mantenedor na compilação, com alvo Chrome 52 e fallbacks de APIs e posicionamento por quatro bordas.
- Em motores Chrome 52–86, só entrega um arquivo adaptado após conferir nome, origem e SHA-256 do recurso atual no servidor e do asset local. Motores atuais e recursos diferentes mantêm o carregamento original.
- Limita a quatro verificações por renderizador, 2 MiB por recurso, origem da fonte selecionada e GET sem redirecionamento. A cache é por URL completa; fechar não bloqueia a interface aguardando rede.
- Diagnóstico HTML informa aplicação da adaptação e passa a incluir arquivo/linha de erros de JavaScript.
- Mantém pacote/certificado TVBox, preferências, parada da descoberta, enquadramento, fonte Android corrigida na 1.4.5 e as mesmas bibliotecas nativas/avisos NDI.
- 114 testes Android aprovados; fixture de DOM com APIs removidas confirmou atualizações por WebSocket. Chrome 52 físico, pixels, vídeo e áudio ainda exigem teste na box.
- Captura/programa de terceiros gerado, SDK, chaves e APKs permanecem fora do Git. Abertura do menu 1.4.5 na HTV foi confirmada nas fotos do mantenedor.

# 1.4.5 — Fonte do tema compatível com o relatório da HTV (25/10/2026)

- Substitui o literal global `android:fontFamily` por `android:typeface` sem serifa, que usa enumeração inteira.
- Correção dirigida ao relatório real da HTV-6H/API 25: leitura da fonte no índice 74 de um pool XML com 49 strings ao inflar o botão NDI/HTML.
- Amplia os testes existentes de compatibilidade para APIs 28 e 34, além de 23/25; a suíte de interface também mantém API 35.
- Mantém pacote e certificado TVBox, recuperação, última fonte, busca, HTML, áudio e bibliotecas nativas da 1.4.4.
- Funcionamento real na HTV após esta correção ainda depende do teste no aparelho.

# 1.4.4 — Abertura independente e recuperação em TV Box (25/10/2026)

- Primeira configuração em API 23–25 começa em HTML sem inicializar NDI; preserva a restauração de uma fonte NDI já selecionada.
- HTML, callbacks de superfície e limpeza não carregam o runtime NDI por conta própria.
- Entrada independente para launcher, tela inicial e boot, com relatório local e opção de abrir apenas HTML após abertura interrompida.
- Checkpoints limitados à abertura, inicialização e primeira descoberta; registro de exceção Java mantém o tratamento normal do Android. Nenhuma telemetria.
- Tipos de recorte/insets das APIs 28/30 ficam isolados da Activity carregada em Android antigo.
- APK TVBox conserva pacote e certificado de teste da 1.4.3. Bibliotecas nativas, áudio e exibição HTML não foram alterados.
- Candidata para investigar o fechamento antes do menu relatado na H6+ com Android 7.1.2; a causa e o funcionamento real ainda dependem do relatório/teste do aparelho.

# 1.4.2 — Descoberta de saídas nomeadas e exibição HTML de monitor

## 1.4.3 — teste de compatibilidade em TV Box (22/10/2026)

- HTML passa a ter mínimo Android 6 / API 23; NDI mantém o limite de API 24 dos binários integrados.
- `NdiReceiver` impede carregamento JNI em API 23 e protege HTML de erros de ligação das bibliotecas NDI.
- Tipos de navegação Android 13 e recuperação de renderizador Android 8 ficam isolados dos caminhos de inicialização antigos; o overload de navegação WebView usado em API 23 também é tratado.
- Nova exibição 16:9 com a densidade do firmware, persistida e inicial em API 23–25, sem reescrever o tema ou forçar renderização por CPU.
- Diagnóstico local e sob demanda do app, Android/API, ABIs, motor WebView, área CSS e recursos CSS. Nenhuma telemetria ou busca de fontes é acrescentada.
- Build `tvboxTest` usa pacote e nome próprios para instalar ao lado da edição habitual.
- Recepção NDI, rotas de busca HTML, última fonte salva e parada da busca mantidas; layout real nas boxes ainda depende do teste no equipamento.


- Procura também /output/retorno e /output/retono, além de um nome de saída salvo elegível. Tenta outro nome somente depois de HTTP 404/410; mantém o limite de trabalhadores e evita repetir conexões em hosts indisponíveis.
- Aceita até dois redirecionamentos limitados à exibição Stage no mesmo servidor; conserva a URL curta da fonte selecionada.
- Acrescenta exibição HTML de monitor 16:9 com área de referência de 1920 × 1080, ajuste proporcional inteiro, texto em 100% e contexto de densidade 1x restrito ao WebView.
- Permite escolher a página original na engrenagem e salva essa preferência por aparelho.
- Preserva modo, fonte, início automático e parada da descoberta depois da seleção.

# 1.4.1 — Última fonte restaurada e descoberta parada após selecionar

- Restaura diretamente o modo e a fonte confirmada ao iniciar, com painel fechado e sem busca geral de outras fontes.
- Salva modo e origem juntos; navegar por outro modo ainda sem fonte não substitui a última seleção persistida.
- Libera o finder NDI após a seleção ou fechamento da busca, além de cancelar a varredura HTML e seus temporizadores.
- Reabrir apenas o painel não reinicia a descoberta. O botão atualizar permite procurar outra fonte; a configuração sem fonte mantém a descoberta inicial.
- Pausa a recepção NDI em segundo plano e retoma a mesma origem ao retornar; descarta conexões enfileiradas que já foram canceladas.
- Mantém os dados e as chaves de preferências da versão 1.4.0.

# 1.4.0 — Busca de retornos HTML pelo controle remoto

- Busca o padrão `:5050/output/saida-html-1` em Wi-Fi/Ethernet IPv4, com até 12 requisições simultâneas, leitura limitada e cancelamento.
- Exibe os candidatos HTML na lista e salva a escolha por aparelho, sem exigir digitação para o padrão suportado.
- Mantém a ordem e as fontes durante novas buscas; tolerância de 90 s para HTML ausente, preservando o endereço salvo.
- Repete a busca 30 s após cada ciclo com o painel aberto; para ao fechar o painel, sair ou trocar de modo.
- Move o endereço manual para um diálogo e separa buscar fontes de recarregar a página.
- Preserva o modo NDI, o renderizador HTML e os avisos do SDK.
- Acrescenta documentação completa de uso, arquitetura, rede, compilação, validação, assinatura e publicação.

# 1.3.0 — NDI e retorno HTML

- Acrescenta seletor NDI/HTML no cabeçalho compacto, preservando a área da lista.
- Abre o link real de Stage HTML, com IP, porta, caminho e parâmetros editáveis.
- Expande um IP digitado para a porta 5050 e o caminho `/output/saida-html-1`;
  preserva endereços HTTP/HTTPS completos.
- Salva modo, endereço HTML e fonte NDI separadamente por aparelho.
- Encerra a recepção NDI ao entrar em HTML e destrói o renderizador web ao
  voltar para NDI ou sair do aplicativo, encerrando a mídia da página.
- Acrescenta recarga, tempo limite e novas tentativas de carregamento da página.
- Valida endereços web, bloqueia navegação para esquemas locais/externos e
  mantém a verificação de certificados HTTPS.
- Permite HTTP para o servidor local do Spresenter. O modo HTML não acrescenta
  áudio nem garante recursos que a página não forneça.

# Alterações

## Revisão de teste 1.2.0 — `versionCode 3`

- descobridor NDI persistente com atualizações contínuas, separadas da fila de conexão;
- atualização manual sem limpar fontes, ordem estável e tolerância de 10 segundos a ausências transitórias;
- barra com três ícones compactos e lista priorizada; configurações de Android/tela inicial movidas para diálogo com rolagem;
- controles adaptáveis, preservação da navegação por controle remoto e áreas de toque de pelo menos 48 dp;
- nome provisório “Monitor de Retorno”, pacote Android preservado e avisos do SDK disponíveis nas configurações;
- target/compile API 36, AGP 8.13.2, Gradle 8.13, NDK r28 e alinhamento de 16 KB;
- testes do catálogo, descobridor e layout; publicação de produção e testes em rede real permanecem etapas separadas.

## Preparação para publicação no GitHub

- preservada a licença MIT escolhida pelo mantenedor;
- atualizado o README com requisitos, comandos de compilação e uso;
- corrigidos o nome do APK e os caminhos e menus no guia;
- ampliado o `.gitignore` para caches nativos, chaves de assinatura, pacotes gerados e arquivos do SDK;
- documentadas as limitações da tela inicial opcional e de páginas de memória de 16 KB;
- separado o código-fonte da distribuição do APK com componentes NDI;
- mantido o código de recepção e exibição da versão testada, sem alterações funcionais nesta preparação.

## Revisão 1.1.0 — `versionCode 2`

- adicionada a possibilidade de escolher o monitor como tela inicial da TV Box;
- adicionados botões para escolher a tela inicial e abrir as configurações do Android;
- mantido acesso ao launcher original para sair da configuração de tela inicial;
- adicionada ocultação do menu e do status durante o vídeo, com revelação temporária por toque ou controle;
- ajustado o comportamento de Voltar para abrir ou fechar o painel;
- adicionado ícone de monitor;
- alterado o nome do arquivo de saída para `NDI Monitor.apk`;
- corrigida a visibilidade pública de `onWindowFocusChanged`.

## Revisão 1.0.0

- substituída a base demonstrativa por recepção NDI via JNI/C++;
- adicionada descoberta de fontes com espera para mDNS;
- adicionada exibição de vídeo RGBX/RGBA em `ANativeWindow`;
- adicionada saída de áudio PCM por `AudioTrack`;
- adicionada seleção e nova busca de fontes;
- adicionada memória da última fonte;
- adicionada reconexão automática do receptor;
- adicionada opção de iniciar após o boot;
- adicionada interface em português para TV Box;
- adicionada preservação da proporção do vídeo;
- adicionado modo compilável sem o SDK proprietário;
- adicionados scripts para copiar os arquivos do SDK instalado pelo usuário;
- scripts agora invalidam o cache nativo para ativar o SDK logo na próxima compilação;
- adicionada documentação de instalação e teste.
