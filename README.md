# S-Retorno 1.5.2

![S-Retorno — Monitor de palco para NDI e HTML](artwork/s-retorno-social.png)

Use sua TV, TV Box ou celular Android como monitor de retorno para igrejas e eventos. O S-Retorno recebe fontes **NDI** e exibe saídas **HTML do Spresenter** pela rede local.


> **Revisão para teste nos aparelhos:** app **1.5.2** com **Central de Retornos 0.1.2**. Mantém vários PCs no catálogo HTML e concentra a consulta de ocorrências e métricas no painel do Spresenter.

## Arquivos desta entrega

- **S-Retorno-1.5.2.apk**: aplicativo pronto para instalar.
- **S-Retorno-Central-0.1.2.zip**: plugin instalável no Spresenter, compatível com o app 1.5.2.
- **Fontes/S-Retorno-1.5.2/**: projeto Android e fontes do plugin no pacote completo.

Permite controlar fontes e acompanhar memória, CPU, rede e ocorrências pelo Spresenter. **Desligar as TVs e boxes após os cultos não gera relatório de erro.** Ao religar, o app restaura a fonte salva e a central retoma o vínculo. [Instalação e uso](central-retornos/README.md).

O pacote desta revisão inclui APK, plugin, fontes e instruções. Os testes automatizados e a compilação estão registrados em [Validação 1.5.2](docs/historico/VALIDACAO_1.5.2.md). O teste físico no Spresenter e nas boxes continua necessário.

## Compatibilidade

| Versão do aparelho | Modos disponíveis |
| --- | --- |
| **Android 6** | **HTML** |
| **Android 7 ou superior** | **HTML e NDI** |

Mantenha o aparelho e o computador com o Spresenter ou transmissor NDI na **mesma rede**, por Wi-Fi ou cabo.

## Como instalar

1. Extraia o pacote da revisão e localize **S-Retorno-1.5.2.apk**.
2. No celular, abra o arquivo baixado. Para TV/box, você pode copiá-lo para um pendrive e abrir pelo gerenciador de arquivos.
3. Se o Android solicitar, permita a instalação por esse navegador ou gerenciador de arquivos.
4. Toque em **Instalar** e abra **S-Retorno**.

**Para atualizar as edições TVBox anteriores, instale por cima, sem desinstalar**, para manter as fontes e configurações salvas.

## Como configurar as fontes HTML

Faça esta configuração em cada TV ou box, uma vez para cada PC com Spresenter. O app mantém até **oito computadores**, cada um com sua própria conexão:

1. No S-Retorno, selecione **HTML** e abra **engrenagem → Saídas HTML do Spresenter → Configurar pelo celular ou PC**.
2. Deixe essa tela aberta: ela mostra um **endereço** e um **código de seis dígitos**.
3. No computador com o Spresenter, abra **Editor remoto (rede)** e use **Copiar link com token**.
4. No navegador do PC ou celular da mesma rede, abra o **endereço mostrado na TV/box**. Informe o código e cole o link completo copiado do Spresenter.
5. Toque em **Enviar para esta TV**. Volte à lista do app e selecione a saída desejada.

O código vale por cinco minutos. Se expirar, abra novamente **Configurar pelo celular ou PC** para gerar outro.

Ao cadastrar outro PC, os anteriores são mantidos. Para remover somente um computador, use **Remover** ao lado do endereço nas configurações das saídas HTML.

[Passo a passo completo para encontrar o link no Spresenter](docs/SAIDAS_SPRESENTER.md).

## Como buscar novas fontes HTML

1. Crie ou habilite a nova **Saída HTML** no Spresenter.
2. No app, abra o painel de fontes, mantenha **HTML** selecionado e toque em **↻ no topo do painel**.
3. Aguarde a lista atualizar e selecione a fonte que deseja exibir.

Com os computadores cadastrados, a busca consulta os catálogos de todos eles e atualiza seus nomes. Se mudar também o endereço de uma saída, selecione a nova entrada na lista.

Para procurar endereços padrão em outra faixa local, abra **engrenagem → Saídas HTML do Spresenter → Buscar em outra faixa de IP** e informe, por exemplo, **192.168.2**. A rede precisa permitir a comunicação com essa faixa. Para nomes personalizados e todas as saídas de um PC, cadastre seu link do Editor remoto.

O app salva a fonte escolhida e a abre novamente na próxima inicialização. Para abrir um link HTML diretamente, use **Informar endereço** e cole o endereço completo da saída.

## Como usar NDI

Em aparelhos com **Android 7 ou superior**, deixe o transmissor NDI ativo no computador, selecione **NDI** no app, toque em **↻** para procurar fontes e escolha a desejada.

## Ocorrências e métricas na Central

Consulte as métricas e ocorrências na **Central de Retornos** do Spresenter. Os atalhos de diagnóstico e relatório saíram da interface da TV/box. O registro interno continua disponível para o painel após o pareamento.

A recuperação de abertura mostra as ações de tentar novamente, abrir HTML e acessar as configurações do Android. [Escopo da interface](docs/INTERFACE_CENTRAL.md).

## Testes e atualizações semanais

**Realizamos testes e disponibilizamos novas atualizações toda semana** para melhorar o funcionamento, a compatibilidade e a estabilidade do aplicativo.

Nos testes relatados pelo mantenedor, todas as boxes utilizadas e a TV Philips com Android funcionaram corretamente. [Ver o registro dos testes](docs/TESTES_EM_APARELHOS.md).

Se encontrar uma dificuldade, consulte a ocorrência no painel e [relate o problema aqui](https://github.com/gedielsonsaramento/S-Retorno.apk/issues), indicando a versão e o que aconteceu.

<details>
<summary>Código-fonte e documentação técnica</summary>

- O código-fonte da 1.5.2 acompanha o pacote completo, incluindo o módulo `central-retornos/`.
- [Recompilar usando as bibliotecas do APK](docs/COMPILAR_1.5.2.md)
- [Documentação completa e instruções de compilação](docs/README.md)
- [Detalhes do download e integridade do APK](docs/DOWNLOADS.md)
- [Histórico de alterações](docs/CHANGELOG.md)
- [Registros de validação](docs/historico/README.md)

</details>

Projeto independente. Código próprio sob [licença MIT](LICENSE). NDI® é marca registrada da Vizrt NDI AB; os componentes NDI têm [termos e avisos próprios](docs/LICENCA_NDI_IMPORTANTE.md).
