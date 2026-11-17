# Zyon Launcher 1.1.0 — Windows 10/11 x64

Esta versão atualiza o fundo do launcher, o ícone do aplicativo, o menu inicial do Minecraft e a marca no HUD. Também implementa início direto pelo Java, login Microsoft próprio e avisos de novas versões publicadas no GitHub.

**Estado da entrega:** o visual e o instalador estão disponíveis. O código do Play direto e do atualizador está implementado, mas o login online e o canal global **ainda dependem dos cadastros do publicador**. As fontes estão configuradas para `Favelado157/zyon-launcher`. O Client ID ainda está vazio. O canal pode publicar atualizações visuais sem aguardar o cadastro Microsoft, mas o envio ao GitHub ainda depende do acesso à conta. Leia `CONFIGURAR-PUBLICACAO.md` para concluir.

## Instalar ou atualizar a 1.0

1. Feche o Minecraft e o Zyon Launcher.
2. Execute **Zyon-Launcher-Setup-1.1.0.exe** e mantenha a pasta de instalação anterior.
3. Abra o Zyon e clique em **Preparar Minecraft**. O Zyon prepara sua própria pasta de bibliotecas, recursos e componentes Windows.
4. Com o aplicativo Microsoft registrado e aprovado, entre em **Minha conta** e conecte sua conta pelo código exibido. A senha é informada na página Microsoft.
5. Use **Play**. O processo iniciado é o Java com Minecraft/Forge; o launcher oficial não é aberto nesse fluxo.

Quem está na 1.0 precisa instalar a nova versão uma vez. A antiga não possui um agente que receba esse aviso retroativamente.

A pasta de mundos e as configurações anteriores são preservadas. Ao preparar, o JAR Zyon 1.0 é movido para `zyon-backups` na instância para não carregar duas versões. Os outros mods não são removidos.

**Enquanto o cadastro Microsoft não estiver concluído**, o Play online direto ainda não funciona. Há uma opção explícita em Configurações → Compatibilidade para abrir a instalação 1.0 já preparada pelo launcher oficial anterior. Ela não é o novo Play direto e não ativa o login próprio.

## Visual no jogo

- Fundo com portal de ametista, ilhas suspensas e cachoeiras no menu inicial.
- Ícone Zyon na janela do Minecraft e no HUD.
- Menu inicial com Um jogador, Multijogador, Opções, Menu Zyon, Mods e Sair.
- **Shift direito:** menu do cliente. **End:** desliga os módulos.
- Interface → Tema permite desativar o menu inicial Zyon; Interface → HUD permite ocultar o ícone.

Os 18 módulos continuam disponíveis. O KillAura exige espada ou machado por padrão. O servidor decide quais ações e movimentos aceita.

## Conta e sessão

O Zyon usa código de dispositivo Microsoft, APIs Xbox e Minecraft. Verifica o acesso ao Java e o perfil do jogador. Não reutiliza tokens de outros launchers.

O cache Microsoft é cifrado pelo armazenamento protegido do Windows (Electron safeStorage/DPAPI). Se esse armazenamento estiver indisponível, o login não salva tokens em texto simples. Sair da conta remove o cache do aplicativo configurado. O token Minecraft permanece em memória e é omitido dos registros capturados pelo Zyon.

Sem o Client ID do próprio publicador e a aprovação Minecraft, o aplicativo informa a pendência e não simula login concluído. Para testes locais, informe o ID público em Configurações → Configuração do publicador. Não informe senhas, tokens ou client secrets.

## Atualizações

O canal usa releases de um repositório público GitHub. O Zyon verifica ao abrir e periodicamente enquanto estiver aberto. Uma versão estável mais nova gera aviso e opção de atualizar. O instalador é baixado, conferido por SHA-256 e iniciado após a escolha da pessoa. Feche o Minecraft antes de atualizar.

O workflow incluído pode publicar uma release nova a cada alteração enviada à branch `main`, com versão crescente. **Salvar um arquivo apenas no seu PC não publica uma atualização.** É necessário enviar a mudança ao repositório e a compilação terminar com sucesso.

O endereço do canal foi definido nas fontes: `Favelado157/zyon-launcher`. As publicações só existirão depois que o projeto for enviado e o workflow terminar. Filebin é um meio temporário de entregar o pacote; não substitui o canal fixo.

## Pastas

| Conteúdo | Padrão |
| --- | --- |
| Aplicativo | `%LOCALAPPDATA%\Programs\Zyon Launcher` |
| Mundos, mods e configurações | `%APPDATA%\ZyonLauncher\instances\1.8.9` ou a pasta anterior escolhida |
| Bibliotecas e recursos próprios | `%APPDATA%\ZyonLauncher\minecraft` |
| Java 8 e componentes Windows | `%APPDATA%\ZyonLauncher\runtime` e `natives` |
| Registros | `%APPDATA%\ZyonLauncher\launcher.log` e `game.log`; `logs/latest.log` na instância |

O Java dedicado não altera o Java do sistema. A instalação oficial anterior não é apagada. O desinstalador mantém os mundos e as configurações em AppData.

## Verificações

- 34 testes de conta, publicação independente do cadastro Microsoft, interface de criptografia do cache, autenticação simulada, comando Java, regras Windows, migração do JAR, integridade e atualizações.
- Preparação real concluída com metadados oficiais: **49 entradas Java, 722 recursos**, nativos Windows e configuração de logs, com verificação dos downloads.
- Sete testes de perfis Java passaram; o mod foi compilado e remapeado para Forge 1.8.9.
- Interface testada no Chromium em 1280×900 e 1020×740, com ponte do aplicativo e login simulados: preparação, configuração pendente, código Microsoft, Play direto, jogo aberto, aviso, erros e notas tratadas como texto.
- Instalador NSIS gerado; estrutura, ícone e metadados Windows conferidos.
- **Não realizado aqui:** executar o instalador no Windows, renderizar o novo menu no jogo e concluir login Microsoft real com aplicativo aprovado.

O instalador não tem assinatura digital de editor. Foi empacotado em Linux para Windows x64.

## Fontes

O pacote de fontes inclui o launcher, o mod em `client-src` e `.github/workflows/release.yml`. Consulte `CONFIGURAR-PUBLICACAO.md` antes de distribuir.

Com Node.js 24:

```powershell
npm ci
npm test
npm run dist:win
```

Para alterar o mod, compile `client-src` com JDK 21 para Gradle e JDK 8 como compilador; copie o JAR final para `payload` e rode `npm run prepare:release`. O workflow faz essas etapas automaticamente.

O Zyon é independente, sem vínculo oficial com Microsoft, Mojang ou LabyMod.
