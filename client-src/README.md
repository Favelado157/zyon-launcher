# Zyon Client 1.2.2

Mod cliente para **Minecraft Java 1.8.9 / Forge 11.15.1.2318**. Menu escuro em português, seis cores, cor animada, busca, atalhos e perfis locais.

## Instalar no PC

1. Extraia este ZIP em uma pasta do computador.
2. Feche o Minecraft. Na instalação 1.8.9 do LabyMod 4, habilite o suporte a Forge / Forge Loader, se disponível nessa instalação.
3. Pelo launcher, abra a pasta de jogo **da instalação**. Copie `zyon-1.8.9-1.2.2.jar` para a subpasta `mods`; crie essa subpasta se não existir.
4. Inicie o jogo e entre em um mundo. **Shift direito** abre o menu. **End** desliga todos os módulos.

O JAR precisa que o Forge esteja carregado. Não é um addon nativo do LabyMod. Se sua instalação não oferecer suporte a Forge, colocar o JAR entre os addons nativos não o carregará. Uma instalação Forge 1.8.9 é o alvo direto do projeto. A compatibilidade específica com LabyMod 4 ainda não foi executada neste ambiente.

Se o menu não abrir, confira se Zyon Client aparece na lista de mods do Forge e verifique conflitos em Opções → Controles → Zyon. A pasta de uma instância pode ser diferente de `%APPDATA%\.minecraft`.

## Menu e perfis

- Clique no nome do módulo para abrir seus ajustes; use o interruptor para ativar/desativar.
- Use a roda do mouse sobre cada painel para rolar a lista ou os ajustes.
- Clique em Definir para escolher um atalho. Delete remove; Esc cancela.
- Interface → Tema configura cores, opacidade e cores animadas. O tema é aplicado diretamente.
- Interface → HUD configura FPS, coordenadas, lista de módulos, alvo e margens X/Y.
- Em Perfis, escreva um nome e clique em Salvar ou Carregar. Salvar substitui um perfil de mesmo nome. Use letras, números, _ ou -, até 32 caracteres.

As alterações são salvas em `config/zyon/auto.json`, dentro da pasta de jogo, e restauradas ao reiniciar. Na primeira abertura, somente o HUD vem ativo. A tecla End também funciona dentro do menu do Zyon. As teclas de menu e desligamento podem ser alteradas nos controles do Minecraft.

## Keystrokes arrastáveis

Abra **Shift direito → Editar HUD**, ou use **Ctrl direito**. Arraste com o mouse e solte para salvar. Em **Interface → Keystrokes**, configure tamanho, cor ao pressionar, opacidade, mouse, CPS e pulo. As teclas acompanham os controles do Minecraft; posição e estilo ficam nos perfis e se adaptam à resolução.

Para zoom e capas, instale separadamente o **OptiFine 1.8.9 HD U M5** na mesma instalação Forge 2318. No Zyon Launcher, use Instalação → OptiFine → Importar. O zoom é C por padrão e a capa depende do serviço OptiFine.

## Os 19 módulos

| Categoria | Módulos e ajustes |
| --- | --- |
| Combate | **KillAura:** alcance, CPS, FOV, categorias de entidades, invisíveis, paredes, rotação, arma e prioridade por vida/distância. Ataca um alvo por intervalo. **TriggerBot:** alvo na mira, alcance, CPS e arma. **AutoClicker:** botão esquerdo segurado, CPS e arma, sem interferir na mineração. **AimAssist:** botão esquerdo segurado, alcance, FOV, graus por tick e arma. |
| Movimento | **AutoSprint:** frente/todas as direções. **Fly:** velocidades horizontal/vertical, Espaço sobe e agachar desce. **Speed:** velocidade e bunny hop. **Step:** altura de degrau. **AutoJump:** salto ao andar no chão. **SafeWalk:** agacha perto de bordas; distância ajustável. |
| Visual | **Fullbright:** intensidade do brilho. **ESP:** caixas, categorias, distância, espessura e paredes. **Tracers:** linhas, categorias, distância e paredes. **Crosshair:** tamanho, espaço central e ponto sobre a mira padrão. |
| Jogador | **AutoTool:** seleciona a ferramenta mais rápida da hotbar; restauração de slot opcional. **AutoEat:** alimentos da hotbar, limite de fome, restaura slot e evita maçãs douradas. **FastPlace:** intervalo local de colocação, de 0 a 4 ticks. |
| Interface | **HUD:** marca, FPS, coordenadas, lista de módulos e informação do alvo. |

TriggerBot, AimAssist e AutoClicker compartilham os filtros de entidades do KillAura, que podem ser ajustados mesmo com o KillAura desligado. Há um temporizador de ataques compartilhado; KillAura tem prioridade quando encontra um alvo. AutoEat pausa o combate enquanto usa comida. Fullbright e Step restauram seus valores anteriores ao desligar.

## Comandos locais

```text
/zyon
/zyon toggle killaura
/zyon bind killaura R
/zyon bind killaura none
/zyon profile save principal
/zyon profile load principal
/zyon profile list
/zyon panic
```

Os comandos são tratados pelo Forge no cliente; Tab completa nomes de módulos.

## Código-fonte e compilação

O JAR incluído já está compilado. Para modificar o projeto, use a pasta `codigo-fonte` deste ZIP. Instale JDK 17 ou 21 para executar o Gradle e JDK 8 para compilar o mod. O wrapper fixa Gradle 8.6, Essential Loom 0.10.0.5 e adaptador Pack200 0.1.3.

```sh
# Linux/macOS
chmod +x gradlew
./gradlew build

# Windows, PowerShell
.\gradlew.bat build
```

Se o Gradle não detectar o JDK 8, informe o caminho dele:

```sh
./gradlew -Porg.gradle.java.installations.paths=/caminho/para/jdk8 build
```

A primeira compilação baixa as dependências. O JAR instalável é `build/libs/zyon-1.8.9-1.2.2.jar`. JARs com sufixo `-dev` são de desenvolvimento; `-sources` contêm fontes. Instale somente o JAR sem esses sufixos.

## Limites e validação

O servidor decide se aceita movimento, alcance, ataques e colocação de blocos. Fly, Speed, Step e alcance ampliado podem ser corrigidos ou rejeitados. Não há bypass de anticheat nem promessa de indetectabilidade.

ESP/Tracers mostram apenas entidades carregadas e enviadas pelo servidor. SafeWalk não impede todas as quedas. AutoTool usa a velocidade base da ferramenta, sem calcular encantamentos. AutoEat usa apenas a hotbar. Não há X-ray de minérios, Scaffold, ChestStealer ou módulos fora da lista acima.

Se FastPlace encontrar uma incompatibilidade, ele se desliga e registra o erro em `logs/latest.log`. Para remover o mod, feche o jogo e remova o JAR da pasta `mods`.

O projeto não inclui telemetria, serviço de rede, login próprio, injeção no processo ou atualização automática. As configurações são locais.

O arquivo `VERIFICACAO.txt` no ZIP registra as verificações desta reconstrução. Os testes de perfis verificam persistência, rejeição de arquivos inválidos sem aplicar mudanças parciais, limites e proteção contra nomes que escapem da pasta. Minecraft/LabyMod e a interface gráfica ainda não foram executados aqui.

No jogo, confira o menu em janela e tela cheia, rolagem do KillAura, sliders, atalhos, tecla End, salvar/carregar perfil, restauração de brilho/Step e troca de mundo com AutoEat/AutoTool/SafeWalk.

Referências: https://laby.net/support e https://files.minecraftforge.net/net/minecraftforge/forge/index_1.8.9.html

## Identidade visual 1.1

Menu inicial Zyon com fundo próprio, marca no HUD e ícones de janela. Os recursos ficam em `src/main/resources/assets/zyon/textures`. Interface → Tema permite alternar o menu inicial personalizado; Interface → HUD controla o ícone. A lógica dos módulos anteriores foi preservada.

## Menu azul neon e ajuda

Shift direito abre o painel reorganizado: categorias à esquerda, busca global e lista de funções no centro, ativação/atalho/ajustes à direita. O botão **?** em cada função mostra uma explicação detalhada ao passar o mouse. A ajuda também aparece no cabeçalho da função selecionada e não altera a ativação. A roda do mouse percorre a lista ou os ajustes sob o cursor.

A cor padrão é **Azul neon**, inclusive nos perfis que usavam o antigo destaque padrão. Outras cores, transparência e cores animadas continuam disponíveis em Interface > Tema. Perfis, teclas e posições de HUD continuam no mesmo formato.
