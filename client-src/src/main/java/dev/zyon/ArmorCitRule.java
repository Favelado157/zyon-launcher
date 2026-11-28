package dev.zyon;
import java.util.*;
import net.minecraft.item.*;
import net.minecraft.util.ResourceLocation;
/** Durability-based CIT rules for worn armor and its inventory/HUD icons. */
public final class ArmorCitRule {
 public final ResourceLocation source;public final boolean armor;public final int priority,weight;public final Set<Item> items;
 public final Map<String,ResourceLocation> textures;private final Range damage,count;private final boolean percent;private final int mask;
 public ArmorCitRule(ResourceLocation source,Properties p,int priority){
  this.source=source;this.priority=priority;String type=p.getProperty("type","item");if(!type.equals("armor")&&!type.equals("item"))throw new IllegalArgumentException("Unsupported CIT type");armor=type.equals("armor");
  for(String key:p.stringPropertyNames())if(key.startsWith("nbt.")||key.startsWith("enchantment")||key.equals("model")||key.equals("hand"))throw new IllegalArgumentException("Not a durability armor rule");
  String names=p.getProperty("items",p.getProperty("matchItems",basename(source.getResourcePath()))).trim();Set<Item> resolved=new HashSet<Item>();
  for(String name:names.split("\\s+")){if(name.matches("[0-9]+-[0-9]+")){String[] ends=name.split("-");int min=Integer.parseInt(ends[0]),max=Integer.parseInt(ends[1]);if(min<0||max-min>512)throw new IllegalArgumentException("Invalid item interval");for(int id=min;id<=max;id++){Item i=Item.getItemById(id);if(i instanceof ItemArmor)resolved.add(i);}}else{Item i=Item.getByNameOrId(name);if(i instanceof ItemArmor)resolved.add(i);}}
  if(resolved.isEmpty())throw new IllegalArgumentException("Not an armor rule");items=Collections.unmodifiableSet(resolved);
  String condition=p.getProperty("damage","");percent=condition.contains("%");damage=new Range(condition.replace("%",""));count=new Range(p.getProperty("stackSize",""));mask=Integer.decode(p.getProperty("damageMask","-1"));weight=Integer.parseInt(p.getProperty("weight","0"));
  Map<String,ResourceLocation> locations=new HashMap<String,ResourceLocation>();for(String k:p.stringPropertyNames())if(k.equals("texture")||k.startsWith("texture.")||k.equals("tile"))locations.put(k.equals("tile")?"texture":k,texture(source,p.getProperty(k)));
  if(!armor&&!locations.containsKey("texture"))locations.put("texture",texture(source,basename(source.getResourcePath())));textures=Collections.unmodifiableMap(locations);
 }
 public boolean matches(ItemStack stack){if(stack==null||!items.contains(stack.getItem())||!count.matches(stack.stackSize))return false;int value=stack.getItemDamage()&mask;if(percent)value=stack.getMaxDamage()>0?(int)(100L*value/stack.getMaxDamage()):0;return damage.matches(value);}
 public ResourceLocation armorTexture(ItemStack stack,int slot,ResourceLocation original){String material=((ItemArmor)stack.getItem()).getArmorMaterial().getName();String suffix=original.getResourcePath().endsWith("_overlay.png")?"_overlay":"";ResourceLocation chosen=textures.get("texture."+material+"_layer_"+(slot==2?2:1)+suffix);return chosen!=null?chosen:suffix.isEmpty()?textures.get("texture"):null;}
 static String basename(String path){int start=path.lastIndexOf('/')+1,end=path.lastIndexOf('.');return path.substring(start,end>start?end:path.length());}
 public static ResourceLocation texture(ResourceLocation source,String value){
  if(value==null||value.trim().isEmpty())throw new IllegalArgumentException("Empty texture");String path=value.trim().replace('\\','/'),domain=source.getResourceDomain();int colon=path.indexOf(':');boolean root=colon>=0;
  if(root){domain=path.substring(0,colon);path=path.substring(colon+1);}else if(path.startsWith("assets/minecraft/")){domain="minecraft";path=path.substring(17);root=true;}else if(path.startsWith("~/")){path="mcpatcher/"+path.substring(2);root=true;}
  if(path.startsWith("/")){path=path.substring(1);root=true;}if(path.startsWith("textures/")||path.startsWith("mcpatcher/")||path.startsWith("optifine/"))root=true;
  if(!root){String base=source.getResourcePath();path=base.substring(0,base.lastIndexOf('/')+1)+path;}
  Deque<String> parts=new ArrayDeque<String>();for(String part:path.split("/")){if(part.isEmpty()||part.equals("."))continue;if(part.equals("..")){if(parts.isEmpty())throw new IllegalArgumentException("Texture escapes assets");parts.removeLast();}else parts.add(part);}
  path=String.join("/",parts);if(!domain.matches("[a-z0-9_.-]+")||path.isEmpty()||!path.matches("[A-Za-z0-9_./ -]+"))throw new IllegalArgumentException("Invalid texture path");if(!path.endsWith(".png"))path+=".png";return new ResourceLocation(domain,path);
 }
 public static final class Range {
  private final List<int[]> ranges=new ArrayList<int[]>();public Range(String text){if(text.trim().isEmpty()){ranges.add(new int[]{0,Integer.MAX_VALUE});return;}for(String token:text.trim().split("\\s+")){if(token.matches("[0-9]+")){int n=Integer.parseInt(token);ranges.add(new int[]{n,n});}else if(token.matches("[0-9]*-[0-9]*")&&!token.equals("-")){String[] ends=token.split("-",-1);int min=ends[0].isEmpty()?0:Integer.parseInt(ends[0]),max=ends[1].isEmpty()?Integer.MAX_VALUE:Integer.parseInt(ends[1]);if(min>max)throw new IllegalArgumentException("Reversed interval");ranges.add(new int[]{min,max});}else throw new IllegalArgumentException("Invalid interval");}}
  public boolean matches(int value){for(int[] r:ranges)if(value>=r[0]&&value<=r[1])return true;return false;}
 }
}
