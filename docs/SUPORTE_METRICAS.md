# Suporte e métricas — preparação do S-Retorno 1.4.12

**Registro histórico da implementação da 1.4.12.** Na 1.5.2, os atalhos de relatório e diagnóstico saíram da interface da box. A consulta local de ocorrências e métricas acontece pela Central de Retornos 0.1.2 do Spresenter. As telas e os envios descritos abaixo pertencem à implementação anterior; o APK atual mantém o suporte remoto desativado. Veja [Interface do app e Central](INTERFACE_CENTRAL.md).

O aplicativo passa a ter **engrenagem → Relatar problema**, disponível também na recuperação de abertura. O usuário escolhe um tipo de problema e pode copiar um relatório local com versão e consumo do próprio app.

O painel e o servidor próprios ainda serão construídos. O APK desta preparação tem o endereço de suporte vazio: **não envia dados nem inicia consultas periódicas de métricas**. O botão de envio e o compartilhamento automático ficam indisponíveis até haver um destino HTTPS configurado. A cópia manual do relatório continua funcionando.

## O que poderá ser enviado

- Identificador aleatório da instalação, gerado pelo app; não é Android ID, serial ou identificador de hardware.
- Versão do aplicativo.
- Tipo de ocorrência e, em um relato manual, a categoria escolhida pelo usuário.
- PSS de memória do processo principal, uso de CPU do processo e bytes recebidos/enviados atribuídos ao app pelo Android.
- Para uma exceção Java, classe da exceção, até oito locais do código publicado do app e uma impressão digital do erro. Mensagens de exceções e nomes de arquivos não são enviados.

O payload não contém nome, e-mail, modelo do aparelho, versão Android, MAC, SSID, fontes, URLs de retorno, chaves, letras, imagens, áudio, vídeo, pacotes de rede ou logs completos. O User-Agent do suporte é fixo, sem os detalhes de aparelho presentes no User-Agent padrão do Android.

O IP de origem é tecnicamente visível ao servidor que recebe uma conexão. Para respeitar este escopo, o futuro servidor deve descartar esse dado e desabilitar logs de acesso que o persistam. Não deve guardar cabeçalhos ou informações extras do proxy junto às métricas.

## Controle pelo usuário

O compartilhamento automático começa **desativado**. Quando houver um servidor configurado, a opção visível **Compartilhar falhas e métricas automaticamente** autoriza os batimentos e o envio de falhas. Desativar interrompe novos envios automáticos e apaga a fila local e o identificador.

Um envio manual é autorizado pelo botão **Enviar relatório** e não ativa a coleta automática. Copiar um relatório apenas coloca no clipboard o tipo escolhido, a versão e as medidas exibidas, sem enviar ao servidor.

A autorização vale para o destino HTTPS configurado. Mudar o destino em outra compilação exige nova ativação; não transfere uma autorização antiga a outro servidor.

## Frequência e limites

- Somente enquanto uma tela do app estiver em primeiro plano.
- Primeira amostra automática após cerca de cinco segundos; depois, um batimento por minuto.
- Falha de comunicação usa espera crescente de 2, 4, 8 e até 15 minutos. Não há laço contínuo de tentativas.
- Até oito ocorrências pendentes, limitadas a 32 KiB no total; até 8 KiB por envio. Heartbeats não viram histórico local.
- Conexão e leitura têm limites de três segundos cada. A rede não roda na thread da interface.
- HTTPS com verificação normal de certificado. HTTP, credenciais/parâmetros na URL e redirecionamentos são recusados.
- Uma ocorrência pendente é removida após uma resposta HTTP 2xx. O servidor deve usar `event_id` para evitar duplicidade quando uma confirmação se perder.

## Falhas confirmadas e interrupções

O registrador Java mantém o tratamento normal do Android e apenas enfileira os campos permitidos. O envio ocorre em uma execução seguinte com autorização e comunicação disponíveis, não durante a queda do processo.

Android 11/API 30 ou superior pode informar o motivo da saída do processo. O app consulta apenas esse motivo, sem ler traces ou tombstones, para distinguir Java, falha nativa, ANR e encerramentos por recursos. Uma parada explícita do usuário não vira falha.

A partir da 1.5.1, um marcador de sessão incompleta **não gera relatório automático**. As TVs e boxes da igreja são desligadas diariamente, e a falta de encerramento pode representar apenas o corte de energia. Eventos `session_interrupted` pendentes de versões anteriores são descartados antes do envio. Falhas Java registradas e motivos confirmados pelo Android continuam elegíveis.

Somente saídas posteriores à autorização são elegíveis. O motivo já observado não é contado novamente. Exceções Java registradas pelo próprio app não são duplicadas pelo histórico do Android.

## Contrato para o servidor futuro

Destino de exemplo: `POST https://monitor.seudominio.com/api/v1/telemetry`.

```json
{
  "schema": 1,
  "event_id": "e645918d-a25a-4871-b4f9-a2c372f26a67",
  "installation_id": "541beb8f-a24f-466d-8857-a92b40511b2d",
  "app_version": "1.4.12",
  "kind": "heartbeat",
  "metrics": {
    "memory_pss_kib": 81920,
    "sample_window_ms": 60000,
    "cpu_percent": 12.5,
    "rx_bytes": 60000000,
    "tx_bytes": 12000
  }
}
```

`kind` atual: `heartbeat`, `manual_report`, `java_crash`, `native_crash`, `anr`, `memory_pressure`, `initialization_failure` ou `resource_limit`. O esquema ainda reconhece `session_interrupted` para compatibilidade com versões antigas, mas o app 1.5.1 não o produz como relatório automático.

Em `manual_report`, há `category`: `video`, `audio`, `connection`, `startup` ou `other`. Falhas podem incluir `failure.exception_class`, `failure.app_frames`, `failure.fingerprint` ou uma origem estática em `failure.source`. Não há campo de texto livre ou mensagem de exceção.

`metrics` pode ser `null` em uma falha anterior. Campos de medidas indisponíveis são `null`, não zero. `cpu_percent` é normalizado pelo número de processadores disponíveis e refere-se ao processo principal; não mede a carga de todos os apps da box. `rx_bytes` e `tx_bytes` são diferenças no intervalo indicado. Para obter Mbit/s: `bytes × 0,008 / sample_window_ms`. Os contadores de rede dependem do suporte do firmware.

O recebimento deve validar campos e limites, persistir o evento e responder 202 ou 204. O app não executa comandos recebidos nem consome instruções do corpo da resposta.

O endpoint de recebimento deve permitir apenas escrita, com limites globais e por instalação. O acesso ao painel e às consultas deve ter autenticação separada; uma credencial administrativa não pertence ao APK.

## Como o painel poderá contar aparelhos

Atualize o último batimento e as últimas métricas de cada `installation_id`. Conte como ativas as instalações com comunicação recente, por exemplo nos últimos três minutos. Falta de comunicação significa **sem comunicação**, não crash confirmado. Na rotina diária da igreja, desligamentos e religamentos não devem incrementar contadores de erro.

A contagem é de instalações, não um inventário de hardware. Desinstalar, apagar dados ou desativar o compartilhamento pode gerar outro identificador no futuro. Reinstalações não são vinculadas a uma identidade física.

Falhas Java/nativas e ANRs devem ser mostrados separadamente de interrupções desconhecidas e de encerramentos por recursos. O horário do servidor indica quando recebeu uma ocorrência; uma falha pendente pode ter acontecido antes. O servidor deve usar retenção curta e não guardar um histórico bruto infinito de heartbeats.

## Configurar quando o servidor estiver pronto

Compile o próximo APK com a propriedade:

```bash
./gradlew :app:assembleTvboxTest -PmonitorSupportEndpoint=https://monitor.seudominio.com/api/v1/telemetry
```

O endereço é validado pelo cliente e não é uma chave administrativa. A conexão é protegida por HTTPS; o endereço de um serviço dentro de um APK não é um segredo. A opção de envio continua dependendo da escolha visível do usuário.

As instruções de compilação e integração NDI ficam no [guia de desenvolvimento](DESENVOLVIMENTO.md#compilar).

Fontes técnicas: [TrafficStats](https://developer.android.com/reference/android/net/TrafficStats), [Debug.getPss](https://developer.android.com/reference/android/os/Debug#getPss()), [Process.getElapsedCpuTime](https://developer.android.com/reference/android/os/Process#getElapsedCpuTime()) e [ApplicationExitInfo](https://developer.android.com/reference/android/app/ApplicationExitInfo).
