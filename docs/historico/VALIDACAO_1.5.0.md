# S-Retorno 1.5.0 + Central de Retornos 0.1.0

Primeira versão da integração local, preparada em 30/10/2026. Instala sobre a edição anterior, preservando o pacote `br.com.polegar.ndimonitor.tvboxtest` e a assinatura. A edição estável anterior permanece disponível enquanto esta integração é validada fisicamente.

## Alterações

- Pareamento local explícito por código de seis números, janela de cinco minutos, cinco tentativas e credencial aleatória por box. O acesso para ao sair do primeiro plano, retorna com o app e pode ser revogado na TV.
- Plugin com cartões de aparelhos, estado de comunicação separado de estado da reprodução, amostras e gráficos de CPU/memória/rede, resposta do app e ocorrências locais.
- Catálogo HTML configurado uma vez no PC, distribuído sem o token de API e conservado no app após reinício. Seleção remota de HTML ou NDI, identificação da tela, recarga, reconexão e limpeza de cache HTML.
- Confirmações associadas a identificadores de comando; até 32 confirmações por sessão. Comandos sem confirmação não são repetidos automaticamente pelo plugin.
- Contagem e taxa de quadros capturados, descartes e fila NDI. JNI recompilado para ARM64 e ARMv7 com NDK r28c/API 24 e alinhamento de 16 KiB; runtime `libndi.so` preservado em ambas as arquiteturas.
- Relato manual para a central local, histórico limitado a 24 ocorrências e motivos de saída confirmados quando o Android os oferece. Interrupções desconhecidas são mantidas separadas.
- Envio remoto de suporte continua desativado. Nenhuma alteração de IP, controle de outros aplicativos ou reinício completo do Android.

## Verificações automatizadas

- Suíte Android com Robolectric, incluindo sockets HTTP reais: autorização, limites, bloqueio de origens de navegador, pareamento, revogação, troca de credencial, expiração e repetição de comandos.
- Persistência protegida da credencial, remoção de identidade e ocorrências na revogação, histórico limitado, tratamento de interrupções e medição de FPS com reinício de contador.
- Catálogo restaurado após recriação da Activity, recusa de URLs de outra origem, conservação de fontes durante falta de rede e limpeza de cache preservando a configuração.
- Suíte Node do plugin: pareamento, credenciais ausentes das mensagens da interface, limites de endereços, confirmação/resultado desconhecido, identidade divergente, distribuição do catálogo sem token e busca com concorrência limitada.
- Análise Android sem erros, compilação do APK, verificação de assinatura e integridade ZIP; conferência de nome, pacote, versão, ABIs e funções de desempenho exportadas pelo runtime.
- Regressões nativas do descobridor NDI aprovadas com a implementação simulada existente.

## Integração da interface

O painel foi exercitado em navegador com o servidor Java real da API local e dados sintéticos de reprodução. Foram conferidos pareamento, catálogo sem distribuição da chave de API, exibição de relato manual, cancelamento e confirmação de limpeza, seleção NDI, layout em 1120 px e 420 px, perda de comunicação e retorno. Não houve erros de JavaScript nesse fluxo. O teste não executa o S-Present nem decodifica vídeo NDI real.

## Validação física pendente

Os testes automatizados não medem a reprodução real em uma box. Devem ser conferidos: instalação por cima da versão anterior, pareamento pelo controle, persistência após desligamento, comunicação com o plugin no S-Present real, catálogo da versão instalada, áudio/vídeo HTML e NDI, reconexão de rede, consumo durante uso prolongado e limpeza de cache fora da apresentação.

O serviço local usa HTTP em rede confiável. O tempo de resposta não é a latência de vídeo. Os valores de CPU/memória representam o processo principal, e os contadores de rede dependem do firmware. Uma página HTML carregada não confirma a atualização contínua de conteúdo. O contador NDI representa captura, não confirmação visual da tela.
