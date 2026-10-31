# Validação — S-Retorno 1.5.2 + Central 0.1.2

Data: 31/10/2026. Revisão compilada para teste nas boxes do projeto. O teste físico na igreja continua pendente.

## Resultado

- **281 testes Android aprovados**, sem falhas, erros ou testes ignorados, incluindo Android 6/API 23, Android 7/API 25 e as plataformas cobertas pela suíte existente.
- **14 testes do plugin aprovados**, incluindo o envelope do SDK com `MessageEvent.source` nulo, tentativa visível, confirmação da lógica e aviso quando o plugin não responde.
- **Lint: zero erros, dez avisos**. Os avisos são de compatibilidade/orientação, backup legado, estrutura de layout, recursos não usados, autofill e composição de texto.
- **Compilação TVBox concluída**, versão 1.5.2, código 20, pacote `br.com.polegar.ndimonitor.tvboxtest`.
- Certificado igual ao APK 1.5.1; as quatro bibliotecas nativas ARM32/ARM64 são idênticas byte a byte às dessa base.
- O APK contém manifesto, JavaScript/CSS adaptados e avisos do perfil HTML; hashes conferidos. Esses arquivos estavam ausentes no APK 1.5.1.

## Regressões verificadas

Os testes Android cobrem migração da configuração de um PC para vários, chaves separadas por origem, manutenção da fonte selecionada, remoção de um PC sem apagar os demais e preservação de catálogos da Central. Duas APIs HTTP reais em loopback foram consultadas com as respectivas chaves, incluindo destinos fora da faixa da box; uma falha de autorização não remove o outro catálogo.

A busca pública cobre a faixa adicional e o endereço salvo fora da sub-rede, sem cabeçalho de autorização. O monitor de rede agrupa notificações repetidas, detecta alteração de IPv4, ignora rede móvel/VPN e cancela callbacks ao sair. Uma atividade com fonte salva encontra outro PC após a mudança de rede sem remover o anterior, abrir o painel ou trocar a reprodução. Abrir a lista inicia uma busca pública; a atualização explícita consulta os catálogos configurados.

A interface real da Central foi executada em Chromium com `LanControlServer` Java em loopback. Passaram: relay com origem nula, erro de código inválido dentro da janela de vínculo, varredura de 254 endereços com um app encontrado, pareamento, leitura e distribuição do catálogo sem distribuir o token, cancelamento/execução confirmada de manutenção, escolha NDI, layout estreito e largo e desligamento/reconexão. Nenhum erro de console.

## Compatibilidade HTML e foto recebida

Cinco fixtures da transformação JavaScript passaram. O programa adaptado foi aceito como ES5. A captura fornecida foi executada em JSDOM com APIs modernas removidas; os polyfills, as quatro bordas explícitas do contêiner e as atualizações de letra/elementos por WebSocket passaram.

Em Chromium atual, o mesmo perfil manteve tema de **1920 × 1080**, área da letra com **1728 × 540**, dentro da tela, e atualização por WebSocket. A fixture passou a publicar uma atualização inicial após a conexão, eliminando uma corrida entre a abertura do WebSocket e as consultas HTTP.

A falta do perfil no APK anterior é uma causa plausível para a composição comprimida fotografada, mas a foto não comprova uma causa única. O perfil só é aplicado ao JavaScript/CSS conhecido, depois de conferir os hashes, em motores Chrome 44–86. Outro build do Spresenter pode exigir uma nova captura; o app não substitui arquivos desconhecidos. Não foi executado o WebView físico da box nem comprovada a atualização de mídia nela.

## Reproduzir

```sh
./gradlew :app:testDebugUnitTest :app:lintTvboxTest :app:assembleTvboxTest -PmonitorPrebuiltNativeDir=../local-native
cd central-retornos
npm test
python3 scripts/package.py
```

Ferramentas usadas: JDK 17, Gradle 8.13, Android Gradle Plugin 8.13.2, SDK Android 36. O projeto completo contém os arquivos de compatibilidade HTML e o runtime nativo reutilizado. [Compilação e assinatura](../COMPILAR_1.5.2.md).

## Identidade do APK

- Arquivo: `S-Retorno-1.5.2.apk`
- Tamanho: 16250657 bytes
- SHA-256: `0929b70668e8f0fa2fe78572827cdb718b5c794400086dd8dbc602a0f7315bdb`
- SHA-256 do certificado: `6007ea0469cbe58451d689913869fcfd617e6e686967ed8a1e113091eac51421`

## Conferência na igreja

1. Atualizar o APK sobre a 1.5.1, sem desinstalar, e instalar Central 0.1.2 no Spresenter.
2. Conferir tentativa/erro de vínculo, localização da box e envio do catálogo pelo link com token.
3. Trocar de rede com os PCs antigos ainda cadastrados; abrir a lista e selecionar uma fonte do novo endereço.
4. Conferir letras, relógio e disposição do tema da foto na box; testar também uma atualização do conteúdo.
5. Desligar e religar os aparelhos na rotina do culto: ausência permanece **Sem comunicação**, sem novo relatório de falha.
