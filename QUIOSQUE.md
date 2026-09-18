# NDI Monitor 1.1 — tela inicial opcional

O aplicativo pode ser escolhido como tela inicial (Home) da TV Box. Não usa root,
não bloqueia aplicativos e não interrompe serviços de terceiros. Não há promessa
de melhora de desempenho nem recuperação automática de travamentos.

## Atualizar o projeto que já recebe NDI

Feche o Android Studio. Faça uma cópia de segurança do projeto antigo.
Extraia o pacote atualizado em outra pasta e copie estes arquivos para os mesmos
locais no projeto antigo, substituindo apenas esses arquivos:

- app/build.gradle
- app/src/main/AndroidManifest.xml
- app/src/main/java/br/com/polegar/ndimonitor/MainActivity.java
- app/src/main/res/layout/activity_main.xml
- app/src/main/res/drawable/ic_monitor.xml

Não substitua cpp/include nem jniLibs: preserve seu SDK NDI instalado.
Abra a raiz do projeto antigo no Android Studio, sincronize o Gradle e use
Build > Generate App Bundles or APKs > Generate APKs, ou execute
`.\gradlew.bat :app:assembleDebug` no PowerShell na raiz.
O novo APK fica em app/build/outputs/apk/debug/NDI Monitor.apk.
Instale como atualização usando a mesma chave de assinatura anterior.

## Ativar na TV Box

Abra o painel de fontes e use “Definir tela inicial / sair do quiosque”.
Escolha NDI Monitor como aplicativo de início padrão. Em alguns aparelhos,
pressione Home e escolha NDI Monitor > Sempre. A disponibilidade depende do
firmware. Não selecione como tela inicial do celular se não desejar esse comportamento.
Mantenha marcada a abertura após ligar e selecione uma fonte para salvar.
Reinicie a TV Box e teste a abertura e a reconexão. Esse comportamento depende
do firmware e das restrições da versão do Android, e deve ser validado no
aparelho de uso final.

## Desativar e manter acesso ao Android

Toque no vídeo, abra o menu e use “Definir tela inicial / sair do quiosque”.
Escolha o launcher original. O botão “Configurações do Android” permanece
disponível no painel. No controle, OK revela os controles; OK novamente abre
o painel. Voltar abre ou fecha o painel em vez de encerrar o monitor.

## Desempenho

Pelas configurações do Android, desinstale ou desative apenas aplicativos
dispensáveis conhecidos por você. Não desative launcher original, serviços de
rede, instalador, configurações ou componentes do sistema. O APK não aplica
essas alterações. Não há lista de apps autorizados nesta versão.

## Validação

Compilação e teste em aparelho devem ser feitos no seu Android Studio/TV Box.
Confira vídeo, áudio, ocultação de controles, retorno pelo Home, reinício e saída
para o launcher original antes de usar em produção.
