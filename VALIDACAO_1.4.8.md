# Validação — Monitor de Retorno 1.4.8-TVBox

Candidata de 26/10/2026, `versionCode 13`, baseada na 1.4.7 (`95465c0f57da6592d567e05aadec5b2999b67bfa`). Pacote `br.com.polegar.ndimonitor.tvboxtest`.

**Correção posterior da orientação de configuração:** a interface de criação manual de chaves do S-Present 0.3.50 omite `advanced:read`. A instrução de marcar essa permissão não pode ser seguida nessa interface. A revisão 1.4.9 aceita o link completo do Editor remoto e permite enviá-lo pelo celular/PC; consulte [SAIDAS_SPRESENTER.md](SAIDAS_SPRESENTER.md). Os testes abaixo continuam sendo o registro da 1.4.8 e não comprovam configuração ou funcionamento em equipamento real.

## Evidência e contrato

As fotos mostram nomes de destinos personalizados e gerados pelo S-Present, como musicos, madureira e canvas-2. A busca anterior tentava caminhos predefinidos, encerrava as alternativas no primeiro HTML do PC e usava o título genérico da página. A nova captura inclui `Erro.html`, salvo de `http://192.168.2.53:5050/output/`, com a resposta **Cannot GET /output/**. Não há catálogo nesse caminho nessa instalação.

O pacote S-Present 0.3.50 já fornecido pelo mantenedor contém o endpoint autenticado `GET /api/v1/advanced`, protegido por `advanced:read`, com resposta `{ config, virtualOutputs, displays, statuses }`. `config.outputs` contém os destinos; tipo network, enabled diferente de false, slug, name e canvasKey identificam os HTML publicados. O programa `slide/static/js/main.ff832fdd.js` desse pacote corresponde byte a byte à captura original: SHA-256 `f3a8628c9a08d8923db81be8a031b5660234a433af634011418cc7cb8732a043`.

A implementação usa esse contrato de leitura. Não modifica o S-Present. Nenhum código, arquivo asar ou configuração privada do programa foi incluído no Git ou no pacote de instalação. Versões do servidor com outro contrato podem exigir nova adaptação.

## Comportamento e limites

- Configura um PC por instalação em **engrenagem → Saídas HTML do S-Present**, com endereço da API e chave de leitura. Outros PCs mantêm a busca anterior por caminhos conhecidos.
- A consulta única retorna todos os destinos HTML habilitados desse PC. Nomes novos são lidos dos registros, sem lista fixa de slugs. A listagem válida atualiza nomes, acrescenta destinos e remove entradas anteriores ausentes/desabilitadas.
- Preserva a última URL escolhida como histórico e não altera a reprodução sem seleção explícita. Se o slug mudar, o usuário seleciona a nova entrada. Na reabertura, mantém a restauração direta da fonte, sem consulta em segundo plano.
- A chave vai somente no cabeçalho da origem configurada (protocolo/host/porta), sem redirecionamentos ou migração automática para IPs encontrados. URLs de exibição e nomes não recebem credenciais. Somente GET; nenhum controle de apresentação é chamado.
- Até 1 MiB de resposta, cinco segundos e 512 registros. A consulta integra os limites existentes de 512 alvos/12 trabalhadores e o cancelamento da busca. Prefere a interface Wi-Fi/Ethernet da sub-rede do PC. A repetição de 30 segundos continua restrita à busca solicitada com o painel aberto.
- Falhas de autenticação, acesso, formato e rede exibem mensagens locais, preservando os resultados anteriores. Um catálogo vazio válido é tratado como zero destinos.
- Credencial cifrada com AES/GCM/AndroidKeyStore, vinculada à origem como dado autenticado. Firmware sem armazenamento funcional mantém somente a sessão e avisa; nova abertura do processo exige a chave outra vez. A aplicação já desabilita backup.

## Verificações executadas

- Suíte Android completa: **146 testes**, zero falhas, erros ou ignorados. Inclui a matriz existente de APIs 23, 25, 28, 34 e 35.
- Após o ajuste final de rótulos de acessibilidade do formulário, os **15 cenários de interface** foram executados novamente nas APIs 23/25/28/34/35, todos aprovados. Lint e montagem do APK final também passaram.
- Novos testes de catálogo cobrem vários destinos do mesmo PC, slugs arbitrários/gerados, nome de canvas, destino desabilitado, duplicidade, slugs inválidos/nulos, resposta vazia/malformada/grande e atualização/remoção de nomes.
- MockWebServer: GET sem corpo no endpoint correto, Authorization só no cabeçalho, rejeição de redirecionamento para outro servidor sem enviar a chave, códigos 401/403/404, HTML em vez de JSON, limite de leitura e cancelamento antes de abrir conexão.
- Interface simulada: salvar/remover conexão, campos inválidos, preservação da fonte, atualização de nomes e rejeição de callbacks de gerações anteriores ou posteriores à seleção. Seleção do resultado salva a URL real do destino.
- Preferência de rede conferida com sub-redes Wi-Fi/Ethernet distintas. Os testes anteriores de trabalhadores/cancelamento permanecem.
- Cifra com chave AES de teste: IV diferente entre gravações, origem vinculada, detecção de adulteração, recuperação e remoção. Preferências sem chave em texto puro. Falha de armazenamento confere o modo somente de sessão. Não equivale à validação do AndroidKeyStore físico da box.
- Lint: **zero erros**, mesmos três avisos anteriores (`DiscouragedApi`, `DataExtractionRules`, `MergeRootFrame`).
- Manifest final: `1.4.8-tvbox`/13, mínimo API 23, destino API 36, ARM32/ARM64, `StartupActivity`.
- Assinaturas v1/v2 válidas; certificado TVBox preservado: `6007ea0469cbe58451d689913869fcfd617e6e686967ed8a1e113091eac51421`.
- CRC do APK e hashes conferidos. As quatro bibliotecas nativas, os avisos NDI e todos os assets do perfil HTML são idênticos byte a byte à 1.4.7 verificada. A adaptação ES5/Chrome 44–86 permanece.
- `zipalign -c -P 16 4` aprovado: valida empacotamento, sem comprovar execução em aparelho com páginas de 16 KiB.

APK: **11.376.804 bytes**. SHA-256: `ee831f8fb0c058ccfe2a3101f00eec54fc76425f3c3332c5e06e948cb3c0fb06`.

## Validação ainda necessária

Não houve acesso ao PC/LAN, à API ativa com chave real ou às boxes. Os testes de rede usam um servidor controlado com o contrato observado. É necessário habilitar a API, configurar uma chave advanced:read no próprio aparelho e testar criação/renomeação/desativação dos destinos reais conforme [SAIDAS_SPRESENTER.md](SAIDAS_SPRESENTER.md).

O catálogo não confirma renderização, enquadramento, mídia ou estabilidade na B9/HTV. Essas verificações físicas continuam conforme [TVBOX.md](TVBOX.md). NDI e áudio não foram alterados; o áudio associado a letras segue pendente. Nenhum release ou envio à Play foi feito. Capturas, asar, SDK, chaves e APKs ficam fora do Git.
