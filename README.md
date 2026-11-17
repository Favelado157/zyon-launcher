# Zyon Launcher

Launcher desktop para **Minecraft Java 1.8.9 + Forge**, com instância própria, novo menu do jogo, ícones Zyon e um canal de atualizações.

[Baixar a versão mais recente](https://github.com/Favelado157/zyon-launcher/releases/latest) · [Acompanhar compilações](https://github.com/Favelado157/zyon-launcher/actions) · [Guia de configuração](CONFIGURAR-PUBLICACAO.md)

## Estado do projeto

- Identidade visual, cliente Java e instalador Windows implementados.
- Canal definido: `Favelado157/zyon-launcher`. Novas alterações enviadas à `main` disparam o workflow de publicação.
- O login online próprio ainda depende do **Client ID Microsoft aprovado para Minecraft**. O canal de atualizações funciona independentemente desse cadastro.
- A versão 1.0 precisa ser atualizada manualmente uma vez para receber o verificador. Na 1.1 já instalada, o canal pode ser definido em Configurações como `Favelado157/zyon-launcher`.

## Instalação

Abra a página Releases, baixe o instalador `.exe` e execute-o com o Minecraft e o Zyon fechados. Mundos e configurações em AppData são preservados. A primeira release aparecerá depois que a compilação concluir.

Consulte [LEIA-ME.md](LEIA-ME.md) para controles, pastas, limitações e verificações.

## Publicação automática

O workflow gera uma versão crescente, compila o cliente Java, executa os testes, cria o instalador Windows e publica o `.exe` com `SHA256SUMS.txt`. Não distribui tokens de acesso aos jogadores.

A variável de repositório `MICROSOFT_CLIENT_ID` é opcional para publicar atualizações visuais. Sem um ID aprovado, o aplicativo deixa explícito que o login online próprio ainda não está ativado.

## Desenvolvimento

Requisitos: Node.js 24; JDK 21 para Gradle e JDK 8 para compilar o cliente.

```powershell
npm ci
npm test
npm run dist:win
```

As fontes Java estão em `client-src`; os arquivos distribuídos com o launcher estão em `payload`. Para configurar os identificadores públicos de publicação, veja [CONFIGURAR-PUBLICACAO.md](CONFIGURAR-PUBLICACAO.md).

O projeto não é um produto oficial da Microsoft, Mojang ou LabyMod.
