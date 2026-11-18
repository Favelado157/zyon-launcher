# Zyon Discord v2 — recriar o servidor

[Baixar Template-Zyon-Recriar-v2.zip](https://github.com/Favelado157/zyon-launcher/raw/refs/heads/discord-server-template/Template-Zyon-Recriar-v2.zip)

**6 categorias · 27 canais · 10 cargos em azul neon**

Comece Aqui, Launcher, Comunidade, Suporte, Salas de Voz e Equipe privada. Inclui os oito emojis Zyon já criados e os textos iniciais.

## Recriar

Extraia o ZIP, leia **LEIA-ME.txt**, confira **ESTRUTURA.md** e abra **RECRIAR-SERVIDOR.bat**. Requer Python 3.11 ou mais recente (recomendado 3.12) e um bot que você controla. ID do servidor e token são digitados somente no seu PC; o token não é salvo.

O aplicador mostra o nome, o ID e a lista exata do que será apagado. Para autorizar, digite **APAGAR seguido do ID do servidor**. Depois ele exclui os canais/categorias antigos e os cargos gerenciáveis antes de criar a nova estrutura.

**Apagar canais remove permanentemente suas mensagens, anexos e tópicos. Apagar cargos remove suas atribuições aos membros.** A referência estrutural salva no PC contém nomes/IDs/permissões, sem mensagens ou restauração do histórico.

São preservados o servidor, membros, emojis, integrações, `@everyone`, cargos gerenciados pelo Discord e cargos atribuídos ao próprio bot aplicador. Mova o cargo do bot acima dos cargos antigos que ele deve apagar. Comunidade precisa estar desativada para permitir a remoção de seus canais obrigatórios; essa condição é conferida antes de excluir itens.

## Retomar

Mantenha o arquivo `progresso-<ID>.json` na mesma pasta. Uma limpeza interrompida retoma apenas os IDs do plano original. Depois de encerrada a limpeza, a retomada cria os itens faltantes sem apagar a estrutura nova.

Depois de uma recriação concluída, executar o modo de recriação prepara uma nova limpeza e exige outra confirmação. Para somente criar itens faltantes ou gerar/sincronizar o link nativo, use **APLICAR-SEM-APAGAR.bat**.

Os cargos são atribuídos manualmente. Administrador e Moderador possuem permissões de moderação. A estrutura não inclui bots de tickets, música ou logs automáticos. Envie os PNGs da pasta `emojis` manualmente pelo Discord.

O link nativo copia a estrutura e cargos do servidor; mensagens, membros, bots e emojis devem ser configurados separadamente.

## Versão anterior

[Pacote original, que preserva os canais e cargos existentes](https://github.com/Favelado157/zyon-launcher/raw/refs/heads/discord-server-template/Template-Zyon-Discord.zip). Esse ZIP anterior permanece sem alterações.

## Validação

Testes com API simulada: confirmação exata, exclusão por IDs, proteção de cargos essenciais, hierarquia, Comunidade/canais privados, interrupção e retomada, cancelamento e continuação da montagem sem repetir a limpeza. A aplicação real acontece somente ao executar o pacote no seu PC e confirmar o servidor escolhido.
