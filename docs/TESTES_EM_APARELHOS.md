# Testes em aparelhos — relato do mantenedor

Atualizado em **29/10/2026**, na publicação do **S-Retorno 1.4.11 (16)**.

## Resultado informado

O mantenedor informou que **todas as boxes que testou rodaram perfeitamente e a TV Philips com Android também**. Este é o resultado dos testes físicos realizados por ele nos equipamentos do projeto.

| Grupo de equipamentos | Resultado relatado em 29/10/2026 |
| --- | --- |
| Todas as boxes testadas pelo mantenedor | Funcionamento perfeito |
| TV Philips com Android | Funcionamento perfeito |

Os diagnósticos anteriores das boxes incluem BTV B9 com Android 6.0.1 / API 23, aparelhos H6+/HTV-6H com Android 7.1.2 / API 25 e AB E10 com Android 9 / API 28. O relato final não traz uma nova ficha por aparelho nem discrimina a versão instalada, o modo e cada função exercitada.

A marca **Philips** segue a informação atual do mantenedor; o modelo exato e a versão Android dessa TV não foram informados nesse relato. As fotos antigas identificadas como Philco/Skyworth permanecem como histórico em [TVBOX.md](TVBOX.md), sem presumir que sejam a mesma TV.

## Alcance do registro

O relato confirma o resultado geral nos aparelhos testados. Ele não é uma matriz de certificação de todo hardware Android e não detalha resultados separados de áudio associado a letras, saída simultânea HDMI/AV, reconexão ou abertura automática ao ligar.

Os requisitos técnicos continuam: **Android 6 utiliza HTML; NDI requer Android 7 / API 24 ou superior**, ARM compatível e carregamento correto do runtime. O resultado da B9 não deve ser interpretado como recepção NDI no Android 6.

As verificações de desenvolvimento da versão estão em [VALIDACAO_1.4.11.md](historico/VALIDACAO_1.4.11.md): 194 testes existentes aprovados, lint sem erros, compilação e assinatura/integridade do APK conferidas. Essas verificações foram realizadas separadamente dos testes físicos relatados pelo mantenedor.

[Baixar aplicativo e código-fonte](DOWNLOADS.md).
