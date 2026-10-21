# Instalação e desenvolvimento

O Algora usa Java 21, JavaFX 21, ANTLR4 e RichTextFX. O build utiliza Gradle Wrapper com Kotlin DSL; não é necessário instalar Gradle globalmente. A primeira execução precisa de rede para baixar a distribuição e as dependências. Um JDK completo, com `jpackage`, é necessário para gerar pacotes.

## Desenvolvimento

No Linux:

```sh
./gradlew test
./gradlew :desktop:run
./gradlew :cli:run --args='examples/ola.alg'
```

No Windows, use os mesmos argumentos com `gradlew.bat`, por exemplo:

```powershell
.\gradlew.bat test
.\gradlew.bat :desktop:run
.\gradlew.bat :cli:run --args="examples/ola.alg"
```

O desktop requer sessão gráfica e as bibliotecas nativas necessárias ao JavaFX. Em um servidor Linux sem tela, um display virtual pode ser necessário para testes de interface. O núcleo e a CLI não dependem de uma sessão gráfica.

## Pacotes nativos

```sh
./gradlew :desktop:jpackageAppImage
./gradlew :desktop:jpackageInstaller
```

O primeiro comando usa o tipo `app-image` do `jpackage`: uma pasta executável com runtime Java incluído. Esse nome não designa o formato de arquivo `.AppImage` do Linux. O segundo gera um instalador nativo para o sistema do build. Não há compilação cruzada de instaladores pelo `jpackage`: gere Linux em Linux e Windows em Windows.

A tarefa Linux gera `.deb` e requer `dpkg-deb` e `fakeroot`. A tarefa Windows gera `.exe` e requer WiX compatível com o JDK 21. Os pacotes precisam ser testados em cada sistema; a compilação Java por si só não verifica diálogos nativos, fontes, atalhos ou instalação.

A aplicação fica em `desktop/build/jpackage/image/Algora/`; no Linux, o executável é `desktop/build/jpackage/image/Algora/bin/Algora`. Instaladores ficam em `desktop/build/jpackage/installer/`. A distribuição inclui runtime; o usuário final não precisa instalar um JDK separadamente.

## Verificação dos pacotes

O build, os testes JavaFX e a execução da aplicação nativa com runtime incluído passaram em Linux e Windows. Consulte o [registro de verificação](verificacao.md) para o commit validado, resultados e limites desses testes.

## Ícone do aplicativo

Os builds usam o emblema da logo oficial: PNG no Linux e ICO com múltiplas resoluções no Windows. O `jpackage` recebe o ícone tanto na aplicação quanto no instalador; a janela JavaFX também carrega o PNG. Os recursos ficam em `desktop/src/main/resources/io/algora/desktop/icons/`.

Se você instalou um pacote anterior com o ícone padrão do Java, baixe o instalador atualizado da release e reinstale o aplicativo. Atalhos antigos criados manualmente podem precisar ser recriados; o ambiente desktop também pode manter o ícone antigo em cache até uma nova sessão.

### Ícone no dock Linux

O atalho distribuído no `.deb` declara `StartupWMClass=io.algora.desktop.AlgoraApplication`, correspondente à classe nativa da janela JavaFX. Isso permite associar a janela aberta ao ícone do menu, conforme a [especificação Desktop Entry](https://specifications.freedesktop.org/desktop-entry/latest/recognized-keys.html). Após atualizar, feche o aplicativo e abra-o pelo menu. Se ele estiver fixado no dock por um atalho antigo, remova essa fixação e fixe o aplicativo atualizado novamente.

### Diagnóstico do instalador Windows

O CI instala o `.exe` silenciosamente, verifica o registro por usuário e os atalhos, executa a aplicação instalada e a desinstala. Os logs MSI ficam no artefato `Windows-installer-diagnostics` do workflow. Esse teste não substitui a validação dos diálogos interativos nem identifica problemas específicos de uma máquina sem a mensagem de erro correspondente.

Para gerar um log local de instalação pelo PowerShell, no diretório do download:

```powershell
.\Algora-0.2.0.exe /L*v "$env:TEMP\algora-install.log"
```
