package dev.zyon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.input.Mouse;

public final class ChatAutoText {
 private final Zyon client;private final Minecraft mc=Minecraft.getMinecraft();
 public ChatAutoText(Zyon c){client=c;}
 @SubscribeEvent public void draw(GuiScreenEvent.DrawScreenEvent.Post event){if(!(event.gui instanceof GuiChat)||!client.feature("autotext").enabled())return;int x=event.gui.width-32,y=event.gui.height-38;boolean hover=Ui.hit(event.mouseX,event.mouseY,x,y,26,20);Ui.box(x,y,26,20,4,hover?client.accent():0xE01A3450);mc.fontRendererObj.drawStringWithShadow("AT",x+7,y+6,0xFFFFFFFF);if(hover)mc.fontRendererObj.drawStringWithShadow("Autotexto",Math.max(2,x-66),y+6,0xFFFFFFFF);}
 @SubscribeEvent public void mouse(GuiScreenEvent.MouseInputEvent.Pre event){if(!(event.gui instanceof GuiChat)||!client.feature("autotext").enabled()||Mouse.getEventButton()!=0||!Mouse.getEventButtonState())return;int x=Mouse.getEventX()*event.gui.width/mc.displayWidth,y=event.gui.height-Mouse.getEventY()*event.gui.height/mc.displayHeight-1;if(!Ui.hit(x,y,event.gui.width-32,event.gui.height-38,26,20))return;event.setCanceled(true);String draft="";try{GuiTextField input=(GuiTextField)ReflectionHelper.findField(GuiChat.class,"inputField","field_146415_a").get(event.gui);draft=input.getText();}catch(Exception ignored){}mc.displayGuiScreen(new AutoTextGui(client,new GuiChat(draft)));}
}
