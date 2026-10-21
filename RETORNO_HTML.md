# NDI e retorno HTML no mesmo aplicativo

A revisão 1.3.0 acrescenta o modo HTML ao Monitor de Retorno. Cada aparelho
guarda seu modo e sua fonte: uma TV pode receber NDI enquanto outra abre o
retorno HTML do Spresenter. A exibição usa um modo por vez em cada aparelho.

## Abrir o retorno

1. Ative um destino **Saída HTML** em **Retorno (Stage)** no Spresenter e copie
   o endereço exibido nessa configuração.
2. Mantenha o computador e o aparelho Android em uma rede que permita a
   comunicação entre eles. Uma rede de convidados pode impedir esse acesso.
3. No aplicativo, abra o painel e toque no seletor **NDI ▾** do cabeçalho.
   Escolha **HTML — endereço do retorno**.
4. Cole o link e toque em **Salvar e abrir retorno**.

O endereço completo fica editável. Exemplo de formato observado na configuração
de Stage: `http://IP_DO_PC:5050/output/saida-html-1`. O IP, a porta e o caminho
devem ser os fornecidos pela sua instalação; o nome do destino pode mudar.

Para facilitar, digitar apenas um IP ou nome de computador faz o aplicativo
acrescentar a porta `5050` e o caminho `/output/saida-html-1`. Se esse destino
for diferente no seu Spresenter, cole o link completo. Um endereço que já
começa com `http://` ou `https://` é preservado, inclusive porta, caminho e
parâmetros. O aplicativo não tenta descobrir o PC automaticamente.

## Uso diário

- O modo HTML salva o endereço neste aparelho e o reabre quando o aplicativo
  inicia novamente. O NDI mantém sua memória de fonte separada.
- Toque na imagem ou use OK/Menu/Voltar no controle remoto para revelar os
  controles. O seletor do cabeçalho permite voltar ao NDI.
- O ícone de atualização recarrega a página no modo HTML; no modo NDI, consulta
  as fontes sem apagar a lista.
- A troca para HTML encerra a recepção NDI. A troca para NDI destrói o renderizador
  web, encerrando sua reprodução de mídia. Sair do aplicativo também encerra o
  renderizador HTML; ao retornar, a página é reaberta.
- Se a página não carregar por falha de rede ou erro do servidor, há novas
  tentativas com intervalo crescente, até 30 segundos. Um erro de endereço,
  autorização HTTP ou certificado exige conferência e recarga pelo menu.

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
