# Monitor de Retorno — tela inicial opcional

O aplicativo pode ser escolhido como tela inicial (Home) da TV Box. Não usa root,
não bloqueia aplicativos e não interrompe serviços de terceiros. Não há promessa
de melhora de desempenho nem recuperação automática de travamentos.

## Atualizar o projeto que já recebe NDI

Feche o Android Studio. Faça uma cópia de segurança do projeto antigo.
Use a versão completa atualizada do código em uma pasta separada e integre novamente
sua cópia local do SDK pelos scripts de instalação. A revisão 1.4.1 inclui os
dois modos e a busca HTML, além das melhorias de descoberta e interface da 1.2.
Use o conjunto completo dos arquivos atuais, incluindo:

- app/build.gradle
- app/src/main/AndroidManifest.xml
- app/src/main/java/br/com/polegar/ndimonitor/MainActivity.java
- app/src/main/res/layout/activity_main.xml
- app/src/main/res/drawable/ic_monitor.xml
- app/src/main/java/br/com/polegar/ndimonitor/LanHtmlDiscovery.java
- app/src/main/res/layout/dialog_html_address.xml

Não substitua cpp/include nem jniLibs: preserve seu SDK NDI instalado.
Abra a raiz do projeto atualizado no Android Studio, sincronize o Gradle e use
Build > Generate App Bundles or APKs > Generate APKs, ou execute
`.\gradlew.bat :app:assembleDebug` no PowerShell na raiz.
O novo APK fica em app/build/outputs/apk/debug/app-debug.apk.
Instale como atualização usando a mesma chave de assinatura anterior.

## Ativar na TV Box

Abra a engrenagem de configurações no painel e use “Escolher tela inicial”.
Escolha Monitor de Retorno como aplicativo de início padrão. Em alguns aparelhos,
pressione Home e escolha Monitor de Retorno > Sempre. A disponibilidade depende do
firmware. Não selecione como tela inicial do celular se não desejar esse comportamento.
Mantenha marcada a abertura após ligar e selecione uma fonte para salvar.
O modo e a fonte confirmada são restaurados ao abrir. Depois de selecionar,
a busca para; reabrir o painel não a reinicia. Use atualizar para procurar
outra origem.
Reinicie a TV Box e teste a abertura e a reconexão. Esse comportamento depende
do firmware e das restrições da versão do Android, e deve ser validado no
aparelho de uso final.

## Desativar e manter acesso ao Android

Toque no vídeo, abra o painel, depois a engrenagem e use “Escolher tela inicial”.
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
