# Zyon Visual — Zyon Launcher

Cliente independente para **Minecraft Java 1.8.9 / Forge**, com tema azul neon, catálogo de mods e **20 recursos visuais locais**, incluindo widgets arrastáveis, ajustes de cores e animações. Requer conta Microsoft com acesso ao Minecraft Java, verificado nos serviços oficiais antes de preparar e a cada abertura.

**Autorização pendente:** o pedido anterior de acesso às APIs foi recusado. Esta edição foi preparada para uma nova análise; não foi aprovada pela Microsoft/Mojang. Até os serviços autorizarem o aplicativo e validarem a conta, preparar e jogar ficam bloqueados. Incluir um Client ID ou publicar um instalador não concede autorização. O login real ainda precisa ser validado após essa autorização.

[Baixar instalador Windows](https://github.com/Favelado157/zyon-launcher/releases/latest) · [Compilações](https://github.com/Favelado157/zyon-launcher/actions) · [Guia](LEIA-ME.md) · [Informações para revisão](docs/API-review.md)

## Recursos desta edição

| Recurso | Comportamento |
| --- | --- |
| HUD | Marca própria, FPS, coordenadas do jogador e recursos visuais ativos. |
| Keystrokes | Teclas, cliques físicos e CPS; tamanho, cores, opacidade e posição editáveis. |
| Mira | Tamanho, espaço central e ponto sobre a mira padrão. |
| Zoom | Segure **C** para ampliar a imagem; fator configurável. |
| Tema | Menu azul neon, cores, opacidade, perfis e cenário noturno com cerejeiras. |

**Shift direito** abre o painel; **Ctrl direito** abre o editor dos keystrokes. Passe o mouse sobre **?** para ler a ajuda de cada recurso. **End** oculta os extras.

## Acesso e atualização

O navegador abre o login oficial Microsoft por código de dispositivo. O Zyon consulta os serviços Xbox/Minecraft e verifica a propriedade e o perfil antes de preparar ou iniciar. Não há entrada só por nick, fallback sem autenticação ou importação de contas de outros launchers. O cache de autenticação usa o armazenamento criptografado do Windows.

A versão 1.5 adiciona Fullbright, Item Physics, Nametag, PvP Clássico, FPS, Vida, Mão principal, Peitoral, Botas e Contorno de blocos aos sete recursos anteriores. Consulte o [guia do novo HUD](docs/hud-studio.md) para controles e limitações. O motor antigo de combate, movimento automatizado e visualização de entidades foi removido. Não há importação de OptiFine, capas de terceiros, automação de jogo ou carregamento de mods externos nesta edição.

Atualizações são oferecidas ao abrir e periodicamente pelo canal público `Favelado157/zyon-launcher`. A pessoa escolhe quando instalar; o instalador é conferido pelo SHA-256 publicado.

## Migração

A nova instância fica em `%APPDATA%\ZyonLauncher\instances\zyon-visual-1.8.9`. A instalação anterior, seus mundos e arquivos ficam na pasta original. O botão **Abrir pasta da instalação anterior** permite localizá-la. Somente campos de perfis visuais compatíveis são copiados, sem copiar os mods antigos. Pastas com mods externos são recusadas sem apagar esses arquivos.

Versões antigas no histórico Git/Releases pertencem à edição anterior e **não representam esta edição visual**. Para avaliação, use a fonte e o instalador da série 1.5. As demais branches guardam ferramentas separadas da comunidade; não acompanham o instalador.

## Desenvolvimento e publicação

Use Node.js 24, JDK 21 para o Gradle e JDK 8 para o cliente. Execute `npm ci`, compile em `client-src` com `gradlew build`, copie o JAR sem sufixos para `payload` e execute `npm run prepare:release`, `npm test` e `npm run dist:win`.

Cada envio à `main` dispara a compilação Windows, os testes e a publicação do instalador com `SHA256SUMS.txt`. Veja [CONFIGURAR-PUBLICACAO.md](CONFIGURAR-PUBLICACAO.md). Nunca use IDs emprestados, senhas ou tokens de jogadores na configuração de publicação.

Aplicativo independente de terceiros. **NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.**

## Insígnia e nick em terceira pessoa

Em Shift direito → HUD, use **Insígnia Zyon** e **Meu nick no F5**. O emblema aparece antes do próprio nick no TAB e no nome acima da própria cabeça, nas duas câmeras de terceira pessoa. Ajuste o tamanho do nick no painel. F1, agachar, invisibilidade, dormir e espectador ocultam o nome. O desenho respeita a profundidade dos blocos. O nome e as cores enviados pelo servidor são preservados.

Esta identificação é local: outros clientes não recebem o emblema, e o mod não marca desconhecidos como usuários Zyon. A descoberta e o compartilhamento público de usuários ainda não estão disponíveis. O instalador aceita os hashes conhecidos do cliente oficial anterior e guarda o JAR antigo em zyon-backups, sem trocar a pasta de mundos.

A atualização 1.5.1 adiciona Capacete, Calça e um painel de autotexto acionado pelo usuário. Vida passa a aparecer sobre os jogadores, sem widget arrastável. Consulte o guia HUD Studio para os dados disponíveis em servidores e os controles.
