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
