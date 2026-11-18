package dev.zyon;

import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

public final class HudEditor extends GuiScreen {
 private final Zyon client;private final GuiScreen parent;private boolean dragging;private int offsetX,offsetY;
 public HudEditor(Zyon client,GuiScreen parent){this.client=client;this.parent=parent;}
 public void initGui(){
  dragging=false;buttonList.clear();int x=width/2-155;
  buttonList.add(new GuiButton(0,x,43,80,20,"Concluir"));
  buttonList.add(new GuiButton(1,x+85,43,110,20,label()));
  buttonList.add(new GuiButton(2,x+200,43,110,20,"Restaurar posicao"));
 }
 private String label(){return "Keystrokes: "+(client.feature("keystrokes").enabled()?"ON":"OFF");}
 public void drawScreen(int x,int y,float partial){
  drawRect(0,0,width,height,0x9905101D);
  drawCenteredString(fontRendererObj,"EDITOR DO HUD",width/2,10,client.accent());
  drawCenteredString(fontRendererObj,"Arraste os keystrokes. Tamanho, cores e teclas: Interface > Keystrokes.",width/2,25,0xFFE1E4EE);
  int[] b=client.keystrokes.bounds(width,height);
  drawRect(b[0]-2,b[1]-2,b[0]+b[2]+2,b[1]+b[3]+2,client.accent());
  drawRect(b[0]-1,b[1]-1,b[0]+b[2]+1,b[1]+b[3]+1,0xFF091B2E);
  client.keystrokes.draw(width,height,true);
  if(!client.feature("keystrokes").enabled())drawCenteredString(fontRendererObj,"Previa: ative Keystrokes para mostrar durante o jogo.",width/2,height-18,0xFFFFD08A);
  super.drawScreen(x,y,partial);
 }
 protected void actionPerformed(GuiButton button){
  dragging=false;
  if(button.id==0)mc.displayGuiScreen(parent);
  if(button.id==1){client.feature("keystrokes").toggle();button.displayString=label();}
  if(button.id==2)client.keystrokes.reset();
  client.profiles.saveAutomatic();
 }
 protected void mouseClicked(int x,int y,int button)throws IOException{
  // Toolbar controls take priority when a widget is positioned behind them.
  for(GuiButton control:buttonList)if(control.mousePressed(mc,x,y)){super.mouseClicked(x,y,button);return;}
  int[] b=client.keystrokes.bounds(width,height);
  if(button==0&&x>=b[0]&&x<b[0]+b[2]&&y>=b[1]&&y<b[1]+b[3]){dragging=true;offsetX=x-b[0];offsetY=y-b[1];return;}
  super.mouseClicked(x,y,button);
 }
 protected void mouseClickMove(int x,int y,int button,long time){if(dragging&&button==0)client.keystrokes.move(x-offsetX,y-offsetY,width,height);}
 protected void mouseReleased(int x,int y,int button){if(button==0&&dragging){dragging=false;client.profiles.saveAutomatic();}super.mouseReleased(x,y,button);}
 protected void keyTyped(char typed,int key)throws IOException{if(key==Keyboard.KEY_ESCAPE||key==client.editorKey.getKeyCode())mc.displayGuiScreen(parent);else super.keyTyped(typed,key);}
 public void onGuiClosed(){client.profiles.saveAutomatic();}
 public boolean doesGuiPauseGame(){return false;}
}
