// Java / ClickGui.java / Java 8 / Forge 1.8.9
package dev.zyon;
import net.minecraft.client.gui.*;
import org.lwjgl.input.*;
import org.lwjgl.opengl.GL11;
import java.io.IOException;
import java.util.*;
public final class ClickGui extends GuiScreen {
 private static final int W=740,H=430,TEXT=0xFFE9EBF5,MUTED=0xFF929BB4;
 private final Zyon client;private Feature.Category category=Feature.Category.COMBAT;private Feature selected;
 private GuiTextField search,profile;private boolean profilesPage,binding;private int listScroll,settingsScroll,profileScroll,originX,originY;private float scale;private Setting dragging;private String status="";
 public ClickGui(Zyon client){this.client=client;selected=client.feature("killaura");}
 public void initGui(){
  Keyboard.enableRepeatEvents(true);String oldSearch=search==null?"":search.getText(),oldProfile=profile==null?"principal":profile.getText();
  scale=Math.min(1.5f,Math.min((width-16)/(float)W,(height-16)/(float)H));originX=(int)((width-W*scale)/2);originY=(int)((height-H*scale)/2);
  search=new GuiTextField(1,fontRendererObj,495,24,210,18);search.setMaxStringLength(48);search.setEnableBackgroundDrawing(false);search.setTextColor(TEXT);search.setText(oldSearch);
  profile=new GuiTextField(2,fontRendererObj,191,109,250,18);profile.setMaxStringLength(32);profile.setEnableBackgroundDrawing(false);profile.setTextColor(TEXT);profile.setText(oldProfile);
 }
 private int mx(int x){return(int)((x-originX)/scale);}private int my(int y){return(int)((y-originY)/scale);}
 private boolean hit(int x,int y,int left,int top,int w,int h){return x>=left&&x<left+w&&y>=top&&y<top+h;}
 private void label(String s,int x,int y,int c){fontRendererObj.drawString(s,x,y,c);}
 private void card(int x,int y,int w,int h,int c){drawRect(x+3,y,x+w-3,y+h,c);drawRect(x,y+3,x+w,y+h-3,c);drawRect(x+1,y+1,x+w-1,y+h-1,c);}
 private void button(String s,int x,int y,int w,int h,int mouseX,int mouseY,boolean highlight){card(x,y,w,h,highlight?client.accent():hit(mouseX,mouseY,x,y,w,h)?0xFF343B52:0xFF272D40);label(s,x+(w-fontRendererObj.getStringWidth(s))/2,y+(h-8)/2,highlight?0xFF141725:TEXT);}
 public void drawScreen(int mouseX,int mouseY,float partial){
  drawRect(0,0,width,height,0x99080B13);int x=mx(mouseX),y=my(mouseY);if(dragging!=null)updateSlider(x);
  GL11.glPushMatrix();GL11.glTranslatef(originX,originY,0);GL11.glScalef(scale,scale,1);try{
   int opacity=client.feature("theme").setting("opacity").integer();card(0,0,W,H,(opacity<<24)|0x111520);card(0,0,154,H,(opacity<<24)|0x171B29);drawRect(153,10,154,H-10,0xFF2B3146);
   label("Z Y O N",24,25,client.accent());label("CLIENT / 1.8.9",24,44,MUTED);label("CATEGORIAS",24,76,0xFF606B86);
   for(Feature.Category c:Feature.Category.values()){int row=97+c.ordinal()*36;boolean active=!profilesPage&&c==category;if(active){card(14,row-8,126,30,0xFF2A2D43);drawRect(14,row-2,16,row+13,client.accent());}label(c.label,27,row+2,active?client.accent():MUTED);}
   drawRect(24,282,130,283,0xFF2B3146);button("Perfis",18,300,118,28,x,y,profilesPage);button("Desligar tudo",18,367,118,28,x,y,false);label("END: desligar",24,405,0xFF68748F);
   label(profilesPage?"Seus perfis":category.label,174,21,TEXT);label(profilesPage?"Configuracoes salvas neste computador":"Seu cliente, do seu jeito.",174,40,MUTED);
   if(profilesPage)drawProfiles(x,y);else{card(480,17,240,32,0xFF222838);if(search.getText().isEmpty()&&!search.isFocused())label("Buscar modulos...",495,28,MUTED);search.drawTextBox();drawModules(x,y);drawInspector(x,y);}
   drawRect(174,390,720,391,0xFF2B3146);int count=0;for(Feature f:client.features)if(f.enabled())count++;
   String footer=status.isEmpty()?count+" ativos  |  Shift direito: menu  |  ESC: fechar":status;label(fontRendererObj.trimStringToWidth(footer,540),174,405,status.isEmpty()?MUTED:client.accent());
  }finally{GL11.glPopMatrix();}
 }
 private List<Feature> visible(){List<Feature> list=new ArrayList<Feature>();String q=search.getText().trim().toLowerCase(Locale.ROOT);for(Feature f:client.features)if(q.isEmpty()?f.category==category:(f.name+" "+f.description).toLowerCase(Locale.ROOT).contains(q))list.add(f);return list;}
 private void drawModules(int x,int y){
  List<Feature> list=visible();listScroll=Math.max(0,Math.min(listScroll,Math.max(0,list.size()*48-306)));clip(174,70,218,306);
  try{for(int i=0;i<list.size();i++){Feature f=list.get(i);int top=70+i*48-listScroll;if(top+42<70||top>=376)continue;
   card(174,top,218,42,selected==f?0xFF2B3048:hit(x,y,174,top,218,42)?0xFF242B3E:0xFF1D2333);if(selected==f)drawRect(174,top+10,176,top+32,client.accent());label(f.name,187,top+9,f.enabled()?client.accent():TEXT);
   label(f.id.equals("theme")?"Cores e aparencia":f.key==0?"Sem atalho":"Tecla: "+Keyboard.getKeyName(f.key),187,top+25,MUTED);
   if(!f.id.equals("theme")){card(350,top+14,28,14,f.enabled()?client.accent():0xFF434B62);card(f.enabled()?365:352,top+16,10,10,0xFFF0F0FA);}
  }if(list.isEmpty())label("Nenhum modulo encontrado.",184,86,MUTED);}finally{unclip();}
 }
 private void drawInspector(int x,int y){
  card(408,70,312,306,0xFF1C2232);label(selected.name,424,85,TEXT);if(!selected.id.equals("theme"))button(selected.enabled()?"ON":"OFF",654,79,50,22,x,y,selected.enabled());
  List<String> desc=fontRendererObj.listFormattedStringToWidth(selected.description,276);for(int i=0;i<Math.min(2,desc.size());i++)label(desc.get(i),424,109+i*11,MUTED);
  label("Atalho",424,146,MUTED);if(selected.id.equals("theme"))label("Sempre aplicado",580,146,MUTED);else button(binding?"Pressione tecla":selected.key==0?"Definir":Keyboard.getKeyName(selected.key),570,137,134,24,x,y,binding);
  drawRect(424,169,704,170,0xFF31384D);settingsScroll=Math.max(0,Math.min(settingsScroll,Math.max(0,selected.settings.size()*46-188)));clip(424,179,280,188);
  try{if(selected.settings.isEmpty())label("Somente ativacao e atalho.",424,191,MUTED);for(int i=0;i<selected.settings.size();i++){Setting s=selected.settings.get(i);int top=182+i*46-settingsScroll;label(s.label,424,top,TEXT);String value=s.display();label(value,701-fontRendererObj.getStringWidth(value),top,client.accent());
   if(s.kind==Setting.Kind.NUMBER){drawRect(426,top+24,700,top+27,0xFF38415A);int thumb=426+(int)(274*(s.get()-s.min)/(s.max-s.min));drawRect(426,top+24,thumb,top+27,client.accent());card(thumb-3,top+21,6,9,0xFFF3F1FF);}
   else{card(426,top+20,274,15,0xFF282F43);label(s.kind==Setting.Kind.BOOLEAN?"Clique para alternar":"Clique para mudar",433,top+24,MUTED);label(s.kind==Setting.Kind.BOOLEAN?(s.on()?"[x]":"[ ]"):">",680,top+24,client.accent());}
  }}finally{unclip();}
  if(selected.settings.size()*46>188){int content=selected.settings.size()*46,barH=Math.max(16,188*188/content),barY=179+(188-barH)*settingsScroll/(content-188);drawRect(712,barY,714,barY+barH,client.accent());}
 }
 private void drawProfiles(int x,int y){
  card(174,70,546,306,0xFF1C2232);label("Nome do perfil",190,85,MUTED);card(184,102,273,28,0xFF2B3247);profile.drawTextBox();button("Salvar",470,102,108,28,x,y,true);button("Carregar",590,102,112,28,x,y,false);label("PERFIS DISPONIVEIS",190,150,MUTED);
  List<String> names=client.profiles.list();profileScroll=Math.max(0,Math.min(profileScroll,Math.max(0,names.size()*30-190)));clip(184,171,522,190);try{for(int i=0;i<names.size();i++){int top=173+i*30-profileScroll;if(top+25<171||top>361)continue;button(names.get(i),184,top,522,25,x,y,names.get(i).equalsIgnoreCase(profile.getText()));}}finally{unclip();}
 }
 protected void mouseClicked(int mouseX,int mouseY,int button)throws IOException{
  if(button!=0)return;int x=mx(mouseX),y=my(mouseY);binding=false;
  if(hit(x,y,18,367,118,28)){client.panic();status="Todos os modulos desligados.";return;}
  if(hit(x,y,18,300,118,28)){profilesPage=true;search.setFocused(false);return;}
  for(Feature.Category c:Feature.Category.values())if(hit(x,y,14,89+c.ordinal()*36,126,30)){category=c;profilesPage=false;search.setText("");search.setFocused(false);profile.setFocused(false);listScroll=settingsScroll=0;for(Feature f:client.features)if(f.category==c){selected=f;break;}return;}
  if(profilesPage){profileClick(x,y);return;}search.mouseClicked(x,y,button);
  if(hit(x,y,174,70,218,306)){List<Feature> list=visible();int index=(y-70+listScroll)/48;if(index<list.size()&&(y-70+listScroll)%48<42){Feature f=list.get(index);if(selected!=f)settingsScroll=0;selected=f;if(x>=345&&!f.id.equals("theme")){f.toggle();client.profiles.saveAutomatic();}}return;}
  if(!selected.id.equals("theme")&&hit(x,y,654,79,50,22)){selected.toggle();client.profiles.saveAutomatic();return;}
  if(!selected.id.equals("theme")&&hit(x,y,570,137,134,24)){binding=true;search.setFocused(false);status="Escolha uma tecla. DELETE: remover; ESC: cancelar.";return;}
  if(hit(x,y,424,179,280,188)){int index=(y-182+settingsScroll)/46;if(index>=0&&index<selected.settings.size()){Setting s=selected.settings.get(index);if(s.kind==Setting.Kind.NUMBER){dragging=s;updateSlider(x);}else{s.cycle();client.profiles.saveAutomatic();}}}
 }
 private void profileClick(int x,int y){profile.mouseClicked(x,y,0);try{
  if(hit(x,y,470,102,108,28)){client.profiles.save(profile.getText());status="Perfil salvo: "+profile.getText();}
  else if(hit(x,y,590,102,112,28)){client.profiles.load(profile.getText());client.profiles.saveAutomatic();status="Perfil carregado: "+profile.getText();}
  else if(hit(x,y,184,171,522,190)){int index=(y-173+profileScroll)/30;List<String> names=client.profiles.list();if(index>=0&&index<names.size()){profile.setText(names.get(index));profile.setFocused(false);}}
 }catch(Exception e){status=e.getMessage()==null?"Erro no perfil; veja latest.log.":e.getMessage();}}
 private void updateSlider(int x){double fraction=Math.max(0,Math.min(1,(x-426)/274.0));dragging.set(dragging.min+fraction*(dragging.max-dragging.min));}
 protected void mouseReleased(int mouseX,int mouseY,int button){if(dragging!=null){dragging=null;client.profiles.saveAutomatic();}}
 public void handleMouseInput()throws IOException{super.handleMouseInput();int wheel=Mouse.getEventDWheel();if(wheel==0)return;int x=mx(Mouse.getEventX()*width/mc.displayWidth),y=my(height-Mouse.getEventY()*height/mc.displayHeight-1),delta=wheel>0?-28:28;if(profilesPage&&hit(x,y,184,171,522,190))profileScroll+=delta;else if(hit(x,y,174,70,218,306))listScroll+=delta;else if(hit(x,y,408,170,312,206))settingsScroll+=delta;}
 protected void keyTyped(char typed,int key)throws IOException{
  if(!binding&&key==client.panicKey.getKeyCode()){client.panic();status="Todos os modulos desligados.";return;}
  if(binding){if(key==Keyboard.KEY_ESCAPE){binding=false;status="Atalho mantido.";return;}if(key==client.menuKey.getKeyCode()||key==client.panicKey.getKeyCode()){status="Tecla reservada.";return;}selected.key=key==Keyboard.KEY_DELETE||key==Keyboard.KEY_BACK?0:key;binding=false;status="Atalho atualizado: "+selected.name;client.profiles.saveAutomatic();return;}
  if(key==Keyboard.KEY_ESCAPE||key==client.menuKey.getKeyCode()){mc.displayGuiScreen(null);return;}if(profilesPage)profile.textboxKeyTyped(typed,key);else if(search.textboxKeyTyped(typed,key))listScroll=0;
 }
 public void updateScreen(){search.updateCursorCounter();profile.updateCursorCounter();}public void onGuiClosed(){Keyboard.enableRepeatEvents(false);client.profiles.saveAutomatic();}public boolean doesGuiPauseGame(){return false;}
 private void clip(int x,int y,int w,int h){GL11.glPushAttrib(GL11.GL_SCISSOR_BIT);GL11.glEnable(GL11.GL_SCISSOR_TEST);double factor=mc.displayWidth/(double)width;GL11.glScissor((int)((originX+x*scale)*factor),mc.displayHeight-(int)((originY+(y+h)*scale)*factor),Math.max(0,(int)(w*scale*factor)),Math.max(0,(int)(h*scale*factor)));}private void unclip(){GL11.glPopAttrib();}
}
