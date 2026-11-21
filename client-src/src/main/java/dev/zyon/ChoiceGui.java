package dev.zyon;
import java.io.IOException;
import net.minecraft.client.gui.*;
import org.lwjgl.input.Keyboard;
public final class ChoiceGui extends GuiScreen {
 private final Zyon client;private final GuiScreen parent;private final Setting setting;
 public ChoiceGui(Zyon c,GuiScreen p,Setting s){client=c;parent=p;setting=s;}
 public void initGui(){buttonList.clear();int top=height/2-setting.choices.length*12;for(int i=0;i<setting.choices.length;i++)buttonList.add(new GuiButton(i,width/2-130,top+i*24,260,20,(i==setting.integer()?"> ":"")+setting.choices[i]));buttonList.add(new GuiButton(999,width/2-130,top+setting.choices.length*24+8,260,20,"Voltar"));}
 public void drawScreen(int x,int y,float p){drawRect(0,0,width,height,0xED101522);drawCenteredString(fontRendererObj,setting.label,width/2,height/2-setting.choices.length*12-27,client.accent());super.drawScreen(x,y,p);}
 protected void actionPerformed(GuiButton b){if(b.id!=999){setting.set(b.id);client.profiles.saveAutomatic();}mc.displayGuiScreen(parent);}
 protected void keyTyped(char c,int key)throws IOException{if(key==Keyboard.KEY_ESCAPE)mc.displayGuiScreen(parent);}
 public boolean doesGuiPauseGame(){return false;}
}
