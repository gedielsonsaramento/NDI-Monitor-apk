# Guia passo a passo — Windows + Android Studio

## 1. Baixe e extraia o projeto

No GitHub, use **Code > Download ZIP**. Extraia o ZIP para uma pasta curta e sem acentos, por exemplo:

```text
C:\Projetos\NDIMonitor_Android7
```

Não abra o projeto diretamente de dentro do ZIP.

## 2. Abra no Android Studio

1. Abra o Android Studio.
2. Clique em **Open**.
3. Selecione a pasta que contém `settings.gradle`.
4. Confirme **Trust Project**, se aparecer.
5. Configure um JDK 17 para o Gradle.
6. Aguarde a sincronização do Gradle.

Na primeira abertura, o Android Studio poderá solicitar estes componentes:

- Android SDK Platform 34;
- Android SDK Build-Tools;
- NDK (Side by side);
- CMake 3.22.1.

Aceite a instalação desses componentes oficiais do Android Studio.

## 3. Instale sua cópia oficial do NDI SDK

O arquivo oficial fornecido pela NDI é um instalador em shell. Ele mostra um contrato antes da extração. Execute-o você mesmo em um ambiente compatível, leia o contrato e escolha pessoalmente se aceita os termos.

No Windows, uma opção é usar WSL/Ubuntu ou Git Bash:

```bash
tar -xzf Install_NDI_SDK_v6_Android.tar.gz
chmod +x Install_NDI_SDK_v6_Android.sh
./Install_NDI_SDK_v6_Android.sh
```

Depois, localize a pasta extraída do SDK, normalmente chamada de forma semelhante a `NDI SDK for Android`.

## 4. Copie o SDK para o projeto

Abra o PowerShell dentro da pasta do projeto e execute:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\instalar_ndi.ps1 -SdkDir "C:\CAMINHO\NDI SDK for Android"
```

O script procura e copia:

- `Processing.NDI.Lib.h` e os outros headers;
- `libndi.so` ARM64;
- `libndi.so` ARM32, quando disponível no SDK.

Se sua TV Box for somente ARM64 e o SDK atual não fornecer ARM32, abra `app/build.gradle` e remova `armeabi-v7a` de `abiFilters` antes de compilar.

## 5. Confirme a integração

No Android Studio, confira se estes arquivos existem:

```text
app/src/main/cpp/include/Processing.NDI.Lib.h
app/src/main/jniLibs/arm64-v8a/libndi.so
app/src/main/jniLibs/armeabi-v7a/libndi.so
```

Depois clique em:

1. **File > Sync Project with Gradle Files**;
2. **Build > Assemble Project** (ou **Make Project**, conforme a versão do Android Studio).

Na saída do CMake, a mensagem esperada é:

```text
NDI SDK encontrado para arm64-v8a: recepcao real ativada
```

## 6. Teste na TV Box

1. Ative as opções de desenvolvedor e a depuração USB na TV Box.
2. Conecte a TV Box ao computador ou use ADB pela rede.
3. Selecione a TV Box no Android Studio.
4. Clique em **Run**.
5. Deixe a TV Box e a fonte NDI na mesma rede local.
6. De preferência, use cabo Ethernet.
7. No aplicativo, clique em **Buscar fontes** e escolha a origem.

Toque na imagem para revelar o menu e o status. Com o painel fechado, eles se ocultam depois de 4 segundos. Toque no botão de menu para abrir ou fechar o painel de fontes. No controle remoto, OK revela os controles e um novo acionamento abre o painel.

## 7. Gerar o APK

Para teste:

```text
Build > Generate App Bundles or APKs > Generate APKs
```

Em versões anteriores do Android Studio, o menu pode aparecer como **Build > Build APK(s)**. Não é necessário conectar um aparelho para gerar o APK; o botão **Run** é usado para instalar e executar em um dispositivo.

Alternativamente, execute na raiz do projeto:

```powershell
.\gradlew.bat :app:assembleDebug
```

O APK de teste será criado em:

```text
app\build\outputs\apk\debug\NDI Monitor.apk
```

É uma compilação debug para testes. Instale usando a mesma chave de assinatura para atualizar uma instalação anterior. Não publique chaves nem arquivos privados de assinatura.

## 8. Tela inicial opcional da TV Box

O painel inclui **Definir tela inicial / sair do quiosque** e **Configurações do Android**. Leia [QUIOSQUE.md](QUIOSQUE.md), anote o launcher original e teste a saída antes de definir o monitor como tela inicial. Essa função não bloqueia aplicativos e não garante melhoria de desempenho.

## Antes de distribuir

O SDK NDI não está incluído neste repositório. O APK com `libndi.so` depende das condições de distribuição aplicáveis ao SDK; leia [LICENCA_NDI_IMPORTANTE.md](LICENCA_NDI_IMPORTANTE.md). Consulte também as limitações de compatibilidade no [README](README.md), especialmente o suporte ainda não validado a páginas de 16 KB.

## Soluções rápidas

### Nenhuma fonte aparece

- confirme que a fonte NDI está ativa;
- use a mesma rede e sub-rede;
- teste com Ethernet;
- verifique se o roteador bloqueia mDNS/multicast;
- desative temporariamente o isolamento de clientes Wi-Fi;
- permita o aplicativo no firewall do computador transmissor.

### O aplicativo informa que o SDK está ausente

Os headers ou a `libndi.so` não foram copiados para as pastas esperadas. Execute novamente o script do passo 4 e leia o resumo exibido no PowerShell.

### Vídeo funciona, mas o áudio falha

Confirme que a fonte realmente envia áudio e verifique o volume de mídia da TV Box. Algumas caixas usam uma saída de áudio diferente quando o HDMI é conectado.

### A TV Box não aparece no Android Studio

Use `adb devices` para confirmar a conexão. Se aparecer `unauthorized`, aceite a autorização que surge na tela da TV Box.

### Erro `prepareKotlinBuildScriptModel` ou projeto chamado apenas `app`

Confirme se você abriu a raiz que contém `settings.gradle`, `gradlew.bat` e a pasta `app`. Abrir somente a subpasta `app` não carrega este projeto completo. Reabra a raiz e sincronize o Gradle.

### CMake continua indicando que o SDK está ausente depois da cópia

Os scripts de integração removem os caches nativos gerados. Feche o Android Studio, execute novamente o script de integração e reabra o projeto. Preserve as pastas `cpp/include` e `jniLibs`, que contêm sua cópia do SDK. Leia a saída do CMake para confirmar a ativação em cada arquitetura.
