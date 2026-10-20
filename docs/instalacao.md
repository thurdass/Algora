# Instalação e desenvolvimento

Use Node.js 24 e `npm ci`. O script de instalação baixa explicitamente o runtime do Electron 44. A interface inclui Monaco e seu worker no build; não precisa de CDN para carregar o editor.

## Linux

Os pacotes ficam em `release/`. Em sistemas Debian/Ubuntu, instale o `.deb` com o gerenciador do sistema, por exemplo:

```sh
sudo apt install ./release/Algora-0.1.0-amd64.deb
algora
```

O instalador registra o menu, o executável e a configuração de sandbox apropriada ao sistema. O AppImage é outra distribuição disponível, sujeita ao suporte de FUSE e namespaces do ambiente.

No host Linux usado no desenvolvimento, a execução direta do Electron extraído de `node_modules` foi recusada pelo sandbox do Chromium: namespaces de usuário estão restritos e o helper local não tem proprietário/permissões SUID de instalação. Esse é um limite observado do ambiente. O projeto não desativa o sandbox automaticamente nem altera a política do sistema. Nesse cenário, use o pacote `.deb` ou a interface no navegador:

```sh
npm run dev:web
```

A interface abre em `http://127.0.0.1:5187`. O interpretador, terminal e depurador são os mesmos. A abertura e o salvamento usam os diálogos do navegador.

## Windows

Execute `Algora Setup 0.1.0.exe`, gerado com NSIS. O instalador permite escolher o diretório. Os pacotes locais não possuem certificado de assinatura. Para desenvolver em Windows, use `npm ci` e `npm run dev`.

## Desenvolvimento desktop e testes

`npm run dev` inicia Vite e Electron, requerendo suporte ao sandbox no Linux. `npm run check` verifica tipos, testes do núcleo/CLI e build da interface. `npm run test:e2e` instala Chromium e verifica a interface no navegador. `npm run test:desktop` verifica a ponte Electron, importação Windows-1252, execução e salvamento. Em Linux sem sessão gráfica, rode esse último com `xvfb-run -a`.

O Playwright controla uma instância Electron de teste com perfil temporário próprio. Seus argumentos de automação são gerenciados pelo Playwright; o aplicativo distribuído mantém a configuração de sandbox padrão e `contextIsolation`, sem integração Node no renderer.
