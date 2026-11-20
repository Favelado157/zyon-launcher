# Edição visual 1.3 — informações para nova análise

**Estado:** pedido anterior recusado; nova edição preparada para reavaliação, sem aprovação ou garantia de aprovação. O login Microsoft real e a abertura autenticada ainda precisam ser validados após autorização oficial. A publicação por si só não modifica as permissões do App ID.

## Escopo atual

Cliente independente de terceiros para Minecraft Java 1.8.9, com sete recursos: HUD com informações do próprio jogador, keystrokes com contagem de cliques físicos, mira, zoom local por tecla, tema, insígnia local e nome próprio em terceira pessoa. Não promete aumento de FPS; oferece ajuste de memória e orienta opções de vídeo do jogo.

O código antigo de combate, ataques automáticos, movimentação automatizada, visualização de entidades e demais automações foi removido fisicamente do cliente. A classe Engine não integra o novo JAR. Não há importação OptiFine, capas de terceiros, mod loader aberto para extensões na instância visual ou escolha de executável externo pela interface.

## Cadeia de autenticação e propriedade

`MicrosoftAccount.signIn/getSession` usa MSAL, login por código de dispositivo no navegador oficial e o aplicativo do publicador. Microsoft > Xbox Live > XSTS > Minecraft login_with_xbox > entitlements/mcstore > minecraft/profile. Só os direitos game_minecraft ou product_minecraft, acompanhados de perfil válido, são aceitos.

Preparar e jogar exigem `sessions.resolveSession`. Um token em cache é revalidado em entitlements e profile a cada operação. Falhas de rede, recusa HTTP 403, licença ausente, perfil inválido, token expirado ou cancelamento impedem a operação. Preparações longas são seguidas de nova verificação antes de iniciar o Java. `game.launchArguments` também rejeita sessões sem propriedade verificada recentemente. Não há fallback por nick, IDs emprestados nem importação de tokens de outro launcher.

O cache MSAL fica criptografado por safeStorage no Windows; account.json contém somente dados públicos de perfil e Client ID. A senha é digitada somente na Microsoft. Logs do processo Java ocultam tokens. Sair remove o cache local desse aplicativo.

## Migração e histórico

A edição visual utiliza uma pasta de jogo nova e recusa mods externos sem apagá-los. A edição antiga e os mundos ficam preservados para o usuário. A migração copia somente campos conhecidos de HUD, keystrokes, mira e tema; módulos antigos são descartados da cópia.

Histórico Git e releases anteriores documentam uma edição diferente com capacidades incompatíveis com a candidatura atual. Não são apresentados como aprovados ou como parte do instalador atual. As branches de ferramentas Discord/servidor da comunidade não integram o produto desta candidatura. Informe esse histórico com transparência e o escopo exato que estiver submetendo.

## Evidência e limites

Os testes automatizados verificam entitlement desconhecido/ausente, revogação de propriedade com token em cache, rejeição de sessões sem validação recente, bloqueio das rotas desktop de preparação e abertura, migração sem tocar nos mundos e rejeição de mods externos. O teste do payload confere o hash, a versão e a ausência do motor anterior no JAR distribuído. Os testes Java verificam exatamente sete registros, zoom, layout, ajuda e perfis.

Validação desta edição: 44 testes Node e 22 Java passaram. A interface desktop foi conferida em seis telas, a 1280 e 1020 pixels, com erros de autorização/licença simulados e sem erros JavaScript. A captura `launcher-visual.png` usa estado de demonstração sem conta conectada; não representa login real.

Esses testes usam fixtures e respostas simuladas; não representam autenticação real nem autorização da Microsoft. Não há modo de teste, conta fictícia ou bypass distribuído ao jogador. O instalador público não abre o jogo enquanto o serviço oficial negar o acesso.

## Identidade e recursos

Nome e ícone próprios Zyon; aplicativo de terceiros. Não usa logotipo oficial do Minecraft como marca do aplicativo nem afirma endosso. O cenário de menu foi fornecido pelo publicador; antes de submeter, ele deve confirmar os direitos de uso de todas as artes e descrever o aplicativo e contatos corretamente. Essa verificação de direitos não foi feita automaticamente.

**NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.**

## Identificação visual local — cliente 1.3.1

Inclui insígnia gráfica opcional antes do próprio nome no TAB e Meu nick no F5, para mostrar o próprio nome formatado em terceira pessoa. Não modifica identidade, equipes, cargos, pacotes ou nomes de outros jogadores. Não há descoberta ou compartilhamento público de perfis. Não inclui os cosméticos experimentais 1.4.
