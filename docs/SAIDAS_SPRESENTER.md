# Descobrir as saídas HTML do Spresenter

No **S-Retorno 1.5.2**, podem ser cadastrados até **oito PCs**, cada um com seu endereço e sua chave. A configuração anterior é mantida na atualização. A box pode ser configurada pelo navegador do celular ou do próprio PC, sem digitar a chave no controle.

A **1.4.10** corrige a recusa **Abra o endereço mostrado na TV** durante o envio do formulário da 1.4.9, mesmo com o endereço certo. Atualize a box e abra uma nova sessão; o endereço/porta/código são temporários. Não reutilize uma aba de configuração anterior.

## Configurar uma vez

No computador com o Spresenter:

1. Abra **Configurações → Telas**. Na saída NDI existente, expanda a setinha e escolha **Abrir editor avançado**.
2. Na barra superior, abra **Editor remoto (rede)**, identificado pela descrição ao passar o mouse sobre o ícone.
3. Se **Publicar na rede** já estiver ligado, use **Copiar link com token**. Se estiver desligado, essa opção habilita a API e o editor remoto na rede; ative-a somente se desejar esse acesso.

O link autoriza leitura **e alteração do mapeamento das saídas**. O Monitor usa somente a consulta. Na versão Spresenter 0.3.50 analisada, a criação manual em **Configurações → API** não oferece o campo `advanced:read`, embora o servidor exija essa permissão. A orientação anterior para marcar esse campo estava incorreta para essa interface. `outputs:read` sozinho não substitui essa permissão.

Uma chave existente com `advanced:read` também pode ser usada na configuração manual abaixo, sem precisar habilitar o editor. O Monitor não exige permissão de escrita. Não compartilhe o link completo ou seu QR Code.

No app Android:

1. Selecione **HTML** e abra **engrenagem → Saídas HTML do Spresenter → Configurar pelo celular ou PC**.
2. A TV mostra um endereço próprio e um código de seis dígitos. Mantenha essa tela aberta.
3. No navegador do celular ou do PC conectado à mesma rede da box, digite o endereço mostrado **na TV**. Esse é o endereço temporário da box, diferente do endereço do Spresenter.
4. Na página, informe o código da TV e cole o **link completo** de **Copiar link com token**. Clique em **Enviar para esta TV**.
5. O app salva a conexão e busca as saídas. Selecione a saída desejada na lista da TV. Não precisa separar ou digitar a chave com o controle.

No próprio PC do Spresenter, o link já está na área de transferência: basta colá-lo no formulário do navegador. No celular, obtenha o link do editor remoto e cole-o inteiro. Este projeto não acrescenta leitor de QR ou acesso à câmera.

O código vale por até cinco minutos e para um único envio. Fechar a tela ou sair do app encerra a configuração. Cinco códigos incorretos também encerram a sessão. Para tentar novamente, abra a opção na TV para gerar outro código/endereço. Cada box tem sua própria sessão.

## Colar o link no próprio aparelho ou configurar manualmente

No celular com o Monitor instalado, é possível colar o link completo em **Ou cole o link completo neste aparelho** e tocar em **Usar link e buscar saídas**.

Em **Configuração manual**, informe a API do PC, como `http://192.168.2.53:5050/api/v1`, e uma chave com `advanced:read`; toque em **Salvar e buscar saídas**. Um IP sem porta usa HTTP/5050. Um endereço completo mantém a porta e o protocolo informados.

O campo da chave fica vazio ao reabrir o diálogo. Deixá-lo vazio mantém a chave existente somente para o mesmo endereço do PC. Se mudar o endereço, informe a chave novamente. **Remover conexão da API** remove essa configuração, preservando a seleção HTML existente.

## Atualização dos nomes e destinos

O botão **↻** do cabeçalho consulta novamente os destinos do PC configurado. Uma consulta bem-sucedida acrescenta novas saídas, atualiza nomes e remove destinos da listagem anterior que foram excluídos ou desabilitados. O nome vem do destino HTML; quando vazio, usa o nome do canvas, Retorno (Stage) ou o slug como alternativa.

As saídas são obtidas dos registros do Spresenter. Exemplos como `musicos`, `madureira` e `canvas-2` não são uma lista fixa de nomes procurados. A mesma consulta pode retornar várias saídas do mesmo PC. Não para no primeiro destino.

Uma alteração de nome amigável com a mesma URL atualiza o rótulo. Se o Spresenter mudar também o caminho da URL, selecione a nova entrada. O app não troca a fonte em reprodução por conta própria. O último endereço escolhido permanece disponível como **Último retorno salvo**, mesmo que tenha sido removido do catálogo ou esteja temporariamente indisponível.

Enquanto a busca solicitada estiver aberta, mantém o intervalo existente de 30 segundos. Selecionar uma fonte, fechar o painel, trocar de modo ou sair cancela a busca. Na próxima abertura do app, a fonte escolhida é restaurada diretamente; a API não é consultada em segundo plano para substituir essa seleção.

## Endereço e chave

Cada conexão vale para o PC informado. Cadastrar outro computador mantém os anteriores; a busca consulta todos os catálogos cadastrados. Os endereços aparecem nas configurações e podem ser removidos individualmente. Se o IP de um PC mudar, cadastre seu novo link e remova o endereço antigo. A chave não é enviada automaticamente aos novos IPs encontrados na varredura.

**Buscar em outra faixa de IP** permite indicar uma faixa IPv4 local, como `192.168.2`, para procurar saídas em endereços padrão na porta 5050. Deixe o campo vazio para retirar essa faixa adicional. A busca é limitada e não identifica nomes arbitrários de saídas; para a lista completa de cada PC, use seu catálogo. O acesso entre faixas depende das rotas e permissões da rede.

A consulta faz somente **GET** no endpoint `BASE/advanced`, sendo `BASE` a URL `/api/v1` do PC. A chave vai no cabeçalho Authorization, nunca na URL de reprodução. Não segue redirecionamentos nem altera destinos, camadas, temas ou apresentações. Importar o link do editor apenas extrai a origem e a chave localmente: o app não abre a página `/warp/` para controlar o Spresenter.

A página temporária de configuração usa HTTP na rede local, sem serviço de nuvem. Só abre por solicitação e escuta um endereço IPv4 de Wi-Fi/Ethernet atribuído à box, em porta temporária. Recebe o link pelo corpo de um POST após conferir o código; não o coloca na URL nem o devolve na resposta. Limita cabeçalho/corpo a 8 KiB, tempo por pedido a três segundos, duração a cinco minutos e tentativas de código a cinco. Valida Host/Origin, bloqueia formulários de outras origens e não disponibiliza CORS, acesso a arquivos ou controles de apresentação. A página e as respostas usam no-store. Use uma rede local de confiança; isolamento de clientes/convidados pode impedir o acesso.

Em aparelhos com AndroidKeyStore funcional, a chave é armazenada com AES/GCM, vinculada ao endereço configurado. Se o firmware não permitir esse armazenamento, o app avisa e mantém a chave somente na memória da sessão. Após encerrar o processo nesse caso, será necessário informá-la novamente. A chave não aparece na lista de fontes ou nos relatórios.

## Se a lista não aparecer

- **Chave inválida ou revogada:** confira a chave em Configurações → API e atualize a conexão no app.
- **Ative o acesso e confira advanced:read:** confira a ativação da API e a permissão de leitura da chave.
- **API indisponível/incompatível:** use o endereço da API mostrado pelo Spresenter. A rota foi identificada no pacote 0.3.50 fornecido pelo mantenedor; versões diferentes podem oferecer outro contrato.
- **Zero saídas:** confira se os destinos **Saída HTML** estão habilitados. Saídas NDI e displays físicos não entram nesse catálogo.
- **Falha de rede:** confira IP, porta, rede Wi-Fi/Ethernet e acesso do aparelho ao PC. Uma falha não apaga imediatamente os resultados anteriores.

A captura `Erro.html` recebida em 26/10/2026 confirmou `Cannot GET /output/` no PC. Esse caminho não fornece a lista nessa instalação. Não é necessário apagar o nome amigável das saídas.

O catálogo não confirma que a página será renderizada corretamente. A adaptação Chrome 44–86 da 1.4.7 continua no pacote. Enquadramento, mídia, áudio e estabilidade ainda exigem teste físico. Esta revisão não corrige o áudio associado às letras.

Veja [TVBOX.md](TVBOX.md) e [VALIDACAO_1.4.10.md](historico/VALIDACAO_1.4.10.md).
