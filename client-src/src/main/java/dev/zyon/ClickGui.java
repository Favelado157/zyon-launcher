package dev.zyon;

import java.io.IOException;
import java.awt.Color;
import java.util.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

/** Catalog and detail pages use the same coordinate system for drawing and hit tests. */
public final class ClickGui extends GuiScreen {
 private final Zyon client;private Feature selected;private int scroll,category,scene;private boolean list,binding,cheats;
 private float scale;private int ox,oy,mx,my;private long opened;
 private GuiTextField search,hex,number;private double colorBefore;private Setting editingColor,editingNumber,dragging;private String tip="",notice="";
 private final java.util.List<Row> rows=new ArrayList<Row>();private final java.util.List<Card> cards=new ArrayList<Card>();
 private net.minecraft.client.shader.ShaderGroup ownedBlur;private boolean blurAttempted;
 private static final int W=800,H=464,LEFT=56,TOP=56,PW=688,PH=366;
 private static final class Row {Setting s;int y;Row(Setting s,int y){this.s=s;this.y=y;}}
 private static final class Card {Feature f;int x,y,w;Card(Feature f,int x,int y,int w){this.f=f;this.x=x;this.y=y;this.w=w;}}
 public ClickGui(Zyon client){this.client=client;}
 private Feature theme(){return client.feature("theme");}
 private int accent(){return client.accent();}
 private int radius(){return theme().flag("pixel")||theme().setting("style").integer()==2?0:5;}
 private int bg(){int rgb=theme().setting("background").integer();int a=theme().setting("style").integer()==1?185:theme().setting("opacity").integer();return (a<<24)|rgb;}
 private int card(){return 0xF0000000|theme().setting("button").integer();}
 public void initGui(){scale=ClickLayout.scale(width,height);ox=(int)((width-W*scale)/2);oy=(int)((height-H*scale)/2);String query=search==null?"":search.getText();search=new GuiTextField(0,fontRendererObj,74,105,393,22);search.setMaxStringLength(40);search.setText(query);search.setEnableBackgroundDrawing(false);opened=System.currentTimeMillis();}
 private int vx(int x){return (int)((x-ox)/scale);}private int vy(int y){return (int)((y-oy)/scale);}
 private void text(String s,int x,int y,int c){if(theme().flag("shadow"))fontRendererObj.drawStringWithShadow(s,x,y,c);else fontRendererObj.drawString(s,x,y,c);}
 private void label(String s,int x,int y){text(s,x,y,0xFFF0F2FA);}
 private void rect(int x,int y,int w,int h,int color){Ui.box(x,y,w,h,radius(),color);}
 private boolean hit(int x,int y,int w,int h){return Ui.hit(mx,my,x,y,w,h);}
 private void button(String s,int x,int y,int w,boolean active){rect(x,y,w,23,active?accent():hit(x,y,w,23)?0xFF39435A:card());text(fontRendererObj.trimStringToWidth(s,w-12),x+6,y+8,0xFFF7F8FF);}
 private void toggle(boolean on,int x,int y){Ui.box(x,y,30,15,4,on?accent():0xFF454B59);Ui.box(x+(on?17:2),y+2,11,11,3,0xFFF2F4FC);}
 private void sound(){if(theme().flag("sounds"))mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"),1));}
 private void clip(int x,int y,int w,int h){ScaledResolution res=new ScaledResolution(mc);int s=res.getScaleFactor();GL11.glEnable(GL11.GL_SCISSOR_TEST);GL11.glScissor((int)((ox+x*scale)*s),(int)((height-oy-(y+h)*scale)*s),(int)(w*scale*s),(int)(h*scale*s));}
 public void drawScreen(int x,int y,float partial){try(HudRenderState state=new HudRenderState()){drawContents(x,y,partial);}}
 private void drawContents(int x,int y,float partial){
  mx=vx(x);my=vy(y);tip="";updateBlur();
  if(!theme().flag("hideBackground")){int a=theme().flag("transitions")?(int)(150*Math.min(1,(System.currentTimeMillis()-opened)/180f)):150;drawRect(0,0,width,height,(a<<24)|0x080B14);}
  GlStateManager.pushMatrix();GlStateManager.translate(ox,oy,0);GlStateManager.scale(scale,scale,1);
  try{
   rect(0,0,W,34,0xEC101521);Branding.image(Branding.LOGO,13,5,24,24);label("ZYON",44,13);button("Menu",126,6,65,false);button("Recursos",196,6,65,!cheats);button("Xit",348,6,65,cheats);button("Opcoes",266,6,72,false);text(mc.thePlayer==null?"Zyon Client":mc.thePlayer.getName(),610,13,0xFFAEBBD1);button("X",758,6,28,false);
   rect(LEFT-1,TOP-1,PW+2,PH+2,theme().setting("frame").rgb());rect(LEFT,TOP,PW,PH,bg());if(theme().flag("contrast"))rect(LEFT,TOP,PW,PH,0x66000000);
   if(selected==null)catalog();else detail(partial);
   text("Shift direito: menu   /   Ctrl direito: editar HUD",74,438,0xFF8794AB);text("1.8.9",691,438,0xFF8794AB);
   if(!notice.isEmpty())text(fontRendererObj.trimStringToWidth(notice,650),74,425,0xFFFFD385);
   if(editingColor!=null)colorPicker();else if(!tip.isEmpty())tooltip(tip);
  }finally{GL11.glDisable(GL11.GL_SCISSOR_TEST);GlStateManager.color(1,1,1,1);GlStateManager.popMatrix();}
 }
 private boolean categoryMatches(Feature f){if(category==0)return true;if(category==1)return f.flag("_favorite");if(cheats){String group=CheatCatalog.group(f.id);return category==2&&group.equals("Combate")||category==3&&group.equals("Movimento")||category==4&&group.equals("Utilidades");}return category==2&&Catalog.widget(f)||category==3&&f.category==Feature.Category.RENDER||category==4&&f.category==Feature.Category.INTERFACE;}
 private java.util.List<Feature> filtered(){java.util.List<Feature> out=new ArrayList<Feature>();String q=search.getText().toLowerCase(Locale.ROOT);for(Feature f:client.features)if((cheats?f.category==Feature.Category.CHEAT:f.category!=Feature.Category.CHEAT)&&categoryMatches(f)&&(f.name.toLowerCase(Locale.ROOT).contains(q)||f.description.toLowerCase(Locale.ROOT).contains(q)))out.add(f);return out;}
 private void catalog(){
  label(cheats?"Xit":"Recursos",74,76);text(cheats?"Combate / movimento / utilidades":"Seu jogo. Do seu jeito.",140,76,0xFF98A5BD);button(cheats?"Desligar Xit":"Editar widgets",601,68,125,false);
  rect(70,100,404,30,0xFF0E1320);if(search.getText().isEmpty()&&!search.isFocused())text("Pesquisar recursos...",80,111,0xFF8592AC);search.drawTextBox();
  button((cheats?new String[]{"Todos","Favoritos","Combate","Movimento","Utilidades"}:new String[]{"Todos","Favoritos","Widgets","Visual","Aparencia"})[category],484,102,127,false);button(list?"Grade":"Lista",619,102,107,false);
  cards.clear();java.util.List<Feature> fs=filtered();int cols=list?1:2;int count=CatalogLayout.rows(fs.size(),list);scroll=CatalogLayout.clamp(scroll,fs.size(),list);
  clip(69,140,660,268);for(int i=0;i<fs.size();i++){Feature f=fs.get(i);int[] geometry=CatalogLayout.card(i,list,scroll);int cx=geometry[0],cy=geometry[1],cw=geometry[2];cards.add(new Card(f,cx,cy,cw));rect(cx,cy,cw,53,hit(cx,cy,cw,53)?0xFF30394D:card());rect(cx+9,cy+10,32,32,0xFF111B2B);drawFeatureIcon(f,cx+17,cy+18);label(fontRendererObj.trimStringToWidth(f.name,cw-115),cx+51,cy+12);text(fontRendererObj.trimStringToWidth(f.description,cw-85),cx+51,cy+32,0xFF9BA7BE);if(!f.id.equals("theme"))toggle(f.enabled(),cx+cw-41,cy+10);text("?",cx+cw-21,cy+36,0xFFAFC3E4);if(hit(cx+cw-30,cy+29,28,22))tip=FeatureHelp.describe(f);}
  GL11.glDisable(GL11.GL_SCISSOR_TEST);bar(scroll,count*61,269,734,140);if(fs.isEmpty())text("Nenhum recurso encontrado.",87,162,0xFFAEBAD0);
 }
 private void drawFeatureIcon(Feature f,int x,int y){if(theme().setting("icons").integer()==1){text(icon(f),x+2,y+4,accent());return;}if(f.id.equals("badge")||f.id.equals("selfname")){Branding.image(new ResourceLocation("zyon","textures/brand/badge.png"),x,y,18,18);return;}net.minecraft.item.Item item=f.id.equals("fullbright")?Items.glowstone_dust:f.id.equals("itemphysics")?Items.feather:f.id.equals("nametag")?Items.name_tag:f.id.equals("classicpvp")||f.id.equals("mainhand")?Items.diamond_sword:f.id.equals("chestplate")?Items.diamond_chestplate:f.id.equals("boots")?Items.diamond_boots:f.id.equals("helmet")?Items.diamond_helmet:f.id.equals("leggings")?Items.diamond_leggings:f.id.equals("autotext")?Items.paper:f.id.equals("theme")?Items.painting:f.id.equals("health")?Items.apple:f.id.equals("blockoverlay")?Items.brick:f.id.equals("zoom")?Items.ender_eye:null;if(item==null){text(icon(f),x+2,y+4,accent());return;}net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();mc.getRenderItem().renderItemAndEffectIntoGUI(new ItemStack(item),x,y);net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();GlStateManager.disableDepth();}
 private String icon(Feature f){if(theme().setting("icons").integer()==1)return "+";if(f.id.equals("fps"))return "F";if(f.id.equals("health"))return "H";if(f.id.equals("theme"))return "A";if(f.id.equals("keystrokes"))return "W";return f.name.substring(0,1).toUpperCase(Locale.ROOT);}
 private boolean visualPreview(){return Arrays.asList("fullbright","itemphysics","nametag","classicpvp","health").contains(selected.id);}
 private int settingsTop(){return Catalog.widget(selected)?252:visualPreview()?226:112;}
 private java.util.List<Setting> shown(){java.util.List<Setting> out=new ArrayList<Setting>();for(Setting s:selected.settings)if(!s.id.startsWith("_")&&!(Catalog.widget(selected)&&(s.id.equals("x")||s.id.equals("y"))))out.add(s);return out;}
 private void detail(float partial){
  button(cheats?"< Xit":"< Recursos",72,68,125,false);label(selected.name,211,76);button(selected.flag("_favorite")?"*":"+",562,68,27,selected.flag("_favorite"));
  if(Catalog.widget(selected))button("Editar widgets",597,68,129,false);else if(!selected.id.equals("theme"))toggle(selected.enabled(),692,73);
  int top=settingsTop();
  if(visualPreview())visualPreview(partial);
  if(Catalog.widget(selected)){
   rect(72,104,654,136,card());text("Pre-visualizacao do widget",82,114,0xFFCED8E9);int[] scenes={0xFF162A3C,0xFF423528,0xFF17372F,0xFF101320};Ui.box(82,130,634,74,3,scenes[scene]);
   for(int i=0;i<15;i++){int xx=84+i*43;drawRect(xx,186-(i%4)*9,xx+34,204,0x40249BFF);}
   if(selected.id.equals("keystrokes")){GlStateManager.pushMatrix();GlStateManager.translate(356,133,0);GlStateManager.scale(.6,.6,1);client.keystrokes.drawAt(0,0,true);GlStateManager.popMatrix();}
   else Widgets.preview(client,selected,385,154);
   for(int i=0;i<4;i++){rect(626+i*21,182,16,16,i==scene?accent():0xFF4C566B);}
   text(selected.enabled()?"No seu HUD":"Fora do seu HUD",83,220,0xFFB9C4D8);if(selected.enabled()){button("Mover",498,209,75,false);button("Remover",581,209,135,false);}else button("Posicionar widget",555,209,161,true);
  }
  java.util.List<Setting> settings=shown();int extras=selected.id.equals("keystrokes")||selected.id.equals("autotext")?2:selected.id.equals("theme")?2:selected.id.equals("nametag")?2:1;
  int groupCount=0;for(Setting s:settings)if(!group(s).isEmpty())groupCount++;int total=settings.size()*45+groupCount*24+extras*37+unavailable().size()*38+(selected.id.equals("theme")?55:0),view=414-top;scroll=Math.max(0,Math.min(scroll,Math.max(0,total-view)));rows.clear();clip(70,top,660,view);
  int yy=top-scroll;
  if(!selected.id.equals("theme")){button(binding?"Pressione uma tecla...":"Atalho: "+(selected.key==0?"Definir tecla":Keyboard.getKeyName(selected.key)),83,yy+4,275,false);yy+=37;}
  if(selected.id.equals("keystrokes")){button("Editar layout das teclas",83,yy+4,313,false);yy+=37;}
  if(selected.id.equals("autotext")){button("Editar mensagens de autotexto",83,yy+4,313,false);yy+=37;}
  if(selected.id.equals("theme")){button("Personalizacao da skin",83,yy+4,248,false);button("Perfis",342,yy+4,150,false);yy+=37;text("Temas: Zyon / Glass / Vanilla",84,yy+10,accent());yy+=37;}
  for(Setting s:settings){String heading=group(s);if(!heading.isEmpty()){text(heading,77,yy+8,0xFF91A6C4);yy+=24;}if(selected.id.equals("theme")&&s.id.equals("style")){rows.add(new Row(s,yy));String[] themes={"Zyon","Glass","Vanilla"};for(int i=0;i<3;i++){int xx=75+i*218;rect(xx,yy+2,212,92,i==s.integer()?accent():card());rect(xx+2,yy+4,208,65,i==1?0xA038495E:i==2?0xFF444444:0xFF111E30);rect(xx+13,yy+15,186,9,i==2?0xFF888888:0xFF526B8E);rect(xx+13,yy+32,89,26,0xFF28486D);rect(xx+109,yy+32,89,26,0xFF28486D);label(themes[i],xx+12,yy+76);}yy+=100;continue;}rows.add(new Row(s,yy));rect(74,yy+2,650,39,card());label(fontRendererObj.trimStringToWidth(s.label,360),85,yy+17);
   if(s.kind==Setting.Kind.BOOLEAN)toggle(s.on(),679,yy+14);
   else if(s.kind==Setting.Kind.CHOICE){button(s.display()+"  v",491,yy+9,219,false);}
   else if(s.kind==Setting.Kind.COLOR){rect(660,yy+11,48,22,s.rgb());text(s.display(),554,yy+17,0xFFAFBDD3);}
   else{int sx=493,sw=142;Ui.box(sx,yy+21,sw,3,1,0xFF101522);int pos=(int)(sw*(s.get()-s.min)/(s.max-s.min));Ui.box(sx,yy+21,Math.max(1,pos),3,1,accent());rect(sx+pos-3,yy+16,7,13,0xFFB8C9E3);button(s.display(),650,yy+9,60,false);}
   if(hit(76,yy+2,370,39))tip=s.label+". "+(s.kind==Setting.Kind.NUMBER?"Arraste a barra ou clique no valor para digitar.":s.kind==Setting.Kind.COLOR?"Clique na cor para abrir a paleta e editar o HEX.":"Clique no controle para alterar. Salvo no perfil.");yy+=45;
  }
  for(String unavailable:unavailable()){rect(74,yy+2,650,33,0x80101622);text(unavailable,85,yy+14,0xFF8B99AE);if(hit(74,yy+2,650,33))tip="Opcao da referencia indisponivel: "+unavailable+". O Zyon nao simula dados de servicos externos nem funcoes de outras versoes.";yy+=38;}
  GL11.glDisable(GL11.GL_SCISSOR_TEST);bar(scroll,total,view,734,top);
  if(editingNumber!=null){rect(276,193,250,72,0xFF101724);label("Digite o valor e pressione Enter",286,205);number.drawTextBox();}
 }
 private String group(Setting s){String id=selected.id,k=s.id;if(id.equals("theme")){if(k.equals("menu"))return "Identidade";if(k.equals("style"))return "Tema";if(k.equals("accent"))return "Cores";if(k.equals("transitions"))return "Menus";if(k.equals("blur"))return "Desfoque";}if(id.equals("keystrokes")){if(k.equals("mouse"))return "Teclas e cliques";if(k.equals("text"))return "Cores";if(k.equals("outline"))return "Contorno";if(k.equals("rounded"))return "Cantos";}if(id.equals("classicpvp")){if(k.equals("inventory"))return "Inventario";if(k.equals("block"))return "Animacoes de itens";if(k.equals("equip"))return "Equipar itens";}if(k.equals("alignX"))return "Alinhamento";return "";}
 private java.util.List<String> unavailable(){if(selected.id.equals("nametag"))return Arrays.asList("Bandeira do pais  /  requer servico de perfis", "Transmissoes ao vivo  /  requer integracao com plataformas");if(selected.id.equals("classicpvp"))return Arrays.asList("Ocultar indicador de ataque  /  nao existe na 1.8.9");if(selected.id.equals("theme"))return Arrays.asList("Roda de gestos  /  sistema ainda nao incluido no Zyon");return Collections.emptyList();}
 private void visualPreview(float partial){
  rect(72,104,654,111,card());Branding.image(new ResourceLocation("zyon","textures/menu.png"),82,113,634,93);drawRect(82,113,716,206,0x9A081223);
  if(selected.id.equals("fullbright")){drawRect(82,113,397,206,0xB0000000);text("Antes",101,126,0xFF9AA7BA);text("Com Fullbright",420,126,0xFFFFFFFF);text("Previa ilustrativa / altera somente o brilho local",100,186,0xFFBCCBE0);}
  else if(selected.id.equals("itemphysics")){GlStateManager.pushMatrix();GlStateManager.translate(390,157,0);GlStateManager.rotate((System.currentTimeMillis()%10000)/1000f*(float)selected.number("speed"),0,0,1);net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();mc.getRenderItem().renderItemAndEffectIntoGUI(new ItemStack(Items.diamond_sword),-8,-8);net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();GlStateManager.popMatrix();text("Previa de rotacao / itens caidos",100,185,0xFFCED8EA);}
  else {if(mc.thePlayer!=null){GlStateManager.color(1,1,1,1);float lx=dev.zyon.platform.Platform.lastBrightnessX,ly=dev.zyon.platform.Platform.lastBrightnessY;net.minecraft.client.renderer.OpenGlHelper.setLightmapTextureCoords(net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit,240,240);net.minecraft.client.gui.inventory.GuiInventory.drawEntityOnScreen(596,199,36,0,0,mc.thePlayer);net.minecraft.client.renderer.OpenGlHelper.setLightmapTextureCoords(net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit,lx,ly);}if(selected.id.equals("health")){label("20 HP",295,131);label(mc.thePlayer==null?"Jogador":mc.thePlayer.getName(),270,150);text("Vida acima da cabeca / dados enviados pelo servidor",100,186,0xFFBECBE0);}else if(selected.id.equals("nametag")){String name=mc.thePlayer==null?"Jogador Zyon":mc.thePlayer.getName();Branding.image(new ResourceLocation("zyon","textures/brand/badge.png"),270,145,16,16);label(name,291,150);text("Respeita F1, invisibilidade e agachamento",100,186,0xFFBECBE0);}else {text("Animacoes locais de itens",100,132,accent());text("Bloqueio / comida / vara / equipar",100,151,0xFFCAD6E9);text("Nao altera ataques ou regras do servidor",100,186,0xFF9FABBF);}}
 }
 private void bar(int value,int total,int view,int x,int y){if(total<=view)return;Ui.box(x,y,3,view,1,0xFF0D111C);int h=Math.max(18,view*view/total);Ui.box(x,y+value*(view-h)/Math.max(1,total-view),3,h,1,0xFF65748F);}
 private void tooltip(String s){java.util.List<String> lines=fontRendererObj.listFormattedStringToWidth(s,280);int tw=298,th=lines.size()*12+16;int x=Math.max(8,Math.min(mx+10,W-tw-8)),y=my+16;if(y+th>H-8)y=my-th-8;rect(x,y,tw,th,0xFC07101F);for(int i=0;i<lines.size();i++)text(lines.get(i),x+9,y+8+i*12,0xFFD9E5FA);}
 private void openColor(Setting s){editingColor=s;colorBefore=s.get();hex=new GuiTextField(1,fontRendererObj,337,320,126,20);hex.setMaxStringLength(7);hex.setText(s.display());hex.setFocused(true);}
 private void colorPicker(){rect(260,130,280,241,0xFF0C121F);label("Cor / "+editingColor.label,273,143);int[] presets={0xFFFFFF,0x000000,0x249BFF,0x58D9E8,0x72D9A0,0xFF677D,0xBB79FF,0xFFCA66};for(int i=0;i<8;i++)rect(275+i*31,164,25,20,0xFF000000|presets[i]);float[] hsv=Color.RGBtoHSB((editingColor.integer()>>16)&255,(editingColor.integer()>>8)&255,editingColor.integer()&255,null);for(int i=0;i<120;i+=3)for(int j=0;j<96;j+=3)drawRect(276+i*2,194+j,282+i*2,197+j,Color.HSBtoRGB(hsv[0],i/117f,1-j/93f));for(int i=0;i<240;i+=2)drawRect(276+i,299,278+i,310,Color.HSBtoRGB(i/240f,1,1));label("HEX",279,327);hex.drawTextBox();button("OK",472,319,48,true);text("Enter confirma / Esc volta",276,354,0xFF98A6BE);}
 private void pickerClick(){if(hit(275,164,248,20)){int[] presets={0xFFFFFF,0x000000,0x249BFF,0x58D9E8,0x72D9A0,0xFF677D,0xBB79FF,0xFFCA66};int i=Math.min(7,(mx-275)/31);editingColor.set(presets[i]);hex.setText(editingColor.display());}else if(hit(276,194,240,96)){float[] hsv=Color.RGBtoHSB((editingColor.integer()>>16)&255,(editingColor.integer()>>8)&255,editingColor.integer()&255,null);editingColor.set(Color.HSBtoRGB(hsv[0],(mx-276)/239f,1-(my-194)/95f)&0xFFFFFF);hex.setText(editingColor.display());}else if(hit(276,299,240,12)){editingColor.set(Color.HSBtoRGB((mx-276)/239f,1,1)&0xFFFFFF);hex.setText(editingColor.display());}else if(hit(472,319,48,23))finishColor();else hex.mouseClicked(mx,my,0);}
 private void finishColor(){try{String s=hex.getText().replace("#","");if(!s.matches("[0-9a-fA-F]{6}"))throw new IllegalArgumentException();editingColor.set(Integer.parseInt(s,16));if(selected.id.equals("theme")&&editingColor.id.equals("highlight"))theme().setting("customAccent").set(1);if(selected.id.equals("keystrokes")&&editingColor.id.equals("pressed"))selected.setting("customPressed").set(1);editingColor=null;client.profiles.saveAutomatic();notice="";}catch(Exception e){notice="Use HEX com 6 caracteres, por exemplo #249BFF.";}}
 protected void mouseClicked(int x,int y,int b){
  mx=vx(x);my=vy(y);if(b!=0)return;
  if(editingColor!=null){pickerClick();return;}if(editingNumber!=null){number.mouseClicked(mx,my,b);return;}
  if(hit(196,6,65,23)||hit(348,6,65,23)){cheats=hit(348,6,65,23);selected=null;binding=false;category=0;scroll=0;search.setText("");return;}
  if(hit(758,6,28,23)){mc.displayGuiScreen(null);return;}if(hit(126,6,65,23)){mc.displayGuiScreen(mc.theWorld==null?new GuiMainMenu():new GuiIngameMenu());return;}if(hit(266,6,72,23)){mc.displayGuiScreen(new GuiOptions(this,mc.gameSettings));return;}
  if(selected==null){search.mouseClicked(mx,my,b);if(hit(601,68,125,23)){if(cheats){for(Feature f:client.features)if(f.category==Feature.Category.CHEAT)f.setEnabled(false);client.profiles.saveAutomatic();}else mc.displayGuiScreen(new HudEditor(client,this));return;}if(hit(484,102,127,23)){category=(category+1)%5;scroll=0;return;}if(hit(619,102,107,23)){list=!list;scroll=0;return;}if(my<140||my>=408)return;for(Card c:cards)if(hit(c.x,c.y,c.w,53)){sound();if(!c.f.id.equals("theme")&&hit(c.x+c.w-45,c.y+5,40,23))c.f.toggle();else{selected=c.f;scroll=0;}client.profiles.saveAutomatic();return;}}
  else{
   if(hit(72,68,125,23)){selected=null;scroll=0;binding=false;return;}if(hit(562,68,27,23)){selected.setting("_favorite").cycle();client.profiles.saveAutomatic();return;}
   if(Catalog.widget(selected)){if(hit(597,68,129,23)||hit(498,209,75,23)&&selected.enabled()){mc.displayGuiScreen(new HudEditor(client,this));return;}if(hit(555,209,161,23)){boolean adding=!selected.enabled();selected.toggle();client.profiles.saveAutomatic();if(adding)mc.displayGuiScreen(new HudEditor(client,this));return;}if(hit(626,182,84,16)){scene=Math.min(3,(mx-626)/21);return;}}
   else if(!selected.id.equals("theme")&&hit(692,73,30,15)){selected.toggle();client.profiles.saveAutomatic();return;}
   int top=settingsTop();if(my<top||my>=414)return;int yy=top-scroll;
   if(!selected.id.equals("theme")){if(hit(83,yy+4,275,23)){binding=true;return;}yy+=37;}
   if(selected.id.equals("keystrokes")&&hit(83,yy+4,313,23)){mc.displayGuiScreen(new KeyLayoutEditor(client,this));return;}
   if(selected.id.equals("autotext")&&hit(83,yy+4,313,23)){mc.displayGuiScreen(new AutoTextGui(client,this));return;}
   if(selected.id.equals("theme")){if(hit(83,yy+4,248,23)){mc.displayGuiScreen(new GuiCustomizeSkin(this));return;}if(hit(342,yy+4,150,23)){mc.displayGuiScreen(new ProfileGui(client,this));return;}}
   for(Row r:rows)if(hit(74,r.y+2,650,selected.id.equals("theme")&&r.s.id.equals("style")?94:39)){Setting s=r.s;if(selected.id.equals("theme")&&s.id.equals("style")){s.set(Math.min(2,(mx-75)/218));client.profiles.saveAutomatic();return;}sound();if(s.kind==Setting.Kind.BOOLEAN)s.cycle();else if(s.kind==Setting.Kind.CHOICE){mc.displayGuiScreen(new ChoiceGui(client,this,s));return;}else if(s.kind==Setting.Kind.COLOR){openColor(s);return;}else if(mx>=650){editingNumber=s;number=new GuiTextField(2,fontRendererObj,286,228,225,20);number.setText(s.display());number.setFocused(true);}else if(mx>=480){dragging=s;setSlider(s,mx);}client.profiles.saveAutomatic();return;}
  }
 }
 private void setSlider(Setting s,int x){s.set(s.min+Math.max(0,Math.min(1,(x-493)/142.0))*(s.max-s.min));}
 protected void mouseClickMove(int x,int y,int b,long t){mx=vx(x);my=vy(y);if(editingColor!=null){if(hit(276,194,240,96)||hit(276,299,240,12))pickerClick();}else if(dragging!=null)setSlider(dragging,mx);}
 protected void mouseReleased(int x,int y,int b){if(dragging!=null){dragging=null;client.profiles.saveAutomatic();}super.mouseReleased(x,y,b);}
 public void handleMouseInput(){super.handleMouseInput();int delta=Mouse.getEventDWheel();if(delta!=0&&editingColor==null&&editingNumber==null)scroll=Math.max(0,scroll+(delta<0?36:-36));}
 protected void keyTyped(char c,int key){
  if(editingColor!=null){if(key==Keyboard.KEY_ESCAPE){editingColor.set(colorBefore);editingColor=null;}else if(key==Keyboard.KEY_RETURN)finishColor();else hex.textboxKeyTyped(c,key);return;}
  if(editingNumber!=null){if(key==Keyboard.KEY_ESCAPE){editingNumber=null;return;}if(key==Keyboard.KEY_RETURN){try{double v=Double.parseDouble(number.getText().replace(',','.'));if(!Double.isFinite(v))throw new IllegalArgumentException();editingNumber.set(v);editingNumber=null;client.profiles.saveAutomatic();notice="";}catch(Exception e){notice="Digite um numero valido.";}}else number.textboxKeyTyped(c,key);return;}
  if(binding){selected.key=key==Keyboard.KEY_ESCAPE||key==Keyboard.KEY_DELETE?0:key;binding=false;client.profiles.saveAutomatic();return;}
  if(key==Keyboard.KEY_ESCAPE){if(selected!=null){selected=null;scroll=0;}else mc.displayGuiScreen(null);return;}if(selected==null&&search.textboxKeyTyped(c,key)){scroll=0;return;}if(key==client.menuKey.getKeyCode())mc.displayGuiScreen(null);
 }
 private void updateBlur(){
  if(ownedBlur!=null&&mc.entityRenderer.getShaderGroup()==ownedBlur)try{java.lang.reflect.Field field=dev.zyon.platform.ReflectionHelper.findField(net.minecraft.client.shader.ShaderGroup.class,"listShaders","field_148031_d");for(Object part:(java.util.List<?>)field.get(ownedBlur))((net.minecraft.client.shader.Shader)part).getShaderManager().getShaderUniform("Radius").set((float)theme().number("blurStrength"));}catch(Exception ignored){}
if(!theme().flag("blur")){stopBlur();blurAttempted=false;return;}if(blurAttempted)return;if(!net.minecraft.client.renderer.OpenGlHelper.isFramebufferEnabled()){blurAttempted=true;notice="Desfoque requer framebuffer ativado nas opcoes de video.";return;}if(mc.theWorld==null){notice="Desfoque disponivel quando um mundo estiver carregado.";return;}if(mc.entityRenderer.isShaderActive()){blurAttempted=true;notice="Desfoque preservou o shader que ja esta ativo.";return;}blurAttempted=true;try{dev.zyon.platform.ReflectionHelper.findMethod(net.minecraft.client.renderer.EntityRenderer.class,mc.entityRenderer,new String[]{"loadShader"},ResourceLocation.class).invoke(mc.entityRenderer,new ResourceLocation("zyon","shaders/post/menu_blur.json"));ownedBlur=mc.entityRenderer.getShaderGroup();mc.getFramebuffer().bindFramebuffer(true);if(ownedBlur==null)notice="Desfoque indisponivel nesta configuracao grafica.";}catch(Exception ignored){notice="Desfoque indisponivel nesta configuracao grafica.";}}
 private void stopBlur(){if(ownedBlur!=null&&mc.entityRenderer.getShaderGroup()==ownedBlur)mc.entityRenderer.stopUseShader();ownedBlur=null;}
 public void onGuiClosed(){stopBlur();blurAttempted=false;client.profiles.saveAutomatic();}
 public boolean doesGuiPauseGame(){return false;}
}
