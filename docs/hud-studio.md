# HUD Studio — cliente 1.5.1

O menu aberto com **Shift direito** segue a organização da referência enviada: catálogo com busca, favoritos, grade de duas colunas ou lista, páginas individuais de ajustes, prévias e editor de widgets. A arte e a marca são do Zyon.

## Telas incluídas

- **Aparência:** temas Zyon, Glass e Vanilla; cantos, paleta, destaque HEX, ícones, sombra da fonte, transição, sons, opacidade, moldura, fundo e botões. O botão de skin abre as opções de camadas da skin do Minecraft. Perfis permite salvar e carregar configurações locais.
- **Fullbright:** brilho local e atalho configurável. O brilho original é restaurado entre quadros, ao desativar e ao sair do mundo.
- **Item Physics:** apresentação dos itens caídos e velocidade de rotação. Não altera colisões ou física do servidor.
- **Nametag:** ocultar nomes e mostrar o próprio nome; funciona junto de Meu nick no F5 e Insígnia Zyon.
- **PvP Clássico:** transformações visuais inspiradas na 1.7 para posição dos itens, bloqueio, comida, vara e equipar; inventário centralizado com efeitos. Não automatiza combate, não altera a duração dos ataques e não representa uma reprodução exata de outro cliente.
- **Keystrokes:** texto, fundo, cor pressionada, opacidade, contorno, cantos, raio, tamanho, CPS, mouse e pulo. O editor de layout permite mover, adicionar, remover e trocar até oito teclas.
- **FPS:** taxa real, cores separadas, formatação, altura e escala.
- **Vida:** valor, percentual ou absorção acima da cabeça dos jogadores. Ícone, aparência hardcore e escala editáveis; deixou de ser um widget arrastável. No próprio personagem aparece em F5 com Meu nick no F5 ativo. Respeita distância de 32 blocos, F1, nomes ocultos, invisibilidade, agachamento, regras de equipes e a profundidade dos blocos. Alguns servidores não disponibilizam a vida real de outros jogadores; o Zyon não tenta descobrir esses dados por outros meios.
- **Mão principal, Capacete, Peitoral, Calça e Botas:** itens realmente equipados, durabilidade em barra, percentual ou valor, com cor fixa ou gradiente.
- **Contorno de blocos:** contorno e preenchimento do bloco selecionado pela mira, com profundidade normal.

Mira, Zoom, HUD Zyon, Insígnia Zyon e Meu nick no F5 permanecem disponíveis. São **20 módulos locais**, dos quais sete são widgets independentes: Keystrokes, FPS, Mão principal, Capacete, Peitoral, Calça e Botas. O autotexto fica disponível no chat por padrão; os novos equipamentos ficam desligados até serem ativados; preferências antigas são preservadas.

## Usar

1. Abra o jogo e pressione Shift direito.
2. Clique no cartão para abrir os ajustes; use o interruptor para ativar.
3. Passe o mouse no `?` para ver a descrição. Estrela adiciona aos favoritos.
4. Clique em uma cor para abrir a paleta. Digite `#RRGGBB` e confirme com Enter; Esc cancela.
5. Arraste uma barra ou clique no valor para digitar um número.
6. Em um widget, use **Posicionar widget**, **Mover** ou **Remover**. **Ctrl direito** abre o editor geral; arraste e conclua para salvar.
7. Em Keystrokes, abra **Editar layout das teclas**. Selecione e arraste; um segundo clique rápido permite trocar a tecla. **Restaurar** recupera o layout padrão.

Configuração automática: `config/zyon/auto.json`, no diretório desta instalação. Perfis mantêm cores, favoritos, atalhos, teclas e posições.

## Adaptações da referência

Bandeiras compartilhadas e estado de transmissão exigem serviços de perfis/plataformas que ainda não existem no Zyon. A interface informa essa indisponibilidade. Não inventa países ou transmissões de outros jogadores.

O indicador de recarga de ataque não existe no Minecraft 1.8.9; a roda de gestos também não foi incluída. O desfoque é aplicado ao menu de mods, em mundos carregados, quando o OpenGL permite, o framebuffer está ativado e não há outro shader ativo. O Zyon não substitui um shader instalado por outro mod. Menus de inventário e pausa não recebem desfoque nesta versão.

As prévias são ilustrativas, com a skin real do jogador nas páginas de personagem e itens de exemplo nas páginas de equipamento. O HUD em jogo usa os dados reais. Os três temas mudam a apresentação deste menu, não todas as interfaces de outros mods.

Os renderizadores de itens/animações respeitam renderizadores externos já registrados. Quando houver conflito, o renderizador existente é preservado; teste essa combinação separadamente. LabyMod e OptiFine não são dependências deste JAR.

## Instalação e atualização

O instalador publica o mod 1.5.1 e preserva o diretório da edição visual, os mundos e as preferências. JARs conhecidos anteriores são guardados em `zyon-backups`. Arquivos modificados ou mods externos continuam bloqueados pelo launcher e são preservados.

Para usar pelo launcher oficial, crie uma instalação Forge **1.8.9 / 11.15.1.2318**, com Java 8, e coloque somente `zyon-1.8.9-1.5.1.jar` como mod Zyon na pasta `mods`. Retire versões anteriores ou experimentais do Zyon dessa pasta antes. O acesso pelo launcher Zyon continua sujeito à autorização das APIs Microsoft/Minecraft e à propriedade da conta. Esta atualização não resolve um HTTP 403 de autorização.

## Autotexto no chat

Abra o chat com T e clique em **AT**, no canto inferior direito, acima da linha de digitação. Edite o nome e o conteúdo de até seis mensagens. **Salvar** e Enter apenas salvam; **Enviar** manda uma única mensagem de até 100 caracteres por clique e fecha o painel. Mensagens vazias não são enviadas. Comandos escritos pelo usuário também podem ser salvos. Não há envio periódico ou repetição automática.

Ao voltar, o texto que estava sendo digitado no chat continua no campo. O painel também pode ser aberto por Shift direito → Autotexto → Editar mensagens. O interruptor Autotexto controla a exibição do botão AT. Mensagens são locais deste PC, em `config/zyon/chat/autotext.json`, e não são publicadas no GitHub.
