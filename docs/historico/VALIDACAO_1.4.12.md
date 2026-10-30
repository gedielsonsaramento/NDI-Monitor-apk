# Validação — S-Retorno 1.4.12

Revisão de 30/10/2026, `versionCode 17`, baseada no código 1.4.11 e na organização dos guias publicada em `3a533ee6ee194289d3912243ab7114cc585a934c`.

## Resultado

- **223 testes Android aprovados**, zero falhas, erros ou ignorados. São 194 casos anteriores e 29 novos casos de suporte, privacidade, métricas, transporte, ciclo de vida e interface.
- Novos testes de interface nas APIs simuladas 23, 25 e 34; as configurações existentes das APIs 28 e 35 também permanecem na suíte.
- `lintDebug` e `lintTvboxTest`: zero erros, somente os três avisos anteriores (`DiscouragedApi`, `DataExtractionRules`, `MergeRootFrame`).
- `assembleTvboxTest` concluído. Nome S-Retorno, versão **1.4.12 (17)**, pacote `br.com.polegar.ndimonitor.tvboxtest`, mínimo API 23, destino API 36 e ARM32/ARM64.
- Assinaturas v1/v2, certificado de atualização, CRC e `zipalign -c -P 16 4` aprovados.
- Exatamente as mesmas quatro entradas nativas da 1.4.11, idênticas byte a byte. Avisos NDI e todos os recursos da adaptação HTML também preservados.
- Nenhuma nova permissão no APK em comparação com a 1.4.11.
- `BuildConfig.SUPPORT_ENDPOINT` vazio no APK distribuído. Nenhum domínio de teste está incluído no aplicativo.

## Comportamentos verificados

- Sem destino ou sem autorização, não inicia amostragem periódica nem requisições automáticas.
- Cópia local funciona sem gerar identificador de instalação e sem transmissão. Envio manual e compartilhamento automático são controles separados.
- JSON limita os campos da falha, exclui mensagens, nomes de arquivos, URLs, tokens e frames externos ao app. Campos desconhecidos são descartados e a impressão digital é calculada sobre dados permitidos.
- Amostras de CPU e tráfego usam diferenças entre leituras; contadores indisponíveis ou reiniciados produzem `null`.
- Fila de falhas limitada a oito eventos/32 KiB, envio limitado a 8 KiB; confirmação remove apenas o evento recebido.
- Primeiro plano, retorno ao segundo plano, revogação de autorização durante uma tentativa e espera crescente entre falhas foram exercitados sem rede real.
- Transporte simulado verifica HTTPS, certificado padrão, ausência de redirecionamento, User-Agent fixo e nenhuma credencial administrativa.
- Histórico de saída do Android é lido sem traces. Interrupção em Android antigo não se transforma em falha nativa confirmada; parada pelo usuário não vira crash.
- Exceção Java continua encaminhada ao tratamento original do Android, enfileira apenas os campos limitados e não faz rede no registrador da queda.

Os testes de rede usam destino sintético e conexão simulada. Nenhum relatório foi enviado a servidor externo durante a validação.

## APK

- Arquivo: `S-Retorno-1.4.12.apk`.
- Tamanho: **11.495.923 bytes**.
- SHA-256: `5702809576829c7d41d79ee000131bd81b77b6733ed180dd435ec46acf12efb5`.
- Certificado SHA-256: `6007ea0469cbe58451d689913869fcfd617e6e686967ed8a1e113091eac51421`.

## Limites e próxima integração

O servidor e o painel próprios ainda não existem nesta entrega. O APK permite copiar o relatório; os controles de envio remoto estão indisponíveis até compilar com um destino HTTPS válido. Consulte o [contrato do suporte](../SUPORTE_METRICAS.md).

Não houve teste físico desta versão nas boxes ou na TV do mantenedor, nem recepção de fluxo da LAN. O relato anterior de funcionamento não é uma validação física da 1.4.12. Não houve mudança nas bibliotecas de recepção ou na reprodução de áudio.

Instale por cima da versão TVBox anterior, sem desinstalar. Confira a retomada da fonte salva e abra **engrenagem → Relatar problema** para conferir e copiar as métricas. Em alguns firmwares, medidas podem aparecer como **Indisponível**.
