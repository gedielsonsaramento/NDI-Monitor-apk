# NDI e retorno HTML no mesmo aplicativo

A revisão 1.4.2 acrescentou exibição de monitor 16:9, aceita o redirecionamento
para o Stage e procura também os nomes de saída `retorno` e `retono`. Mantém
a última fonte restaurada e a busca parada após a seleção. Cada aparelho
guarda seu modo e sua fonte: uma TV pode receber NDI enquanto outra abre o
retorno HTML do Spresenter. A exibição usa um modo por vez em cada aparelho.

A revisão **1.4.3** acrescenta HTML em Android 6 (API 23), proteção quando a biblioteca NDI não carrega, exibição 16:9 com densidade do próprio firmware e diagnóstico sob demanda. NDI permanece restrito a API 24 ou superior. Veja o procedimento de teste em [TVBOX.md](TVBOX.md).

Na **1.4.4**, abrir HTML não inicializa o NDI em nenhuma versão do Android. Uma primeira configuração em API 23–25 começa em HTML; uma fonte NDI previamente escolhida continua sendo restaurada. A recuperação oferece **Abrir apenas HTML** se a abertura anterior foi interrompida. Os endereços, a descoberta e o enquadramento HTML mantêm o comportamento anterior.

## Abrir o retorno

1. Ative um destino **Saída HTML** em **Retorno (Stage)** no Spresenter e confira
   o endereço exibido nessa configuração.
2. Mantenha o computador e o aparelho Android em uma rede que permita a
   comunicação entre eles. Uma rede de convidados pode impedir esse acesso.
3. No aplicativo, abra o painel e toque no seletor **NDI ▾** do cabeçalho.
   Escolha **HTML — retornos na rede**.
4. Aguarde os candidatos aparecerem na lista. Selecione o retorno com as setas
   e OK do controle remoto ou por toque; o endereço fica salvo nesse aparelho.
5. Se necessário, use **Informar endereço**, cole o link e acione **Salvar e abrir retorno**.

Com endereço já salvo, o aplicativo o reabre diretamente e mantém o painel
fechado, sem varrer a rede. Reabrir apenas o painel mostra as fontes conhecidas;
use o ícone de atualização do cabeçalho para iniciar uma busca por outras fontes.

O endereço completo fica editável. Exemplo de formato observado na configuração
de Stage: `http://IP_DO_PC:5050/output/saida-html-1`. O IP, a porta e o caminho
devem ser os fornecidos pela sua instalação; o nome do destino pode mudar.

Para facilitar, digitar apenas um IP ou nome de computador faz o aplicativo
acrescentar a porta `5050` e o caminho `/output/saida-html-1`. Se esse destino
for diferente no seu Spresenter, cole o link completo. Um endereço que já
começa com `http://` ou `https://` é preservado, inclusive porta, caminho e
parâmetros. A busca automática usa Wi-Fi/Ethernet com IPv4 e testa o padrão
porta 5050 com os caminhos `/output/saida-html-1`, `/output/retorno` e
`/output/retono`. Um nome salvo em `/output/NOME`, sem parâmetros e na porta
5050, pode ser tentado primeiro. Não procura todas as portas ou nomes de saídas.
Em redes maiores que /24, limita a varredura à faixa /24 do Android. Confira
[algoritmo, limites e endereços personalizados no manual](DOCUMENTACAO_COMPLETA.md).

Os resultados são páginas HTML acessíveis nesse endereço, não uma certificação
do conteúdo do retorno. Servidores com rotas genéricas podem responder HTML
para um caminho inexistente. Selecione e confira a página. Porta e caminho
personalizados podem precisar ser informados uma vez; os parâmetros do link salvo
continuam restritos ao seu servidor. Cada alvo tenta outro nome somente após
uma resposta HTTP 404/410; uma conexão que falha não inicia outras tentativas
para os nomes alternativos. O primeiro HTML válido encerra as tentativas desse alvo.

## Uso diário

- O modo HTML salva o endereço neste aparelho e o reabre quando o aplicativo
  inicia novamente. O NDI mantém sua memória de fonte separada.
- Toque na imagem ou use OK/Menu/Voltar no controle remoto para revelar os
  controles. O seletor do cabeçalho permite voltar ao NDI.
- O ícone do cabeçalho refaz a busca do modo atual sem limpar imediatamente
  a lista. No modo HTML, o botão ao lado de **Informar endereço** recarrega a página.
- A busca começa automaticamente na configuração sem endereço ou quando
  solicitada pelo botão atualizar. Durante essa sessão, repete 30 s após o
  ciclo anterior com o painel aberto. Ao selecionar, fechar o painel, trocar
  de modo ou sair do app, a busca é cancelada.
  Fontes HTML ausentes têm tolerância de 90 s; o último link salvo é preservado.
- A troca para HTML encerra a recepção NDI. A troca para NDI destrói o renderizador
  web, encerrando sua reprodução de mídia. Sair do aplicativo também encerra o
  renderizador HTML; ao retornar, a página é reaberta.
- O modo só substitui a configuração persistida quando há uma fonte escolhida.
  Navegar por outro modo sem configurar uma fonte preserva a última seleção
  para o próximo início. Se o retorno estiver indisponível, as tentativas usam
  o mesmo endereço salvo, sem substituir automaticamente a origem.
- Se a página não carregar por falha de rede ou erro do servidor, há novas
  tentativas com intervalo crescente, até 30 segundos. Um erro de endereço,
  autorização HTTP ou certificado exige conferência e recarga pelo menu.

## Exibição como monitor

O padrão **Monitor 16:9** prepara uma área de 1920 × 1080, como a saída Stage
mostrada na configuração do Spresenter. A cena inteira é reduzida ou ampliada
uniformemente para caber no aparelho; telas com outra proporção podem mostrar
faixas pretas. O renderizador usa texto em 100% e identificação de navegador
de computador para reduzir diferenças do layout móvel. Os controles do app
continuam usando a escala normal do Android.

No modo HTML, abra **engrenagem → Exibição HTML** para escolher entre
**Monitor 16:9 — ajustar à tela**, **Layout original da página** e
**Monitor 16:9 — compatibilidade TV Box**. A última opção mantém a densidade
real do WebView e é o padrão inicial em Android API 23–25. A preferência
fica salva no aparelho e a mesma URL é reaberta ao trocar a opção. O layout
original permite usar o comportamento responsivo fornecido pela própria página.

Esse ajuste não corrige posições ou cortes internos definidos no CSS do tema.
A validação final precisa conferir o conteúdo real do retorno no aparelho.

## Imagem, áudio e compatibilidade

O modo HTML exibe a página servida pelo Spresenter dentro do aplicativo. Ele
não transforma essa página em uma fonte NDI nem acrescenta áudio que ela não
fornece. A reprodução automática de mídia está habilitada no renderizador,
mas áudio, vídeos e atualização do tema precisam ser testados com a página real.

O acesso usa o componente web do aparelho. A disponibilidade e a compatibilidade
variam conforme o Android, o firmware e a versão do Android System WebView.
Se o componente estiver indisponível, o aplicativo informa a limitação e mantém
o seletor de modo acessível. Certificados HTTPS inválidos não são aceitos.

As novas tentativas cobrem o carregamento da página e erros do renderizador.
Elas não comprovam que a conexão interna de uma página já aberta continua
atualizando seu conteúdo; essa reconexão também depende da página do Spresenter.

## Referências

- [Spresenter: saídas e transmissão web](https://spresenter.com/pt)
- [Spresenter: recursos de vídeo e projeção](https://spresenter.com/pt/pitch/features)
- [Android: páginas dentro de um aplicativo](https://developer.android.com/develop/ui/views/layout/webapps/webview)

O material público consultado descreve saídas web e NDI, mas não fornece um
contrato técnico completo do áudio do Stage HTML nem comprova a limitação de
NDI à saída principal em todas as versões. Esta revisão aceita o link real
do retorno em vez de presumir que o exemplo `/slide?output=0` seja o Stage.
