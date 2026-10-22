# 1.4.2 — Descoberta de saídas nomeadas e exibição HTML de monitor

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
