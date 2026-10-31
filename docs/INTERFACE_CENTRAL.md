# Interface do app e Central de Retornos

Solicitação de 31/10/2026: concentrar a consulta de logs no painel do S-Present e retirar a apresentação desses registros da TV/box.

## Alteração preparada

- Retirados os botões **Diagnóstico do aparelho** e **Relatar problema** das configurações do monitor.
- Retirado o atalho de relatório da recuperação de abertura.
- Retirados os detalhes técnicos e a orientação para fotografar um relatório na recuperação.
- A recuperação oferece **Abrir apenas HTML**, **Tentar abrir o monitor novamente** e **Configurações do Android**.
- A mensagem de NDI indisponível orienta o uso de HTML, sem encaminhar a uma tela removida.

A alteração atua nas entradas e na apresentação da interface. Os registradores internos, os dados de falha e as classes que medem o consumo do app continuam disponíveis. O registro de uma interrupção é mantido mesmo após a escolha de abrir HTML.

## Base de código e entrega

O código publicado disponível é **1.4.12**, commit-base `09eb60fe77d14dea06232f157263d3cf5613995e`. O pacote entregue mais recente é **S-Retorno 1.5.1 + Central 0.1.1**, mas não inclui o projeto Android completo. A alteração está separada em uma branch e deve ser integrada aos fontes da 1.5.1 antes da próxima compilação.

Esta preparação não é um APK de atualização da 1.5.1. O número da versão e os links para os APKs já publicados permanecem associados aos respectivos binários.

## Verificação e integração

As verificações existentes de compatibilidade e recuperação foram atualizadas para conferir o menu simplificado e a preservação do registro interno, incluindo a abertura em HTML. Execute no ambiente Android de desenvolvimento:

```sh
./gradlew :app:testDebugUnitTest \
  --tests br.com.polegar.ndimonitor.TvBoxCompatibilityTest \
  --tests br.com.polegar.ndimonitor.StartupRecoveryTest
```

Nesta preparação, `git diff --check` e a leitura dos XMLs de recursos foram concluídos. A tentativa de executar os testes parou no download do Gradle, com `UnknownHostException: services.gradle.org`; os testes Android não chegaram a iniciar. A compilação e a validação em box continuam pendentes.

Antes de disponibilizar outro APK:

1. Integrar a mudança ao fonte completo da 1.5.1.
2. Conferir os controles de pareamento e de desativação da Central e a manutenção da autorização para compartilhamento, sem ativar envios remotos ao retirar telas técnicas.
3. Conferir no painel a chegada de métricas e de uma falha confirmada após remover os atalhos da box.
4. Validar a recuperação simples e a abertura com a última fonte salva.
5. Repetir o ciclo de desligar e ligar usado na igreja: ausência de resposta continua sendo **Sem comunicação**, sem erro automático.

A falha de descoberta HTML em outros IPs permanece uma pendência separada. A simplificação da interface não foi tratada como correção dessa busca.
