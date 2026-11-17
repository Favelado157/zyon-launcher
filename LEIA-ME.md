# Zyon Launcher 1.2 — Windows 10/11 x64

Esta atualização adiciona **modo offline por nick**, sem exigir conta Microsoft. O menu noturno, os keystrokes arrastáveis e a importação opcional do OptiFine continuam disponíveis.

## Atualizar

1. Feche o Minecraft e aceite a atualização na aba **Atualizações** do Zyon, ou baixe o instalador mais recente em https://github.com/Favelado157/zyon-launcher/releases/latest.
2. Abra o launcher e clique em **Preparar Minecraft / Verificar arquivos** para instalar o cliente novo.
3. Mundos e perfis são mantidos. Os JARs anteriores do Zyon 1.0, 1.1 e 1.2.0 são movidos para `zyon-backups`, evitando duas versões do mod.

## Jogar sem login Microsoft

1. Abra **Minha conta** e digite seu nick: de 3 a 16 letras, números ou `_`.
2. Clique em **Usar este nick**. O modo offline fica selecionado e é salvo para as próximas aberturas.
3. Prepare a instalação se necessário e use **Play offline**.

O modo offline não chama os serviços Microsoft/Xbox e não exige Client ID nem aprovação. A primeira preparação dos arquivos precisa de internet. Depois, com todos os arquivos prontos, mundos locais podem ser abertos sem autenticação.

Servidores com autenticação continuam exigindo uma conta original. O nick é a identidade offline, diferenciando maiúsculas/minúsculas; mudar o nome pode mudar seu inventário/progresso em servidores. Skins e capas dependem dos serviços externos e não são garantidas pelo modo offline.

Para usar a conta original, selecione **Usar modo Microsoft** em Minha conta e conecte a conta. O Client ID público do Zyon está incluído, mas esse modo ainda depende da aprovação das APIs Minecraft e de uma conta com acesso ao Java. Se o login Microsoft falhar, o launcher não inicia automaticamente com uma identidade offline.

## Keystrokes

- **Shift direito → Editar HUD**, ou **Ctrl direito**: abra o editor e arraste o painel com o botão esquerdo do mouse. Solte para salvar.
- **Interface → Keystrokes**: ative/desative o módulo, ajuste tamanho, cor ao pressionar, transparência, botões do mouse, CPS e barra de pulo.
- As teclas mostradas acompanham seus atalhos em **Opções → Controles**. O CPS conta cliques físicos do mouse durante o jogo.
- A posição é salva como porcentagem da área disponível, acompanha a resolução e fica nos perfis do Zyon.
- **Restaurar posição** devolve o painel à posição inicial. O editor mostra uma prévia mesmo quando o módulo está desligado.
- **End** desliga todos os módulos, incluindo os keystrokes.

## OptiFine: zoom e capa

1. No launcher, abra **Instalação → OptiFine 1.8.9 → Baixar no site oficial**.
2. Baixe o arquivo original **OptiFine 1.8.9 HD U M5**, compatível com Forge 2318. Não execute nem extraia o JAR.
3. Com o jogo fechado, clique em **Importar OptiFine .jar** e selecione esse arquivo.
4. Abra o Minecraft. O zoom usa **C** por padrão; altere em **Opções → Controles**.
5. A capa exige uma capa OptiFine ativa associada ao nome do jogador. Use **Gerenciar minha capa OptiFine** e habilite **Mostrar capas** nas opções de vídeo e **Capa** na personalização da skin.

O instalador do Zyon não distribui o JAR OptiFine. O autor exige autorização para redistribuição: https://optifine.net/copyright. Informações de capas: https://optifine.net/faq.

A importação verifica o conteúdo do JAR e a versão, evita outro OptiFine nas pastas `mods` e `mods/1.8.9`, e copia somente para a instância escolhida. **Remover da instância** move o arquivo importado para `zyon-backups`; não remove instalações manuais nem arquivos alterados depois da importação. Importar novamente não exige repetir o login Microsoft.

## Pastas e atualização

- Aplicativo: `%LOCALAPPDATA%\Programs\Zyon Launcher`.
- Instância: `%APPDATA%\ZyonLauncher\instances\1.8.9`, ou a pasta que você já escolheu.
- Perfis e layout: `config/zyon` dentro da instância.
- Registros: `launcher.log` e `game.log` em `%APPDATA%\ZyonLauncher`; `logs/latest.log` dentro da instância.

O canal `Favelado157/zyon-launcher` verifica releases ao abrir e periodicamente. O download do instalador é verificado por SHA-256. Alterações enviadas à branch `main` geram uma nova compilação e release; alterações salvas somente no PC não são publicadas.

## Verificação e limites

O projeto inclui testes de instalação, preservação de arquivos, autenticação simulada, atualizações, importação OptiFine, limites do editor, resolução e persistência dos perfis. A importação também foi conferida localmente com o JAR oficial M5, sem incluí-lo nas fontes ou no instalador. O mod compila para Java 8 / Forge 1.8.9.

O login Microsoft real, o instalador no Windows, o início do jogo offline e o zoom/capa dentro do jogo precisam ser confirmados no PC do jogador. O modo offline foi conferido por testes de sessão, argumentos Java e fluxo desktop com processo de jogo simulado. O código não cria uma capa OptiFine nem concede aprovação Microsoft. O executável não possui assinatura digital de editor.

As fontes do cliente estão em `client-src`; leia `CONFIGURAR-PUBLICACAO.md` para publicar. O GitHub Actions compila o mod, atualiza o manifesto, testa e gera o instalador Windows automaticamente.

O Zyon é independente, sem vínculo oficial com Microsoft, Mojang, OptiFine ou LabyMod.
