# Identidade visual — S-Retorno

Nome de exibição do aplicativo: **S-Retorno**. Descrição curta: **Monitor de palco**.

O símbolo tem duas faixas dobradas apontando para a esquerda, representando retorno,
e uma base de monitor. A referência visual é o desenho angular do S-Present; os
vetores deste diretório foram desenhados para este aplicativo. Não incluem o PNG
nem outros arquivos gráficos extraídos do S-Present.

- Fundo: `#12204B`.
- Faixa principal: `#FFFFFF`.
- Faixa secundária: `#5C89FF`.
- Base: `#B9D5FF`.
- [Ícone em SVG](s-retorno-icon.svg).
- [Banner Android TV em SVG](s-retorno-banner.svg).
- [Imagem de apresentação do repositório em PNG, 1280 × 640](s-retorno-social.png).
- [Imagem de apresentação editável em SVG](s-retorno-social.svg).
- [Ícone em PNG, 512 × 512](s-retorno-icon.png).

O nome é o mesmo nos recursos principal e TVBox. O pacote de atualização continua
`br.com.polegar.ndimonitor.tvboxtest` por compatibilidade com instalações existentes;
isso não aparece no nome do aplicativo. O nome de versão é `1.4.11`, sem sufixo.

## Recursos Android

O manifesto aponta para `@mipmap/ic_launcher` e `@mipmap/ic_launcher_round`.
Android 6/7 usa os vetores tradicionais; Android 8+ usa os recursos adaptativos.
Android 13+ também recebe o desenho monocromático para launchers que suportam
ícones temáticos. As partes visíveis cabem na área central segura do adaptativo.
O banner usa texto convertido em curvas para não depender de fontes do firmware.
Os arquivos SVG e VectorDrawable compartilham as mesmas geometrias e cores.

Os contornos de letras do banner foram gerados com DejaVu Sans Bold. A licença
correspondente está em [LICENSE-DejaVu.txt](LICENSE-DejaVu.txt). Não há arquivo
de fonte embutido no APK.
