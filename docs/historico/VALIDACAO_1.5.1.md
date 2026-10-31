# S-Retorno 1.5.1 + Central de Retornos 0.1.1

Revisão de 30/10/2026 para a rotina diária da igreja: TVs e boxes são desligadas após os cultos e religadas nos próximos usos.

## Comportamento

- Falta de resposta fica como **Sem comunicação**, um estado neutro; não gera relatório de falha nem entra no contador de falhas ativas.
- Aguardando fonte HTML/NDI é disponibilidade, não uma falha confirmada do aplicativo. O contador continua mostrando problemas reais de autenticação/configuração e motor indisponível.
- Marcadores de sessão incompleta não produzem relatórios automáticos. Entradas antigas de interrupção ou ausência de sinal são excluídas do histórico apresentado; interrupções não confirmadas pendentes do suporte remoto são descartadas antes de enviar.
- Falhas Java confirmadas, motivos de saída confirmados pelo Android e relatos manuais permanecem disponíveis.
- A fonte salva e o vínculo protegido continuam persistidos. O plugin retoma as consultas ao retorno após a religação e reinicia os gráficos ao detectar nova execução ou intervalo sem comunicação.
- Corrigida a expressão de normalização de nomes, preservando letras e acentos.
- A assinatura das versões anteriores e os binários NDI da 1.5.0 foram mantidos. O envio remoto continua desativado.

## Verificações

- **245 testes Android aprovados**, sem falhas ou erros; incluem 40 ciclos simulados sem encerramento normal, preservação de identidade/credencial/nome, exclusão de relatos não confirmados e conservação de falhas confirmadas.
- **11 testes do plugin aprovados**, incluindo 12 ciclos simulados de indisponibilidade/retorno sem erro, identidade preservada, ausência de novo pareamento, gráficos renovados e manutenção de erros reais de autorização.
- Análise Android sem erros e APK compilado.
- Integração da interface com servidor Java real da API local e reprodução simulada: pareamento, catálogo, comandos, responsividade e reconexão. O contador de falhas permaneceu zerado durante a indisponibilidade simulada.

Os ciclos são testes de software. Ainda é necessário conferir esta revisão no S-Present real e nas boxes, inclusive desligamento físico, início automático e reprodução das fontes salvas.
