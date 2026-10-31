# Interface do app e Central de Retornos — 1.5.2

A consulta de ocorrências e métricas fica na Central de Retornos do Spresenter. A TV/box conserva os controles de fonte, exibição, configuração e pareamento.

- Retirados **Diagnóstico do aparelho** e **Relatar problema** das configurações do monitor.
- Retirados o atalho de relatório e os detalhes técnicos da recuperação de abertura.
- A recuperação oferece **Abrir apenas HTML**, **Tentar abrir o monitor novamente** e **Configurações do Android**.
- Os registros internos, as métricas e o serviço local autenticado foram preservados. Os registros anteriores continuam consultáveis pelo painel após o pareamento.
- A remoção das telas não ativa envio remoto: o APK entregue tem destino de suporte vazio.

O fonte completo da 1.5.1 foi recuperado e recebeu esta revisão, com a Central preservada. O app é 1.5.2 e o plugin corrigido é 0.1.2. O pacote completo contém instaladores, fonte correspondente e os recursos necessários à recompilação Java/interface. Veja [Compilar](COMPILAR_1.5.2.md) e [Validação](historico/VALIDACAO_1.5.2.md).

## Conexão e disponibilidade

Desligar TVs e boxes depois dos cultos permanece neutro: ausência de resposta aparece como **Sem comunicação**, sem criar uma falha. Aguardar uma fonte HTML/NDI também é um estado de disponibilidade. Falhas confirmadas continuam no histórico da Central.

A troca de rede inicia uma busca pública finita, sem remover PCs antigos nem mudar a fonte escolhida. Abrir a lista de fontes também procura novos endereços; a atualização explícita consulta os catálogos autorizados. O serviço da Central já reavalia seu endereço local a cada cinco segundos.

O teste físico continua pendente: parear com a Central 0.1.2, verificar métricas, trocar a rede sem desvincular os PCs, selecionar a nova fonte e repetir o ciclo de desligar e ligar da igreja.
