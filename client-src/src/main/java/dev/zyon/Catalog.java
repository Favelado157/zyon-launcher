package dev.zyon;
import java.util.*;
import static dev.zyon.Setting.*;
import static dev.zyon.Feature.Category.*;
/** Registry shared by the menu, profile storage and local renderers. */
public final class Catalog {
 private static Feature add(List<Feature> list,String id,String name,String description,Feature.Category category,Setting... settings){Feature f=new Feature(id,name,description,category,settings);f.settings.add(bool("_favorite","Favorito",false));list.add(f);return f;}
 private static void widget(Feature f,int x,int y){f.settings.add(number("x","Posicao X (%)",x,0,100,.1));f.settings.add(number("y","Posicao Y (%)",y,0,100,.1));f.settings.add(number("scale","Tamanho",1,.5,2,.1));f.settings.add(choice("alignX","Alinhamento horizontal",0,"Automatico","Esquerda","Centro","Direita"));f.settings.add(choice("alignY","Alinhamento vertical",0,"Automatico","Topo","Centro","Base"));}
 public static boolean widget(Feature f){return Arrays.asList("keystrokes","fps","health","mainhand","chestplate","boots").contains(f.id);}
 public static void register(List<Feature> fs){
  add(fs,"theme","Aparencia","Temas, cores, fundo e comportamento dos menus do Zyon.",INTERFACE,
   bool("menu","Menu inicial Zyon",true),choice("style","Tema",0,"Zyon","Glass","Vanilla"),bool("pixel","Estilo pixelado",false),
   choice("accent","Paleta de destaque",0,"Azul neon","Azul","Ciano","Verde","Rosa","Laranja"),bool("customAccent","Usar cor personalizada",false),color("highlight","Cor de destaque",0x249BFF),
   choice("icons","Pacote de icones",0,"Padrao","Minimalista"),bool("shadow","Sombra da fonte",true),bool("transitions","Transicoes de tela",true),bool("sounds","Sons da interface",true),
   bool("hideBackground","Ocultar fundo do menu",false),number("opacity","Opacidade",245,100,255,1),bool("rainbow","Cores animadas",false),
   color("frame","Moldura",0x353B4C),color("background","Fundo",0x171B26),color("button","Cor do botao",0x252B3B),
   bool("blur","Desfoque do menu",false),number("blurStrength","Forca do desfoque",4,1,10,1),bool("contrast","Contraste aprimorado",false));
  add(fs,"fullbright","Fullbright","Aumenta o brilho local. Restaura o brilho anterior ao desligar ou sair do mundo.",RENDER);
  add(fs,"itemphysics","Item Physics","Inclina e gira somente a imagem dos itens caidos; nao altera a fisica do servidor.",RENDER,number("speed","Velocidade de rotacao",5,0,10,.5));
  add(fs,"nametag","Nametag","Oculta etiquetas acima dos jogadores. Seu nome no F5 e a insignia continuam configuraveis separadamente.",RENDER,bool("hide","Ocultar todas as etiquetas",false),bool("own","Mostrar o proprio nome",true));
  add(fs,"classicpvp","PvP Classico","Animacoes locais de itens inspiradas na 1.7. Nao realiza ataques ou muda o combate.",RENDER,bool("inventory","Centralizar inventario com efeitos",true),bool("block","Espada / bloqueio",true),bool("position","Posicao dos itens",true),bool("food","Comida",true),bool("rod","Vara de pescar",true),bool("equip","Animacao de equipar antiga",true));
  Feature keys=add(fs,"keystrokes","Keystrokes","Teclas, mouse e CPS reais. Edite cada tecla e arraste o widget no editor.",CLIENT,
   bool("mouse","Botoes do mouse",true),bool("cps","Cliques por segundo",true),bool("jump","Mostrar pulo",true),choice("color","Paleta ao pressionar",0,"Tema","Branco","Azul","Verde","Rosa"),
   number("opacity","Opacidade do fundo",190,0,255,1),color("text","Cor do texto",0xFFFFFF),color("background","Cor do fundo",0x122538),color("pressed","Cor ao pressionar",0x249BFF),bool("customPressed","Usar cor personalizada ao pressionar",false),
   bool("outline","Contorno",true),bool("textOutline","Usar cor do texto no contorno",false),color("outlineColor","Cor do contorno",0x285271),bool("rounded","Cantos arredondados",true),number("radius","Raio dos cantos",4,0,10,1));widget(keys,2,35);
  int[][] positions={{25,0},{0,25},{25,25},{50,25},{0,71},{37,71},{0,50},{0,109}};int[] codes={17,30,31,32,-100,-99,57,42};
  for(int i=0;i<8;i++){keys.settings.add(bool("_key"+i,"Tecla ativa",i<7));keys.settings.add(number("_code"+i,"Codigo",codes[i],-100,255,1));keys.settings.add(number("_kx"+i,"X",positions[i][0],0,180,1));keys.settings.add(number("_ky"+i,"Y",positions[i][1],0,180,1));}
  Feature fps=add(fs,"fps","FPS","Mostra a taxa real de quadros do jogo.",CLIENT,color("labelColor","Cor da legenda",0xFFFFFF),color("bracketColor","Cor dos colchetes",0x9CA7BA),color("valueColor","Cor do valor",0x249BFF),number("lineHeight","Altura da linha (%)",100,50,200,5),choice("format","Formatacao",0,"FPS: valor","[FPS: valor]","valor FPS","valor"));widget(fps,2,15);
  Feature health=add(fs,"health","Vida","Vida e absorcao informadas pelo seu jogador, sem alterar o HUD do servidor.",CLIENT,choice("mode","Modo de exibicao",0,"Vida","Porcentagem","Vida e absorcao"),choice("icon","Estilo do icone",0,"Automatico","Coracao","Sem icone"),choice("hardcore","Modo Hardcore",0,"Automatico","Ativado","Desativado"),color("text","Cor do texto",0xFF677D));widget(health,40,85);
  String[] ids={"mainhand","chestplate","boots"},names={"Mao principal","Peitoral","Botas"};for(int i=0;i<ids.length;i++){Feature f=add(fs,ids[i],names[i],"Exibe o item equipado e a durabilidade real. A previa usa um item de exemplo.",CLIENT,choice("durability","Exibicao da durabilidade",0,"Barra","Porcentagem","Restante","Ocultar"),choice("durabilityColor","Cor da durabilidade",0,"Simples","Verde a vermelho"));widget(f,70+i*8,85);}
  add(fs,"blockoverlay","Contorno de blocos","Personaliza apenas o bloco selecionado pela mira, respeitando a profundidade.",RENDER,bool("customOutline","Personalizar contorno",true),color("outlineColor","Cor do contorno",0x249BFF),number("width","Espessura do contorno",2,1,5,.5),bool("customFill","Personalizar preenchimento",true),color("fillColor","Cor do preenchimento",0x249BFF),number("fillAlpha","Opacidade do preenchimento",35,0,120,1));
  add(fs,"crosshair","Mira","Personaliza a mira exibida na tela, em primeira pessoa.",RENDER,number("size","Tamanho",5,2,16,1),number("gap","Espaco central",3,1,12,1),bool("dot","Ponto central",true));
  add(fs,"zoom","Zoom","Aproxima a imagem enquanto a tecla de zoom estiver pressionada.",RENDER,number("factor","Ampliacao",3,1.5,6,.5));
  add(fs,"hud","HUD Zyon","Marca, coordenadas e recursos visuais ativos.",CLIENT,bool("logo","Mostrar icone",true),bool("fps","Mostrar FPS",true),bool("coords","Mostrar coordenadas",true),bool("modules","Mostrar recursos",true),number("x","Margem X",12,0,500,1),number("y","Margem Y",12,0,300,1));
  add(fs,"badge","Insignia Zyon","Seu emblema antes do nome no TAB e no F5; visivel neste cliente.",CLIENT);
  add(fs,"selfname","Meu nick no F5","Mostra seu nick acima da cabeca nas duas cameras F5, preservando as cores do servidor.",CLIENT,number("scale","Tamanho do nick",1,.7,1.5,.1));
 }
}
