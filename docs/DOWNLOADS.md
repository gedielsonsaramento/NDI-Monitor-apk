# Downloads — S-Retorno 1.4.12

| Download | Arquivo |
| --- | --- |
| Aplicativo para Android | [S-Retorno-1.4.12.apk](https://github.com/gedielsonsaramento/S-Retorno.apk/raw/refs/heads/downloads-1.4.12/S-Retorno-1.4.12.apk) |
| Código-fonte da versão | [S-Retorno-1.4.12-fonte.zip](https://github.com/gedielsonsaramento/S-Retorno.apk/archive/refs/heads/versao-1.4.12.zip) |
| Arquivos e checksum do aplicativo | [Ramo downloads-1.4.12](https://github.com/gedielsonsaramento/S-Retorno.apk/tree/downloads-1.4.12) |
| Navegar no código da versão | [Ramo versao-1.4.12](https://github.com/gedielsonsaramento/S-Retorno.apk/tree/versao-1.4.12) |

O GitHub gera o ZIP do código com o nome do repositório e do ramo. Ele contém o código próprio e as instruções de compilação; o APK fica separado no ramo de downloads. O SDK NDI, chaves de assinatura, credenciais e capturas privadas não fazem parte do ZIP do código.

**Novidade:** em **engrenagem → Relatar problema**, copie um relatório com o consumo do app. O servidor de suporte está em preparação; esta versão não envia métricas remotamente.

## Instalar ou atualizar

1. Baixe o APK e copie para a TV/box, por exemplo em um pendrive.
2. Abra o arquivo no aparelho e autorize a instalação por esse gerenciador, se o Android solicitar.
3. Para atualizar as edições TVBox 1.4.3–1.4.11, instale por cima, **sem desinstalar**. O pacote e o certificado foram mantidos para preservar as preferências.
4. Abra **S-Retorno** e confira **1.4.12 (17)** em **Configurações → Diagnóstico do aparelho**.

O APK mantém o identificador técnico `br.com.polegar.ndimonitor.tvboxtest`, embora o nome exibido não contenha “teste”. Instalações antigas do pacote principal, como 1.4.2, permanecem separadas. O arquivo é a compilação de desenvolvimento já entregue, assinada com o certificado de compatibilidade; não é uma publicação na Play.

Requer ARM32 ou ARM64. **Android 6 / API 23: HTML. Android 7 / API 24 ou superior: HTML e NDI**, sujeito ao runtime e ao conteúdo da fonte.

<details>
<summary>Integridade do APK e informações para desenvolvedores</summary>

## Integridade do APK

- Tamanho: **11.495.923 bytes**.
- SHA-256: `5702809576829c7d41d79ee000131bd81b77b6733ed180dd435ec46acf12efb5`.
- Certificado SHA-256: `6007ea0469cbe58451d689913869fcfd617e6e686967ed8a1e113091eac51421`.

No Windows PowerShell:

```powershell
Get-FileHash .\S-Retorno-1.4.12.apk -Algorithm SHA256
```

No Linux:

```bash
sha256sum S-Retorno-1.4.12.apk
```

O APK foi conferido na [validação 1.4.12](historico/VALIDACAO_1.4.12.md). Esta revisão acrescenta o relato local de problemas e prepara o cliente de suporte; não há servidor configurado nem envio remoto de métricas. Os testes físicos relatados para versões anteriores estão em [TESTES_EM_APARELHOS.md](TESTES_EM_APARELHOS.md); a 1.4.12 ainda precisa ser testada nos aparelhos.

## Compilar o código

Abra a raiz que contém `settings.gradle` e siga os requisitos e comandos do [README](DESENVOLVIMENTO.md#compilar) ou do [guia passo a passo](GUIA_PASSO_A_PASSO.md). Para recepção real, instale separadamente o SDK oficial NDI. A assinatura usada em uma compilação própria determina se ela pode atualizar uma instalação existente.

O código próprio está sob a [licença MIT](../LICENSE); o runtime NDI e seus avisos têm termos próprios. [Informações sobre os componentes NDI](LICENCA_NDI_IMPORTANTE.md).

</details>
