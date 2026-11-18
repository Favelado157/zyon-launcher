package dev.zyon;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Explanations of the client behaviour, shared by every help button. */
public final class FeatureHelp {
    private static final Map<String, String> TEXT;
    static {
        Map<String, String> text = new LinkedHashMap<String, String>();
        text.put("killaura", "Seleciona um alvo por vez dentro do alcance e do campo de visao (FOV). Ataca no intervalo de CPS escolhido e pode virar a camera para ele. Tipos de entidade, invisiveis, paredes e prioridade seguem os filtros deste modulo.");
        text.put("triggerbot", "Ataca automaticamente a entidade que esta na sua mira, respeitando alcance e CPS. Usa os filtros de alvos do KillAura, incluindo tipos, invisiveis e paredes. A opcao Exigir arma limita a espadas e machados.");
        text.put("autoclicker", "Repete ataques enquanto voce segura o botao esquerdo. Nao interfere quando a mira aponta para um bloco. Respeita o CPS, a opcao de arma e os filtros de alvo do KillAura; sem um alvo valido, apenas anima o braco.");
        text.put("aimassist", "Ao segurar o botao esquerdo, gira a camera aos poucos na direcao de um alvo dentro do alcance e FOV. A intensidade limita os graus por tick. Usa os filtros e a prioridade do KillAura. Sozinho, nao ataca automaticamente.");
        text.put("autosprint", "Mantem a corrida enquanto voce anda. Precisa de fome suficiente e nao atua agachado ou contra uma parede. Todas as direcoes permite correr tambem ao andar de lado ou para tras.");
        text.put("fly", "Controla seu movimento no ar: Espaco sobe e a tecla de agachar desce. Ajuste as velocidades horizontal e vertical. Enquanto ligado, tem prioridade sobre Speed e AutoJump; sem apertar teclas, interrompe o movimento.");
        text.put("speed", "Ajusta a velocidade horizontal ao andar. Bunny hop pula quando voce se move no chao. Nao atua na agua, lava, escadas ou agachado. Se Fly estiver ligado, o movimento do Fly tem prioridade.");
        text.put("step", "Aumenta a altura que voce consegue subir ao encostar em blocos, sem precisar pular. O ajuste define essa altura. Ao desligar, restaura a altura de subida que estava em uso antes.");
        text.put("autojump", "Pula automaticamente quando voce esta andando e toca o chao. Nao atua agachado, na agua, lava ou escadas. Fica sem efeito enquanto Fly estiver ligado.");
        text.put("safewalk", "Simula a tecla de agachar quando voce anda no chao e nao encontra apoio logo a frente. Distancia da borda define o quanto verifica adiante. Ao voltar a um local com apoio, libera o agachamento automatico.");
        text.put("fullbright", "Aumenta o brilho local da imagem pelo ajuste de gamma, facilitando enxergar em locais escuros. Nao cria luz nos blocos do mundo. Ao desligar, restaura o brilho que estava configurado antes.");
        text.put("esp", "Desenha caixas ao redor das entidades ja carregadas no seu mundo. Voce escolhe jogadores, monstros, animais, distancia e espessura. Atraves de paredes permite ver as caixas mesmo com blocos na frente.");
        text.put("tracers", "Desenha linhas da sua camera ate entidades ja carregadas, ajudando a localizar sua direcao. Filtre jogadores, monstros, animais e distancia. Atraves de paredes controla se as linhas aparecem com blocos na frente.");
        text.put("crosshair", "Desenha uma mira colorida no centro da tela, por cima da mira padrao, em primeira pessoa. Tamanho, espaco central e ponto ajustam seu formato. Nao muda a precisao ou o alcance dos ataques.");
        text.put("autotool", "Ao segurar o botao esquerdo sobre um bloco, escolhe na hotbar o item com maior velocidade base para quebrar esse bloco. Nao procura no restante do inventario. Restaurar slot retorna ao item anterior ao terminar.");
        text.put("autoeat", "Quando a fome chega ao limite escolhido ou abaixo dele, usa a comida da hotbar que recupera mais fome. Nao usa macas douradas nem procura no inventario. Nao atua no criativo; ao terminar, volta ao slot anterior e libera o uso automatico.");
        text.put("fastplace", "Reduz o intervalo local entre colocacoes enquanto voce segura um bloco. O ajuste vai de 0 a 4 ticks. Nao coloca blocos sozinho e nao aumenta seu alcance: voce continua usando o botao de colocar.");
        text.put("hud", "Mostra as informacoes do Zyon durante o jogo: marca, FPS, coordenadas, alvo de combate e lista de modulos ativos. Escolha cada item e ajuste as margens X e Y. Os keystrokes tem controles separados.");
        text.put("theme", "Configura a aparencia do menu e do HUD: cor de destaque, opacidade e cores animadas. A cor padrao e azul neon. Menu inicial Zyon alterna a tela inicial personalizada. O tema e aplicado sempre, sem precisar ativar um modulo.");
        text.put("keystrokes", "Mostra as teclas de movimento, botoes do mouse, pulo e cliques por segundo (CPS). Configure tamanho, cores e transparencia. Use Editar HUD ou Ctrl direito para arrastar o painel; a posicao fica salva no perfil.");
        TEXT = Collections.unmodifiableMap(text);
    }
    private FeatureHelp() {}
    public static boolean contains(String id) { return TEXT.containsKey(id); }
    public static String describe(Feature feature) {
        String text = TEXT.get(feature.id);
        return text == null ? feature.description : text;
    }
}
