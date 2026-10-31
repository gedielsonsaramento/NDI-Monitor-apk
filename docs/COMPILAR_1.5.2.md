# Compilar o S-Retorno 1.5.2

O pacote completo contém os fontes Android e do plugin, o APK compilado e as quatro bibliotecas nativas ARM32/ARM64 usadas na 1.5.1 e mantidas na 1.5.2. O instalador do SDK NDI e chaves privadas de assinatura não acompanham a entrega.

## Android

Use JDK 17 e o SDK Android com plataforma 36. O projeto usa Gradle 8.13 e Android Gradle Plugin 8.13.2. O Android Studio pode instalar os componentes necessários.

Abra a pasta que contém `settings.gradle`. Na exportação completa, a pasta `local-native/` já contém as bibliotecas para recompilar a parte Java/interface sem reconstruir o receptor nativo.

No Windows:

```powershell
.\gradlew.bat :app:assembleTvboxTest -PmonitorPrebuiltNativeDir=../local-native
```

No Linux/macOS:

```sh
./gradlew :app:assembleTvboxTest -PmonitorPrebuiltNativeDir=../local-native
```

Saída: `app/build/outputs/apk/tvboxTest/app-tvboxTest.apk`.

Se obteve apenas o fonte pelo GitHub, copie o APK 1.5.1 ou 1.5.2 para um local conhecido e prepare as bibliotecas:

```sh
python3 tools/extrair_nativas_apk.py /caminho/S-Retorno-1.5.2.apk
```

Para um fonte obtido somente pelo GitHub, use preferencialmente o APK **1.5.2**, que inclui o perfil HTML. O APK 1.5.1 não continha esse perfil; nesse caso, prepare-o com `tools/html-compat/prepare.cjs` e uma captura compatível. O empacotamento para TV verifica a presença e os hashes dos arquivos de compatibilidade.

O extrator confere o SHA-256 de cada biblioteca antes de gravá-la. Ele também copia os avisos de licença e, quando presente, o perfil HTML com conferência de seus hashes. O reaproveitamento nativo é destinado à compilação de desenvolvimento, não ao build `release`.

O APK pronto desta entrega mantém a assinatura de atualização das edições TVBox anteriores. Uma recompilação em outro ambiente precisa usar a chave original para atualizar essas instalações. As chaves privadas são mantidas separadamente do projeto.

Para alterar o código C++/JNI ou gerar `release`, instale seu SDK NDI Android e siga o [guia de desenvolvimento](DESENVOLVIMENTO.md#compilar), usando o fluxo de SDK/CMake em vez de `monitorPrebuiltNativeDir`.

## Central

O plugin corrigido é a versão 0.1.2. O pacote inclui seu ZIP instalável. Para testar e empacotar os fontes:

```sh
cd central-retornos
npm test
python3 scripts/package.py
```

A busca de vários catálogos desta revisão acontece no app. A Central mantém o catálogo configurado no PC e também pode mostrar as fontes que o app disponibiliza. O compartilhamento remoto de suporte permanece desativado no APK fornecido.
