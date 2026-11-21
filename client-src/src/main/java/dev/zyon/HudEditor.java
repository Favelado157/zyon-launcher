package dev.zyon;
import java.io.IOException;
import net.minecraft.client.gui.*;
import org.lwjgl.input.Keyboard;
public final class HudEditor extends GuiScreen {
 private final Zyon client;private final GuiScreen parent;private final Widgets widgets;private Feature dragging;private int dx,dy;
 public HudEditor(Zyon c,GuiScreen p){client=c;parent=p;widgets=new Widgets(c);}
 public void initGui(){buttonList.clear();buttonList.add(new GuiButton(0,width/2-145,28,100,20,"Concluir"));buttonList.add(new GuiButton(1,width/2-40,28,185,20,"Adicionar / configurar widgets"));}
 private int[] bounds(Feature f){return f.id.equals("keystrokes")?client.keystrokes.bounds(width,height):widgets.bounds(f,width,height);}
 public void drawScreen(int x,int y,float p){drawRect(0,0,width,height,0x55050A15);drawCenteredString(fontRendererObj,"EDITOR DE WIDGETS  /  Arraste para mover",width/2,12,client.accent());drawRect(width/2,54,width/2+1,height,0x30249BFF);drawRect(0,height/2,width,height/2+1,0x30249BFF);for(Feature f:client.features)if(Catalog.widget(f)&&f.enabled()){int[] b=bounds(f);Ui.box(b[0]-3,b[1]-3,b[2]+6,b[3]+6,3,f==dragging?0xCC249BFF:0x80445566);if(f.id.equals("keystrokes"))client.keystrokes.draw(width,height,true);else widgets.draw(f,width,height,true);if(Ui.hit(x,y,b[0],b[1],b[2],b[3]))fontRendererObj.drawStringWithShadow(f.name,b[0],Math.max(53,b[1]-13),0xFFFFFFFF);}super.drawScreen(x,y,p);}
 protected void actionPerformed(GuiButton b){if(b.id==0)mc.displayGuiScreen(parent);else mc.displayGuiScreen(parent==null?new ClickGui(client):parent);}
 protected void mouseClicked(int x,int y,int b)throws IOException{for(GuiButton button:buttonList)if(button.mousePressed(mc,x,y)){super.mouseClicked(x,y,b);return;}if(b==0)for(int i=client.features.size()-1;i>=0;i--){Feature f=client.features.get(i);if(!Catalog.widget(f)||!f.enabled())continue;int[] bb=bounds(f);if(Ui.hit(x,y,bb[0],bb[1],bb[2],bb[3])){dragging=f;dx=x-bb[0];dy=y-bb[1];return;}}}
 protected void mouseClickMove(int x,int y,int b,long t){if(dragging==null)return;if(dragging.id.equals("keystrokes"))client.keystrokes.move(x-dx,y-dy,width,height);else widgets.move(dragging,x-dx,y-dy,width,height);}
 protected void mouseReleased(int x,int y,int b){dragging=null;client.profiles.saveAutomatic();}
 protected void keyTyped(char c,int key)throws IOException{if(key==Keyboard.KEY_ESCAPE||key==client.editorKey.getKeyCode())mc.displayGuiScreen(parent);}
 public void onGuiClosed(){client.profiles.saveAutomatic();}public boolean doesGuiPauseGame(){return false;}
}
