# Zyon Launcher / Zyon Client 2.0

Launcher Windows x64 para Minecraft Java 1.8.9, com inicialização independente, entrada por nick offline, HUD editável, aba Xit e suporte a capas públicas OptiFine.

## Uso

Instale o EXE de [Releases](https://github.com/Favelado157/zyon-launcher/releases/latest), abra **Zyon Launcher**, salve o nick em **Perfil offline** e pressione **Jogar offline**. A primeira preparação baixa Java e componentes oficiais verificados. O client não carrega Forge, FML ou Launchwrapper. Servidores que exigem autenticação online continuam exigindo uma conta válida.

**Shift direito** abre o painel; **Recursos** mantém o HUD atual; **Xit** agrupa as 15 funções da primeira edição. Os módulos Xit começam desligados e podem ser configurados, favoritados e vinculados a uma tecla. O botão **?** explica cada função. **Ctrl direito** abre o editor de widgets, **C** controla o zoom e **End** desliga os recursos.

**Capa OptiFine** vem ativada em Recursos → Visual: consulta a imagem pública do nick em `https://s.optifine.net/capes/<nick>.png`, carrega em segundo plano e guarda cache local. Se HTTPS estiver indisponível, usa a rota HTTP pública de compatibilidade do OptiFine, sem enviar credenciais. Não inclui OptiFine nem compra/concede capas. Na ausência de uma capa pública, preserva a capa padrão. A aparência é exibida por clientes com suporte a esse serviço.

## Atualização e arquivos

O app usa `%APPDATA%/ZyonLauncher`. A atualização preserva a pasta de jogo configurada, mundos, texturas e configurações; bibliotecas na pasta `mods` deixam de ser carregadas. Os arquivos preparados da edição Forge não são aceitos como runtime independente. O instalador preserva AppData na desinstalação. Sem credenciais, tokens ou login Microsoft nesta edição.

## Compilar

Requer Node.js 24 e um **JDK 8** (`JAVA_HOME` ou `ZYON_JAVA8`).

```sh
npm ci
npm run test:java
npm test
npm run dist:win
node build/prepare-release.cjs --checksums
```

`build/client.cjs` baixa dependências com hashes verificados, cria o runtime vanilla derivado apenas em `client-src/build`, compila os hooks próprios e gera o SDK em `payload`. **Não distribua** `client-src/build/vanilla-client.jar` nem o arquivo original Minecraft. O SDK contém somente código Zyon, ASM, mapeamentos simbólicos e recursos próprios.

Ao publicar `main`, o workflow Windows compila e testa tudo, gera a versão `2.0.<número da execução>`, constrói o instalador e publica uma release com SHA-256. O launcher consulta esse canal e pede ao usuário para instalar as atualizações.

Aplicativo independente de terceiros, sem afiliação à Mojang/Microsoft. Consulte `build/release-notes.md` para as mudanças e limitações.
