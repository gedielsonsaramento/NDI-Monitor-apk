# Alterações

## Proposta 1.2.0 — preparação para celulares / Play (não publicada)

- removidos tela inicial opcional, inicialização após boot e entrada no launcher de TV;
- nome de produto provisório “Monitor de Retorno”, sem mudar o identificador de pacote;
- atualizados compile/target SDK 36, Gradle 8.13 e AGP 8.13.2;
- alinhamento ELF de 16 KB adicionado à biblioteca JNI; compatibilidade final deve ser verificada num AAB assinado e em dispositivo de 16 KB;
- navegação Voltar migrada para callback nas versões recentes do Android (testes manuais ainda pendentes);
- release bloqueada quando os arquivos locais do SDK e avisos de terceiros estiverem ausentes; disponibilizado botão para consultar esses avisos;
- nenhum SDK ou binário adicionado ao repositório; licença e assinatura ainda exigem avaliação antes de publicar.

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
