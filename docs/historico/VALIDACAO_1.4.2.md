# Validação da revisão de teste 1.4.2

Verificações em 21/10/2026, com SDK NDI para Android 6.3.2.0, JDK 17, API 36, AGP 8.13.2, Gradle 8.13, NDK 28.2.13676358 e CMake 3.22.1. Pacote br.com.polegar.ndimonitor, versionName 1.4.2, versionCode 7, ARM32 e ARM64.

## Problemas relatados e escopo

O teste do usuário mostrou um retorno HTML acessível por endereço manual, com cortes e diferenças em relação ao navegador do computador, mas sem descoberta pela lista. A configuração apresentada depois mostra uma saída Stage de 1920 x 1080 e o caminho /output/retono. A versão anterior procurava somente /output/saida-html-1 e não seguia a mudança de caminho para /slide/.

Esta revisão trata esses dois pontos na descoberta e acrescenta exibição de monitor 16:9. O destino do navegador foi observado em captura; a resposta HTTP do servidor real não foi capturada neste ambiente. O teste de redirecionamento foi feito com servidor local simulado, não com o PC do usuário.

## Resultados automatizados

- 69 testes Java/Robolectric aprovados, sem falhas, erros ou testes ignorados.
- Os 19 testes do probe cobrem HTML, nomes alternativos, /output/retono, redirecionamento relativo e absoluto para Stage, normalização de barra, limites de redirecionamento, cancelamento, leitura limitada e conteúdo inválido.
- Verificado que hosts sem conexão recebem apenas uma tentativa de conexão por alvo; nomes alternativos são usados somente depois de HTTP 404/410. Não visita outro host, login ou controle, e não transfere parâmetros privados de uma fonte salva para outra rota.
- Testes da preferência de exibição confirmam troca entre monitor e página original, restauração da escolha, preservação da URL e ausência de uma nova busca ao abrir apenas o menu.
- Quatro testes gráficos do enquadramento confirmam ajuste proporcional e quatro bordas visíveis em tela de 1536 x 709 e outros formatos. A prévia usa uma View de desenho; Robolectric substitui o motor Chromium e não renderiza o HTML real do tema.
- O contexto do WebView usa densidade 1x e texto em 100% no modo monitor, mantendo densidade e fonte normais dos controles do aplicativo.
- Lint com zero erros e os três avisos anteriores: DiscouragedApi, DataExtractionRules e MergeRootFrame. O componente de enquadramento é criado programaticamente com seu renderizador e não é inflado de XML; isso está indicado na supressão específica ViewConstructor.
- Compilação limpa de teste, lint e APK concluída. Um cache incremental de dex foi limpo após erro de classe de recurso duplicada; a compilação limpa passou.

## Pacote e assinatura

- APK verificado por apksigner; zipalign com páginas de 16 KB aprovado.
- Certificado SHA-256: 00ea89d2a8aa31439cb4dd592dd535826bc8e8692ec4825fd4647f944543d1c1.
- Mesmo certificado dos APKs de teste 1.4.0 e 1.4.1 entregues: permite atualizar essas instalações sem desinstalar, respeitadas as regras normais do Android.
- O APK 1.1.0 antigo enviado pelo usuário usa outro certificado. Atualização sobre aquela instalação exige a chave anterior; desinstalar apaga as preferências.
- As quatro bibliotecas nativas e os avisos do SDK são idênticos aos da 1.4.1. Os segmentos LOAD das duas bibliotecas ARM64 têm alinhamento de 16384 bytes.
- SHA-256 do APK: 2449ee838f926a27a613dc4055061940865df32a26821c747d7d220aa9c086c8.
- Tamanho do APK: 11,128,579 bytes.
- Nenhum novo AAB foi gerado; nenhum pacote foi enviado à Play ou publicado como release.

## Comandos de verificação

```sh
./gradlew :app:clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
./gradlew :app:lintDebug :app:assembleDebug
```

## Validação que falta no aparelho

1. Atualizar sobre a 1.4.1, abrir HTML e solicitar atualizar para procurar a saída retono sem digitar seu IP.
2. Abrir a fonte encontrada e conferir visitantes, Harpa, oportunidade, relógio, imagens e mídia no modo Monitor 16:9.
3. Comparar a opção de página original, voltar ao modo monitor e reiniciar para conferir a preferência e a URL salvas.
4. Confirmar que selecionar uma fonte encerra a busca e que abrir apenas o menu não inicia outra.
5. Testar interrupção de rede, retomada, NDI com áudio/vídeo, controle remoto, Home/boot e uso prolongado.

Não houve acesso à rede 192.168.2.x nem teste desta revisão em hardware real. Não foram medidos consumo de CPU, memória ou desempenho do WebView no celular/TV Box. O enquadramento não reescreve o CSS do tema; cortes internos definidos pela página continuam exigindo avaliação do conteúdo de origem.

Consulte DOCUMENTACAO_COMPLETA.md para o manual integral de uso, rede, arquitetura, compilação e assinatura.
