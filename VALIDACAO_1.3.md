# Validação da revisão 1.3.0

Data: 21/10/2026. Base: revisão 1.2.0, commit
`bf0d9686f539ec7e39fcde6265c9f7aea690f366`.

## Resultado

- APK debug 1.3.0, `versionCode 4`, pacote `br.com.polegar.ndimonitor`.
- Android mínimo API 24, compilação e destino API 36, JDK 17.
- ARM32 e ARM64 presentes no pacote.
- Compilação Java/recursos, 22 testes e lint concluídos com sucesso.
- Lint: zero erros e três avisos (`DiscouragedApi`, `DataExtractionRules`,
  `MergeRootFrame`).
- Verificação de assinatura APK e `zipalign -c -P 16 4` aprovadas.
- Teste nativo de regressão da descoberta aprovado.

| Conjunto | Testes | Cobertura principal |
| --- | ---: | --- |
| Catálogo NDI | 7 | Atualização incremental e preservação das fontes |
| Layout | 4 | Celular baixo, fonte grande, tela grande e formulário HTML |
| Endereços HTML | 4 | IP, porta/caminho, parâmetros, IPv6 e entradas inválidas |
| Renderizador HTML | 4 | Mídia, navegação web, ciclo de vida, falha parcial e tempo limite |
| Troca de modo | 3 | Persistência, endereço inválido, callbacks NDI tardios e controle remoto |

As prévias de layout foram renderizadas em Android simulado e inspecionadas.
O celular de 640×320 conserva três fontes NDI visíveis; no modo HTML, o campo
de endereço e o botão de abertura ficam acessíveis. O teste de controle remoto
confere a abertura do painel mesmo quando o conteúdo web recebe foco.

## Bibliotecas nativas

Esta alteração modifica Java, recursos e configuração de acesso HTTP. As
bibliotecas `libndimonitor.so` e `libndi.so` ARM32/ARM64, assim como os avisos de
terceiros, foram reaproveitadas do APK 1.2.0 e comparadas byte a byte após o
empacotamento. Não houve alteração do código C++ nem da interface JNI.

Para gerar este APK, a etapa CMake foi temporariamente omitida e as bibliotecas
já compiladas foram empacotadas sem remoção de símbolos. A configuração Gradle
original foi restaurada no código-fonte, que continua compilando o C++ com o
NDK e o SDK local conforme o guia do projeto. Bibliotecas, SDK e chaves não são
incluídos nesta revisão do repositório.

## Testes no equipamento final

Ainda devem ser verificados na rede e nos aparelhos do usuário:

- Abertura do destino real `/output/saida-html-1` e aplicação do tema Stage.
- Atualização das letras, relógio, visitantes e avisos do retorno.
- Reprodução de vídeos e de áudio, quando fornecidos pela página HTML.
- Interrupção e recuperação do PC/rede, inclusive conexões internas da página.
- Trocas NDI/HTML com mídia em reprodução, retomada do aplicativo e operação
  prolongada em celular, TV Box e TV Android.

Uma página aberta não comprova que sua conexão interna está atualizando o
retorno. A verificação de alinhamento não substitui execução em um aparelho com
páginas de memória de 16 KB.

## Arquivo de teste

`Monitor-de-Retorno-1.3.0-teste.apk` — 11.085.189 bytes.

SHA-256:
`36cad7e53af544570c0127e9763492128ba621ccc1b247864dbf5ebe75f74b4e`

A assinatura debug difere do APK 1.2.0 entregue anteriormente. Para atualizar
uma instalação existente sem removê-la, compile o novo código usando a mesma
chave daquela instalação. A remoção do aplicativo apaga suas configurações locais.
