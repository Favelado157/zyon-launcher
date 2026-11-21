package dev.zyon;
import java.io.IOException;
import net.minecraft.client.gui.*;
import org.lwjgl.input.Keyboard;
public final class ProfileGui extends GuiScreen {
 private final Zyon client;private final GuiScreen parent;private GuiTextField name;private String status="";
 public ProfileGui(Zyon c,GuiScreen p){client=c;parent=p;}
 public void initGui(){name=new GuiTextField(0,fontRendererObj,width/2-140,height/2-40,280,22);name.setMaxStringLength(32);name.setText("meu_perfil");name.setFocused(true);buttonList.clear();buttonList.add(new GuiButton(0,width/2-140,height/2,135,20,"Salvar perfil"));buttonList.add(new GuiButton(1,width/2+5,height/2,135,20,"Carregar perfil"));buttonList.add(new GuiButton(2,width/2-140,height/2+28,280,20,"Voltar"));}
 public void drawScreen(int x,int y,float p){drawRect(0,0,width,height,0xEE101522);drawCenteredString(fontRendererObj,"PERFIS ZYON",width/2,height/2-80,client.accent());name.drawTextBox();drawCenteredString(fontRendererObj,fontRendererObj.trimStringToWidth(status,300),width/2,height/2+66,0xFFD4DFEF);drawCenteredString(fontRendererObj,fontRendererObj.trimStringToWidth("Salvos: "+client.profiles.list(),360),width/2,height/2+86,0xFF91A2BC);super.drawScreen(x,y,p);}
 protected void actionPerformed(GuiButton b){try{if(b.id==0){client.profiles.save(name.getText());status="Perfil salvo.";}if(b.id==1){client.profiles.load(name.getText());client.profiles.saveAutomatic();status="Perfil carregado.";}if(b.id==2)mc.displayGuiScreen(parent);}catch(Exception e){status=e.getMessage();}}
 protected void mouseClicked(int x,int y,int b)throws IOException{name.mouseClicked(x,y,b);super.mouseClicked(x,y,b);}
 protected void keyTyped(char c,int k)throws IOException{if(k==Keyboard.KEY_ESCAPE)mc.displayGuiScreen(parent);else name.textboxKeyTyped(c,k);}
 public boolean doesGuiPauseGame(){return false;}
}
