# Validação — Monitor de Retorno 1.4.9-TVBox

Candidata de 26/10/2026, `versionCode 14`, baseada na 1.4.8 (`ebe863aa480e1fdf26818dfb21df052957f7f7f9`). Pacote `br.com.polegar.ndimonitor.tvboxtest`.

**Falha confirmada após a entrega:** o formulário da 1.4.9 envia `Referrer-Policy: no-referrer`, levando o navegador a enviar Origin null no POST. A validação estrita do app recusa o envio, mesmo com IP/porta corretos. Os testes abaixo não exercitavam o envio num navegador real. A correção e a reprodução em Chromium estão na [1.4.10](VALIDACAO_1.4.10.md); use essa revisão para configurar a box pelo navegador.

## Problema e mudança

O usuário consegue copiar o link do Editor remoto do S-Present, mas só tem o controle da TV Box e não consegue digitar a chave longa. A revisão acrescenta **Configurar pelo celular ou PC**: endereço temporário da box e código de seis dígitos, formulário acessível pelo navegador da mesma rede e envio do link completo. A TV salva a conexão pela proteção já existente, inicia a busca e mantém a escolha de saída explícita.

O link também pode ser colado diretamente no Monitor de um celular. Endereço/chave continuam em **Configuração manual**. O importador valida a URL e extrai origem/token localmente; não abre `/warp/`, não aciona controles e não coloca o token em URLs de reprodução.

As capturas confirmaram **Editor remoto (rede)** com **Publicar na rede** ativado no PC. A interface de criação manual de chaves do S-Present 0.3.50 omite `advanced:read`; as instruções anteriores da 1.4.8 foram corrigidas. O token do editor inclui leitura e escrita do mapeamento. O Monitor continua executando somente o GET de catálogo e não habilita a API ou o editor por conta própria.

## Duração e limites da configuração

- Só abre por solicitação, em um IPv4 atribuído a Wi-Fi/Ethernet; rejeita VPN, rede móvel e endereços não elegíveis. Não escuta todas as interfaces e não abre no boot/reprodução.
- Até cinco minutos, um envio válido e cinco códigos incorretos. Fecha ao cancelar, sair do app ou destruir a Activity; callbacks de uma sessão anterior não alteram a seguinte.
- HTTP na LAN, sem nuvem. Credenciais recebidas no corpo de POST, sem CORS ou redirecionamento. Confere Host/Origin e código; bloqueia cabeçalhos/valores duplicados, framing chunked e envio por GET.
- Cabeçalho e corpo até 8 KiB; até três segundos por pedido. A resposta não reflete chave/link/código, não inclui recursos externos e usa no-store, CSP, proteção de frame e referrer restrito.
- Armazenamento AES/GCM/AndroidKeyStore ou, se o firmware não permitir, somente sessão com aviso. A chave não é registrada em relatórios. Permissões Android não foram ampliadas.

## Verificações executadas

- **194 testes Android**, zero falhas, erros ou ignorados na execução final. Incluem os 146 anteriores e 48 cenários novos.
- Importação: link completo, origem normalizada, token escapado, rejeição de outras rotas/protocolos, userinfo, fragmentos, chaves duplicadas/inválidas e erros sem eco da credencial.
- Servidor HTTP real em loopback: formulário sem código exposto, envio correto e único, código incorreto/limite de tentativas, correção de link inválido, separação de sessões, Host/Origin, ausência de configuração por GET, duplicidades, framing, tamanho, expiração sem tráfego e cancelamento de cliente incompleto. Não representa um teste entre os aparelhos físicos.
- **30 cenários novos de interface** nas APIs **23, 25, 28, 34 e 35**: colagem do link, foco para o controle, erro local, recebimento durante a tela ativa, preservação da fonte, busca após salvar, fechamento/reabertura, callbacks antigos, saída do app e ausência de LAN.
- Seleção de endereço simulada nas APIs 23/25: Wi-Fi com dados móveis como padrão, preferência por Ethernet ativa, exclusão de VPN, loopback, IPv4 público e IPv6.
- Durante a implementação, os testes detectaram foco em modo de toque e fechamento tardio do diálogo anterior. A correção foi seguida da execução final completa acima.
- Lint: **zero erros**, três avisos anteriores (`DiscouragedApi`, `DataExtractionRules`, `MergeRootFrame`). Montagem `assembleTvboxTest` concluída.
- Manifest do APK: `1.4.9-tvbox`/14, mínimo API 23, destino API 36, ARM32/ARM64 e `StartupActivity`. Classes de configuração presentes no DEX.
- CRC, assinaturas v1/v2 e certificado TVBox preservado: `6007ea0469cbe58451d689913869fcfd617e6e686967ed8a1e113091eac51421`.
- `zipalign -c -P 16 4` aprovado; não comprova execução em hardware com páginas de 16 KiB.
- Quatro bibliotecas nativas, avisos NDI e perfil HTML ES5/Chrome 44–86 idênticos byte a byte à 1.4.8. Não houve mudança de áudio, NDI ou adaptação HTML nesta revisão.

APK: **11.377.263 bytes**. SHA-256: `272f1b497a997f424963aa4902d5a186982cf3905e59df254e1d09b624ff96b9`.

## Teste físico pendente

Instale sobre a edição TVBox anterior sem desinstalar. Na box, abra **HTML → engrenagem → Saídas HTML do S-Present → Configurar pelo celular ou PC**. No navegador do PC do S-Present, abra o endereço mostrado na TV, digite o código curto e cole o link copiado do editor. Selecione uma saída na TV após buscar. O PC/celular precisa alcançar a box pela rede local; redes de convidados ou isolamento entre clientes podem bloquear esse acesso.

Não houve acesso à LAN do usuário, às boxes, ao navegador do PC ou à API ativa com chave real. Os testes simulados/loopback não comprovam o comportamento do navegador físico, do AndroidKeyStore do firmware, o enquadramento ou a reprodução de mídia/áudio. O problema de áudio associado às letras continua pendente. Nenhum release, merge ou envio à Play foi realizado.

Veja [passo a passo](../SAIDAS_SPRESENTER.md), [TVBOX.md](../TVBOX.md) e o histórico do catálogo na [1.4.8](VALIDACAO_1.4.8.md).
