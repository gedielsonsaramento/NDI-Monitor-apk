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
