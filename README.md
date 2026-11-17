# Zyon Launcher

Launcher desktop para **Minecraft Java 1.8.9 + Forge**, com modo offline por nick, login Microsoft opcional, instância própria e menu Zyon com cenário noturno de cerejeiras, keystrokes arrastáveis, importação opcional do OptiFine e atualizações pelo GitHub.

[Baixar a versão mais recente](https://github.com/Favelado157/zyon-launcher/releases/latest) · [Acompanhar compilações](https://github.com/Favelado157/zyon-launcher/actions) · [Guia de configuração](CONFIGURAR-PUBLICACAO.md)

## Jogar com nick offline

1. Abra **Minha conta**, digite um nick de 3 a 16 letras, números ou `_` e clique em **Usar este nick**.
2. Prepare os arquivos na aba Instalação, caso ainda não estejam prontos.
3. Use **Play offline**. Não precisa de conta Microsoft, Client ID nem aprovação para esse modo. A primeira preparação precisa de internet.

O modo offline funciona em mundos locais e servidores que aceitam jogadores sem autenticação. Servidores com autenticação exigem uma conta original e o modo Microsoft. O nick, incluindo maiúsculas/minúsculas, determina sua identidade offline; mudar o nome pode mudar inventário/progresso em servidores.

## Keystrokes e OptiFine

- **Shift direito → Editar HUD**, ou **Ctrl direito**: arraste os keystrokes. Em Interface → Keystrokes, configure tamanho, cores, transparência, CPS, mouse e pulo. Posição e estilo ficam nos perfis.
- No launcher, **Instalação → OptiFine**: baixe o JAR oficial **1.8.9 HD U M5** e use **Importar OptiFine .jar**. O JAR não é redistribuído pelo Zyon. Zoom usa **C** por padrão; a capa depende de uma capa OptiFine ativa na conta.
- Veja [LEIA-ME.md](LEIA-ME.md) para instalação e controles.

## Estado do projeto

- Identidade visual, cliente Java e instalador Windows implementados.
- Canal definido: `Favelado157/zyon-launcher`. Novas alterações enviadas à `main` disparam o workflow de publicação.
- Modo offline por nick disponível e selecionado inicialmente. A escolha e o nick são salvos neste PC.
- Client ID Microsoft do Zyon incluído para o modo Microsoft opcional. O login online próprio ainda depende da **aprovação do aplicativo para as APIs do Minecraft**; a autenticação real ainda não foi validada. O canal de atualizações funciona independentemente dessa aprovação.
- A versão 1.0 precisa ser atualizada manualmente uma vez para receber o verificador. Na 1.1 já instalada, o canal pode ser definido em Configurações como `Favelado157/zyon-launcher`.

## Instalação

Abra a página Releases, baixe o instalador `.exe` e execute-o com o Minecraft e o Zyon fechados. Mundos e configurações em AppData são preservados. A primeira release aparecerá depois que a compilação concluir.

Consulte [LEIA-ME.md](LEIA-ME.md) para controles, pastas, limitações e verificações.

## Publicação automática

O workflow gera uma versão crescente, compila o cliente Java, executa os testes, cria o instalador Windows e publica o `.exe` com `SHA256SUMS.txt`. Não distribui tokens de acesso aos jogadores.

A variável de repositório `MICROSOFT_CLIENT_ID` pode substituir o ID público incluído em `release-config.json`. Sem a variável, a compilação usa o ID do projeto. Incluir esse ID não substitui a aprovação Microsoft/Minecraft.

## Desenvolvimento

Requisitos: Node.js 24; JDK 21 para Gradle e JDK 8 para compilar o cliente.

```powershell
npm ci
npm test
npm run dist:win
```

As fontes Java estão em `client-src`; os arquivos distribuídos com o launcher estão em `payload`. Para configurar os identificadores públicos de publicação, veja [CONFIGURAR-PUBLICACAO.md](CONFIGURAR-PUBLICACAO.md).

O projeto não é um produto oficial da Microsoft, Mojang ou LabyMod.
