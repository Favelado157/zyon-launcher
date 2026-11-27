package dev.zyon;

import java.io.File;
import java.io.IOException;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import dev.zyon.platform.event.GuiScreenEvent;
import dev.zyon.platform.event.SubscribeEvent;
import org.lwjgl.input.Keyboard;

/** Replaces only the legacy resource-pack folder action, retaining the game's real directory. */
public final class ResourcePackFolders {
 private final Minecraft mc=Minecraft.getMinecraft();
 @SubscribeEvent public void action(GuiScreenEvent.ActionPerformedEvent.Pre event){
  if(!(event.gui instanceof GuiScreenResourcePacks)||event.button.id!=2||!event.button.enabled)return;
  event.setCanceled(true);
  File directory=mc.getResourcePackRepository().getDirResourcepacks();
  try{FolderOpener.open(directory);}catch(IOException|SecurityException failure){
   org.apache.logging.log4j.LogManager.getLogger("Zyon").warn("Nao foi possivel abrir a pasta de texturas: "+directory,failure);
   mc.displayGuiScreen(new Problem(event.gui,directory.getAbsolutePath()));
  }
 }
 private static final class Problem extends GuiScreen {
  private final GuiScreen parent;private final String path;private boolean copied;
  Problem(GuiScreen parent,String path){this.parent=parent;this.path=path;}
  public void initGui(){buttonList.clear();int w=Math.min(260,width-20);buttonList.add(new GuiButton(0,(width-w)/2,height-57,w,20,"Voltar"));buttonList.add(new GuiButton(1,(width-w)/2,height-31,w,20,"Copiar caminho da pasta"));}
  public void drawScreen(int x,int y,float partial){drawDefaultBackground();drawCenteredString(fontRendererObj,"Pasta de texturas",width/2,16,0xFF249BFF);drawCenteredString(fontRendererObj,"Nao foi possivel abrir o gerenciador de arquivos.",width/2,37,0xFFE9F2FF);drawCenteredString(fontRendererObj,"Copie o caminho e cole no Explorador de Arquivos.",width/2,51,0xFFAAC2DC);List<String> lines=fontRendererObj.listFormattedStringToWidth(path,Math.max(100,width-32));for(int i=0;i<lines.size()&&76+i*12<height-77;i++)drawCenteredString(fontRendererObj,lines.get(i),width/2,76+i*12,0xFFD5E5FA);if(copied)drawCenteredString(fontRendererObj,"Caminho copiado.",width/2,height-74,0xFF88DBB0);super.drawScreen(x,y,partial);}
  protected void actionPerformed(GuiButton button){if(button.id==0)mc.displayGuiScreen(parent);if(button.id==1){setClipboardString(path);copied=true;}}
  protected void keyTyped(char c,int key){if(key==Keyboard.KEY_ESCAPE)mc.displayGuiScreen(parent);}
  public boolean doesGuiPauseGame(){return false;}
 }
}
