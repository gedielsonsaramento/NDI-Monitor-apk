# Descobrir as saídas HTML do S-Present

Disponível na candidata **1.4.8-TVBox (13)**. Configura uma conexão de leitura por instalação do app, para consultar todos os destinos HTML habilitados desse PC e seus nomes atuais.

## Configurar uma vez

No computador com o S-Present:

1. Abra **Configurações → API** e ative a API.
2. Crie uma chave para o Monitor de Retorno com a permissão **advanced:read** (leitura). Essa é a permissão exigida pela listagem usada nesta revisão.
3. Copie o endereço da API e a chave exibida. Uma chave existente pode ser usada se já possuir essa permissão. Nenhuma permissão de escrita é necessária.

No app Android:

1. Selecione **HTML** e abra **engrenagem → Saídas HTML do S-Present**.
2. Informe o endereço da API do PC. Exemplo da rede observada: `http://192.168.2.53:5050/api/v1`. Um IP sem porta usa HTTP/5050. Um endereço completo mantém a porta e o protocolo informados.
3. Cole a chave no campo próprio e toque em **Salvar e buscar saídas**.
4. Selecione uma saída da lista. Esse acionamento continua sendo necessário para escolher qual retorno exibir.

O campo da chave fica vazio ao reabrir o diálogo. Deixá-lo vazio mantém a chave existente somente para o mesmo endereço do PC. Se mudar o endereço, informe a chave novamente. **Remover conexão da API** remove essa configuração, preservando a seleção HTML existente.

## Atualização dos nomes e destinos

O botão **↻** do cabeçalho consulta novamente os destinos do PC configurado. Uma consulta bem-sucedida acrescenta novas saídas, atualiza nomes e remove destinos da listagem anterior que foram excluídos ou desabilitados. O nome vem do destino HTML; quando vazio, usa o nome do canvas, Retorno (Stage) ou o slug como alternativa.

As saídas são obtidas dos registros do S-Present. Exemplos como `musicos`, `madureira` e `canvas-2` não são uma lista fixa de nomes procurados. A mesma consulta pode retornar várias saídas do mesmo PC. Não para no primeiro destino.

Uma alteração de nome amigável com a mesma URL atualiza o rótulo. Se o S-Present mudar também o caminho da URL, selecione a nova entrada. O app não troca a fonte em reprodução por conta própria. O último endereço escolhido permanece disponível como **Último retorno salvo**, mesmo que tenha sido removido do catálogo ou esteja temporariamente indisponível.

Enquanto a busca solicitada estiver aberta, mantém o intervalo existente de 30 segundos. Selecionar uma fonte, fechar o painel, trocar de modo ou sair cancela a busca. Na próxima abertura do app, a fonte escolhida é restaurada diretamente; a API não é consultada em segundo plano para substituir essa seleção.

## Endereço e chave

A conexão vale para um PC por instalação do app. Outros PCs ainda podem aparecer pela busca anterior de caminhos conhecidos; para listar todos os destinos de outro PC, altere a conexão configurada. Se o IP do PC mudar, atualize o endereço nas configurações. A chave não é enviada automaticamente aos novos IPs encontrados na varredura.

A consulta faz somente **GET** no endpoint `BASE/advanced`, sendo `BASE` a URL `/api/v1` do PC. A chave vai no cabeçalho Authorization, nunca na URL de reprodução. Não segue redirecionamentos nem altera destinos, camadas, temas ou apresentações.

Em aparelhos com AndroidKeyStore funcional, a chave é armazenada com AES/GCM, vinculada ao endereço configurado. Se o firmware não permitir esse armazenamento, o app avisa e mantém a chave somente na memória da sessão. Após encerrar o processo nesse caso, será necessário informá-la novamente. A chave não aparece na lista de fontes ou nos relatórios.

## Se a lista não aparecer

- **Chave inválida ou revogada:** confira a chave em Configurações → API e atualize a conexão no app.
- **Ative o acesso e confira advanced:read:** confira a ativação da API e a permissão de leitura da chave.
- **API indisponível/incompatível:** use o endereço da API mostrado pelo S-Present. A rota foi identificada no pacote 0.3.50 fornecido pelo mantenedor; versões diferentes podem oferecer outro contrato.
- **Zero saídas:** confira se os destinos **Saída HTML** estão habilitados. Saídas NDI e displays físicos não entram nesse catálogo.
- **Falha de rede:** confira IP, porta, rede Wi-Fi/Ethernet e acesso do aparelho ao PC. Uma falha não apaga imediatamente os resultados anteriores.

A captura `Erro.html` recebida em 26/10/2026 confirmou `Cannot GET /output/` no PC. Esse caminho não fornece a lista nessa instalação. Não é necessário apagar o nome amigável das saídas.

O catálogo não confirma que a página será renderizada corretamente. A adaptação Chrome 44–86 da 1.4.7 continua no pacote. Enquadramento, mídia, áudio e estabilidade ainda exigem teste físico. Esta revisão não corrige o áudio associado às letras.

Veja [TVBOX.md](TVBOX.md) e [VALIDACAO_1.4.8.md](VALIDACAO_1.4.8.md).
