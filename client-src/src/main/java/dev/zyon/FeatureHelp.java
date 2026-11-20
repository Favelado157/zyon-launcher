package dev.zyon;
import java.util.*;
public final class FeatureHelp {
 private static final Map<String,String> TEXT;
 static{Map<String,String> t=new LinkedHashMap<String,String>();
 t.put("crosshair","Personaliza a mira em primeira pessoa: tamanho, espaco central e ponto. A mudanca e somente visual e nao altera alcance, precisao, ataques ou entidades.");
 t.put("zoom","Aproxima a imagem enquanto voce segura a tecla de zoom, C por padrao. Ampliacao define a intensidade. A imagem volta ao soltar a tecla. Troque a tecla em Controles do Minecraft. Nao move a camera nem realiza acoes por voce.");
 t.put("hud","Mostra a marca Zyon, FPS, coordenadas do proprio jogador e recursos visuais ativos. Selecione as informacoes e ajuste as margens X e Y. As coordenadas sao as mesmas disponiveis na tela F3 do jogo.");
 t.put("keystrokes","Mostra as teclas que voce pressiona, botoes do mouse, pulo e cliques por segundo. Nao repete nem simula entradas. Use Ctrl direito ou Editar HUD para arrastar; tamanho, transparencia e posicao ficam salvos no perfil.");
 t.put("theme","Personaliza as cores do menu e do HUD, opacidade e cores animadas. Azul neon e a cor padrao. A opcao Menu inicial Zyon alterna a tela inicial personalizada. Estas preferencias visuais ficam aplicadas sempre.");
 t.put("badge","Mostra o emblema Zyon antes do seu nome no TAB e no proprio nick exibido pelo recurso Meu nick no F5. Preserva o nome, as cores, os cargos e os prefixos enviados pelo servidor. A imagem e local deste cliente e nao identifica automaticamente outros usuarios do launcher.");
 t.put("selfname","Permite ver seu proprio nick acima da cabeca nas duas cameras de terceira pessoa alternadas com F5. Ajuste Tamanho do nick para mudar a escala. O nome some em primeira pessoa, com F1, ao agachar, dormir, ficar invisivel ou em modo espectador. Respeita a profundidade dos blocos e nao muda o nick nos servidores.");
 TEXT=Collections.unmodifiableMap(t);}
 private FeatureHelp(){}public static boolean contains(String id){return TEXT.containsKey(id);}public static String describe(Feature f){String s=TEXT.get(f.id);return s==null?f.description:s;}
}
