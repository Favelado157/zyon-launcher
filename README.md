# Template de servidor Discord — Zyon

[Baixar o pacote para Windows](https://github.com/Favelado157/zyon-launcher/raw/refs/heads/discord-server-template/Template-Zyon-Discord.zip)

**5 categorias · 20 canais · 8 cargos · 8 emojis personalizados**

- Informações: boas-vindas, regras, anúncios, download e perguntas frequentes.
- Comunidade: chat geral, Minecraft, prints e clipes, sugestões e comandos de bot.
- Suporte: ajuda e relatos de bugs.
- Salas de voz: Geral, Minecraft, Partidas e Música.
- Equipe privada: chat staff, planejamento, registro manual e reunião de voz.

Cargos: Fundador, Administrador, Moderador, Suporte, Desenvolvedor, VIP, Membro e Bots. Os cargos são atribuídos manualmente.

## Aplicar

Extraia o ZIP, leia **LEIA-ME.txt** e confira **ESTRUTURA.md**. Abra **APLICAR-TEMPLATE.bat** no Windows com Python 3.11 ou mais recente (recomendado 3.12). As dependências são instaladas automaticamente.

O aplicador usa um bot que você cria no Discord Developer Portal. Digite o ID do seu servidor e o token do bot somente no terminal do seu PC. O token não é mostrado nem salvo. Um link de convite é exibido se o bot ainda não tiver acesso. Antes de criar recursos, o programa mostra o servidor escolhido e pede que você confirme o plano.

Ele preserva os canais e cargos existentes e guarda o progresso localmente para retomar sem duplicar itens. Os cargos de administração/moderação têm permissões de moderação; revise-os antes de atribuí-los. Não é solicitada a permissão Administrator ao bot.

## Link nativo

Após aplicar, o programa tenta criar e salvar o link nativo do template. **Esse link copia nomes e permissões de todos os canais/cargos do servidor, inclusive os preexistentes.** Para obter somente o modelo Zyon, recomendamos um servidor novo e vazio. Mensagens, membros, bots e emojis não são copiados pelo link nativo. Os textos prontos estão em **TEXTOS-PARA-COPIAR.md** e os PNGs em **emojis**.

Não acompanha bots de tickets, música, logs ou automoderação. O canal registro-manual recebe anotações da equipe.

## Validação

Testes com API simulada: criação, equipe privada, canais de leitura, retomada após interrupção, reaplicação sem duplicatas, conflitos com recursos existentes e geração/sincronização do link. A conexão real depende do seu bot e das permissões no Discord; nenhum token está incluído.
