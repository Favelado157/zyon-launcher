package dev.zyon;
import java.io.IOException;
import java.util.*;
import net.minecraft.client.gui.*;
import org.lwjgl.input.Keyboard;

public final class AutoTextGui extends GuiScreen {
 private final Zyon client;private final GuiScreen parent;private final List<GuiTextField> texts=new ArrayList<GuiTextField>();
 private int page,pageSize,firstSlot,left,panelWidth,listening=-1;private String status="";
 public AutoTextGui(Zyon c,GuiScreen p){client=c;parent=p;}
 public void initGui(){Keyboard.enableRepeatEvents(true);capture();pageSize=Math.min(AutoTextStore.SLOTS,Math.max(1,(height-90)/46));page=Math.min(page,(AutoTextStore.SLOTS-1)/pageSize);firstSlot=page*pageSize;panelWidth=Math.min(620,width-16);left=(width-panelWidth)/2;texts.clear();buttonList.clear();int keyWidth=Math.min(105,panelWidth/4),count=Math.min(pageSize,AutoTextStore.SLOTS-firstSlot);
  for(int row=0;row<count;row++){int slot=firstSlot+row,y=59+row*46;buttonList.add(new GuiButton(100+row,left+8,y,keyWidth,20,keyLabel(slot)));GuiTextField message=new GuiTextField(row,fontRendererObj,left+16+keyWidth,y,panelWidth-keyWidth-86,20);message.setMaxStringLength(100);message.setText(client.autoText.message(slot));texts.add(message);GuiButton send=new GuiButton(10+row,left+panelWidth-59,y,51,20,"Enviar");send.enabled=mc.thePlayer!=null;buttonList.add(send);}
  buttonList.add(new GuiButton(0,left+8,height-30,82,20,"Salvar"));buttonList.add(new GuiButton(1,left+96,height-30,90,20,"Voltar"));if(pageSize<AutoTextStore.SLOTS){buttonList.add(new GuiButton(2,left+panelWidth-96,height-30,40,20,"<"));buttonList.add(new GuiButton(3,left+panelWidth-50,height-30,40,20,">"));}
 }
 private String keyLabel(int slot){return listening==slot?"Pressione...":client.autoText.key(slot)==0?"Definir tecla":"Tecla: "+Keyboard.getKeyName(client.autoText.key(slot));}
 private void capture(){for(int row=0;row<texts.size();row++)client.autoText.setMessage(firstSlot+row,texts.get(row).getText());}
 private boolean save(){capture();try{client.autoText.save();status="Mensagens e teclas salvas neste PC.";return true;}catch(IOException e){status="Nao foi possivel salvar; tente novamente.";return false;}}
 public void drawScreen(int x,int y,float partial){drawRect(0,0,width,height,0xED0B1423);drawCenteredString(fontRendererObj,"AUTOTEXTO ZYON",width/2,12,client.accent());drawCenteredString(fontRendererObj,listening<0?"Escolha uma tecla para enviar a mensagem durante o jogo.":"Pressione a tecla / Del remove / Esc cancela",width/2,28,0xFFB4C7DF);for(int row=0;row<texts.size();row++){int yy=59+row*46;Ui.box(left,yy-4,panelWidth,37,4,0xFF202A3C);texts.get(row).drawTextBox();fontRendererObj.drawString((firstSlot+row+1)+" / ate 100 caracteres",left+8,yy+25,0xFF8CA6C6);}for(GuiButton b:buttonList)if(b.id>=100)b.displayString=keyLabel(firstSlot+b.id-100);drawCenteredString(fontRendererObj,fontRendererObj.trimStringToWidth(status,width-20),width/2,height-45,0xFFFFD08A);super.drawScreen(x,y,partial);}
 protected void actionPerformed(GuiButton button){
  if(button.id>=100){listening=firstSlot+button.id-100;for(GuiTextField field:texts)field.setFocused(false);status="";return;}listening=-1;
  if(button.id==0){save();return;}if(button.id==1){if(save())mc.displayGuiScreen(parent);return;}
  if(button.id==2||button.id==3){if(!save())return;page=Math.max(0,Math.min((AutoTextStore.SLOTS-1)/pageSize,page+(button.id==2?-1:1)));initGui();return;}
  if(button.id>=10){capture();String message=client.autoText.message(firstSlot+button.id-10);if(message.trim().isEmpty()){status="Escreva a mensagem antes de enviar.";return;}if(mc.thePlayer==null)return;if(save()){sendChatMessage(message);mc.displayGuiScreen(null);}}
 }
 protected void mouseClicked(int x,int y,int b){for(GuiTextField field:texts)field.mouseClicked(x,y,b);super.mouseClicked(x,y,b);}
 protected void keyTyped(char c,int key){
  if(listening>=0){if(key==Keyboard.KEY_ESCAPE){listening=-1;return;}if(Keyboard.isRepeatEvent()||key==0)return;int code=key==Keyboard.KEY_DELETE||key==Keyboard.KEY_BACK?0:key;if(code!=0&&AutoTextHotkeys.reserved(client,code)){status="Essa tecla ja esta em uso nos controles do jogo ou Zyon.";return;}try{client.autoText.setKey(listening,code);listening=-1;save();}catch(IllegalArgumentException e){status=e.getMessage();}return;}
  if(key==Keyboard.KEY_ESCAPE){if(save())mc.displayGuiScreen(parent);return;}if(key==Keyboard.KEY_RETURN||key==Keyboard.KEY_NUMPADENTER){save();return;}if(key==Keyboard.KEY_TAB){int next=0;for(int i=0;i<texts.size();i++)if(texts.get(i).isFocused()){next=(i+1)%texts.size();break;}for(int i=0;i<texts.size();i++)texts.get(i).setFocused(i==next);return;}for(GuiTextField field:texts)field.textboxKeyTyped(c,key);
 }
 public void updateScreen(){for(GuiTextField field:texts)field.updateCursorCounter();}
 public void onGuiClosed(){Keyboard.enableRepeatEvents(false);capture();try{client.autoText.save();}catch(IOException e){org.apache.logging.log4j.LogManager.getLogger("Zyon").warn("Falha ao salvar autotexto.");}}
 public boolean doesGuiPauseGame(){return false;}
}
