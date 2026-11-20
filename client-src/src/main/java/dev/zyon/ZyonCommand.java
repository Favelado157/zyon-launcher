// Java / ZyonCommand.java / Java 8 / Forge 1.8.9
package dev.zyon;
import net.minecraft.command.*;
import net.minecraft.util.BlockPos;
import org.lwjgl.input.Keyboard;
import java.util.*;
public final class ZyonCommand extends CommandBase {
 private final Zyon client;public ZyonCommand(Zyon client){this.client=client;}
 public String getCommandName(){return "zyon";}public String getCommandUsage(ICommandSender sender){return "/zyon [gui|toggle modulo|bind modulo tecla|profile save/load nome|profile list|panic]";}
 public int getRequiredPermissionLevel(){return 0;}public boolean canCommandSenderUseCommand(ICommandSender sender){return true;}
 public void processCommand(ICommandSender sender,String[] args){try{
  if(args.length==0||args[0].equalsIgnoreCase("gui")){client.requestMenu=true;return;}
  if(args[0].equalsIgnoreCase("panic")){client.panic();return;}
  if(args[0].equalsIgnoreCase("toggle")&&args.length==2){Feature f=client.feature(args[1]);if(f.id.equals("theme"))throw new IllegalArgumentException("O tema e aplicado diretamente.");f.toggle();client.profiles.saveAutomatic();client.message(f.name+": "+(f.enabled()?"ON":"OFF"));return;}
  if(args[0].equalsIgnoreCase("bind")&&args.length==3){Feature f=client.feature(args[1]);int key=args[2].equalsIgnoreCase("none")?0:Keyboard.getKeyIndex(args[2].toUpperCase(Locale.ROOT));if(key==0&&!args[2].equalsIgnoreCase("none"))throw new IllegalArgumentException("Tecla desconhecida.");if(key==client.menuKey.getKeyCode()||key==client.panicKey.getKeyCode()||key==client.editorKey.getKeyCode()||key==client.zoomKey.getKeyCode())throw new IllegalArgumentException("Tecla reservada.");if(f.id.equals("theme"))throw new IllegalArgumentException("O tema nao tem atalho.");f.key=key;client.profiles.saveAutomatic();client.message("Atalho atualizado: "+f.name);return;}
  if(args[0].equalsIgnoreCase("profile")){
   if(args.length==2&&args[1].equalsIgnoreCase("list")){client.message("Perfis: "+client.profiles.list());return;}
   if(args.length==3&&args[1].equalsIgnoreCase("save")){client.profiles.save(args[2]);client.message("Perfil salvo: "+args[2]);return;}
   if(args.length==3&&args[1].equalsIgnoreCase("load")){client.profiles.load(args[2]);client.profiles.saveAutomatic();client.message("Perfil carregado: "+args[2]);return;}
  }client.message(getCommandUsage(sender));
 }catch(Exception e){client.message(e.getMessage()==null?"Erro; veja latest.log.":e.getMessage());}}
 public List<String> addTabCompletionOptions(ICommandSender sender,String[] args,BlockPos pos){
  if(args.length==1)return getListOfStringsMatchingLastWord(args,"gui","toggle","bind","profile","panic");
  if(args.length==2&&(args[0].equalsIgnoreCase("toggle")||args[0].equalsIgnoreCase("bind"))){List<String> ids=new ArrayList<String>();for(Feature f:client.features)if(!f.id.equals("theme"))ids.add(f.id);return getListOfStringsMatchingLastWord(args,ids.toArray(new String[0]));}
  if(args.length==2&&args[0].equalsIgnoreCase("profile"))return getListOfStringsMatchingLastWord(args,"save","load","list");
  if(args.length==3&&args[0].equalsIgnoreCase("profile")&&args[1].equalsIgnoreCase("load"))return getListOfStringsMatchingLastWord(args,client.profiles.list().toArray(new String[0]));return Collections.emptyList();
 }
}
