package dev.zyon;
import java.io.IOException;
import java.util.*;
import net.minecraft.client.gui.*;
import org.lwjgl.input.Keyboard;

public final class AutoTextGui extends GuiScreen {
 private final Zyon client;private final GuiScreen parent;
 private final java.util.List<GuiTextField> names=new ArrayList<GuiTextField>(),texts=new ArrayList<GuiTextField>();
 private int page,pageSize,left,panelWidth;private String status="";
 public AutoTextGui(Zyon client,GuiScreen parent){this.client=client;this.parent=parent;}
 public void initGui(){Keyboard.enableRepeatEvents(true);
  pageSize=Math.min(AutoTextStore.SLOTS,Math.max(1,(height-90)/46));page=Math.min(page,(AutoTextStore.SLOTS-1)/pageSize);panelWidth=Math.min(620,width-16);left=(width-panelWidth)/2;
  names.clear();texts.clear();buttonList.clear();int count=Math.min(pageSize,AutoTextStore.SLOTS-page*pageSize);
  int nameWidth=Math.min(105,panelWidth/4);
  for(int row=0;row<count;row++){int slot=page*pageSize+row,y=59+row*46;GuiTextField name=new GuiTextField(row*2,fontRendererObj,left+8,y,nameWidth,20),message=new GuiTextField(row*2+1,fontRendererObj,left+16+nameWidth,y,panelWidth-nameWidth-86,20);name.setMaxStringLength(16);name.setText(client.autoText.name(slot));message.setMaxStringLength(100);message.setText(client.autoText.message(slot));names.add(name);texts.add(message);GuiButton send=new GuiButton(10+row,left+panelWidth-59,y,51,20,"Enviar");send.enabled=mc.thePlayer!=null;buttonList.add(send);}
  buttonList.add(new GuiButton(0,left+8,height-30,82,20,"Salvar"));buttonList.add(new GuiButton(1,left+96,height-30,90,20,"Voltar"));
  if(pageSize<AutoTextStore.SLOTS){buttonList.add(new GuiButton(2,left+panelWidth-96,height-30,40,20,"<"));buttonList.add(new GuiButton(3,left+panelWidth-50,height-30,40,20,">"));}
 }
 private void capture(){for(int row=0;row<texts.size();row++)client.autoText.set(page*pageSize+row,names.get(row).getText(),texts.get(row).getText());}
 private boolean save(){capture();try{client.autoText.save();status="Mensagens salvas neste PC.";return true;}catch(IOException e){status="Nao foi possivel salvar; suas mensagens continuam nesta tela.";return false;}}
 public void drawScreen(int mouseX,int mouseY,float partial){drawRect(0,0,width,height,0xED0B1423);drawCenteredString(fontRendererObj,"AUTOTEXTO ZYON",width/2,12,client.accent());drawCenteredString(fontRendererObj,"Salve uma mensagem. Enviar manda uma unica vez.",width/2,28,0xFFB4C7DF);for(int row=0;row<texts.size();row++){int y=59+row*46;Ui.box(left,y-4,panelWidth,37,4,0xFF202A3C);names.get(row).drawTextBox();texts.get(row).drawTextBox();fontRendererObj.drawString((page*pageSize+row+1)+" / ate 100 caracteres",left+8,y+25,0xFF8CA6C6);}drawCenteredString(fontRendererObj,fontRendererObj.trimStringToWidth(status,width-20),width/2,height-45,0xFFFFD08A);super.drawScreen(mouseX,mouseY,partial);}
 protected void actionPerformed(GuiButton button){
  if(button.id==0){save();return;}if(button.id==1){if(save())mc.displayGuiScreen(parent);return;}
  if(button.id==2||button.id==3){if(!save())return;int max=(AutoTextStore.SLOTS-1)/pageSize;page=Math.max(0,Math.min(max,page+(button.id==2?-1:1)));initGui();return;}
  if(button.id>=10){int row=button.id-10;capture();String message=client.autoText.message(page*pageSize+row);if(message.trim().isEmpty()){status="Escreva a mensagem antes de enviar.";return;}if(mc.thePlayer==null){status="Abra um mundo ou servidor para enviar.";return;}if(!save())return;sendChatMessage(message);mc.displayGuiScreen(null);}
 }
 protected void mouseClicked(int x,int y,int b)throws IOException{for(GuiTextField field:names)field.mouseClicked(x,y,b);for(GuiTextField field:texts)field.mouseClicked(x,y,b);super.mouseClicked(x,y,b);}
 protected void keyTyped(char c,int key)throws IOException{if(key==Keyboard.KEY_ESCAPE){if(save())mc.displayGuiScreen(parent);return;}if(key==Keyboard.KEY_RETURN||key==Keyboard.KEY_NUMPADENTER){save();return;}if(key==Keyboard.KEY_TAB){java.util.List<GuiTextField> all=new ArrayList<GuiTextField>();for(int i=0;i<names.size();i++){all.add(names.get(i));all.add(texts.get(i));}int next=0;for(int i=0;i<all.size();i++)if(all.get(i).isFocused()){next=(i+1)%all.size();break;}for(int i=0;i<all.size();i++)all.get(i).setFocused(i==next);return;}for(GuiTextField field:names)field.textboxKeyTyped(c,key);for(GuiTextField field:texts)field.textboxKeyTyped(c,key);}
 public void onGuiClosed(){Keyboard.enableRepeatEvents(false);capture();try{client.autoText.save();}catch(IOException e){org.apache.logging.log4j.LogManager.getLogger("Zyon").warn("Falha ao salvar autotexto.");}}
 public boolean doesGuiPauseGame(){return false;}
}
