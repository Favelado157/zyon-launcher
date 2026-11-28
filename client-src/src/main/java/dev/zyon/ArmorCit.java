package dev.zyon;
import java.io.*;import java.nio.file.*;import java.util.*;import java.util.zip.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.*;
import net.minecraft.client.resources.model.*;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.item.*;
import net.minecraft.util.*;
import dev.zyon.platform.ReflectionHelper;
/** Active resource-pack armor CIT compatibility; no OptiFine classes or texture files are distributed. */
public final class ArmorCit {
 private static List<ArmorCitRule> rules=Collections.emptyList();
 private static final Map<ResourceLocation,ResourceLocation> sprites=new HashMap<ResourceLocation,ResourceLocation>();
 private static final Map<ArmorCitRule,ResourceLocation> ruleSprites=new IdentityHashMap<ArmorCitRule,ResourceLocation>();
 private static final Map<ArmorCitRule,Map<IBakedModel,IBakedModel>> models=new IdentityHashMap<ArmorCitRule,Map<IBakedModel,IBakedModel>>();
 private static final int MAX_RULES=2048,MAX_PROPERTIES=65536;
 private ArmorCit(){}
 public static List<String> propertyPaths(File file,Set<String> domains)throws IOException{
  List<String> paths=new ArrayList<String>();
  if(file.isDirectory()){
   Path root=file.toPath();for(String domain:domains)for(String prefix:new String[]{"mcpatcher/cit","optifine/cit"}){Path dir=root.resolve("assets").resolve(domain).resolve(prefix);if(!Files.isDirectory(dir)||Files.isSymbolicLink(dir))continue;
    try(java.util.stream.Stream<Path> stream=Files.walk(dir)){Iterator<Path> it=stream.iterator();while(it.hasNext()){Path p=it.next();if(!Files.isSymbolicLink(p)&&Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS)&&p.toString().endsWith(".properties")){paths.add(root.relativize(p).toString().replace(File.separatorChar,'/'));if(paths.size()>MAX_RULES)throw new IOException("Too many armor CIT properties");}}}
   }
  }else if(file.isFile())try(ZipFile zip=new ZipFile(file)){Enumeration<? extends ZipEntry> entries=zip.entries();while(entries.hasMoreElements()){ZipEntry e=entries.nextElement();if(!e.isDirectory()&&validPath(e.getName(),domains)){if(e.getSize()>MAX_PROPERTIES)continue;paths.add(e.getName());if(paths.size()>MAX_RULES)throw new IOException("Too many armor CIT properties");}}}
  Collections.sort(paths);return paths;
 }
 private static boolean validPath(String path,Set<String> domains){if(path.contains("\\")||Arrays.asList(path.split("/")).contains("..")||!path.endsWith(".properties"))return false;String[] parts=path.split("/",4);return parts.length==4&&parts[0].equals("assets")&&domains.contains(parts[1])&&(parts[2].equals("mcpatcher")||parts[2].equals("optifine"))&&parts[3].startsWith("cit/");}
 private static ResourceLocation location(String path){String[] parts=path.split("/",3);return new ResourceLocation(parts[1],parts[2]);}
 private static Properties properties(InputStream stream)throws IOException{try(InputStream in=stream){ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] data=new byte[2048];int n;while((n=in.read(data))!=-1){if(b.size()+n>MAX_PROPERTIES)throw new IOException("Oversized armor CIT rule");b.write(data,0,n);}Properties p=new Properties();p.load(new ByteArrayInputStream(b.toByteArray()));return p;}}
 private static boolean exists(IResourceManager manager,ResourceLocation location){try(InputStream resource=manager.getResource(location).getInputStream()){return true;}catch(IOException e){return false;}}
 @SuppressWarnings("unchecked") public static void prepare(TextureMap atlas,IResourceManager manager){
  if(atlas!=Minecraft.getMinecraft().getTextureMapBlocks())return;
  sprites.clear();ruleSprites.clear();models.clear();Map<ResourceLocation,ArmorCitRule> chosen=new LinkedHashMap<ResourceLocation,ArmorCitRule>();
  try{
   Map<String,FallbackResourceManager> domains=(Map<String,FallbackResourceManager>)ReflectionHelper.findField(SimpleReloadableResourceManager.class,"domainResourceManagers").get(manager);
   Set<IResourcePack> visited=Collections.newSetFromMap(new IdentityHashMap<IResourcePack,Boolean>());List<IResourcePack> packs=new ArrayList<IResourcePack>();
   // Each domain keeps packs in ascending priority; Minecraft armor rules normally live in this domain.
   List<String> names=new ArrayList<String>(domains.keySet());Collections.sort(names);names.remove("minecraft");names.add(0,"minecraft");
   for(String domain:names){FallbackResourceManager fallback=domains.get(domain);if(fallback==null)continue;for(IResourcePack pack:(List<IResourcePack>)ReflectionHelper.findField(FallbackResourceManager.class,"resourcePacks").get(fallback))if(visited.add(pack))packs.add(pack);}
   int priority=0;for(IResourcePack pack:packs){priority++;if(!(pack instanceof AbstractResourcePack))continue;File file=(File)ReflectionHelper.findField(AbstractResourcePack.class,"resourcePackFile").get(pack);
    for(String path:propertyPaths(file,pack.getResourceDomains())){ResourceLocation source=location(path);chosen.remove(source);try{ArmorCitRule rule=new ArmorCitRule(source,properties(pack.getInputStream(source)),priority);boolean available=!rule.textures.isEmpty();for(ResourceLocation texture:rule.textures.values())available&=exists(manager,texture);if(available)chosen.put(source,rule);}catch(IllegalArgumentException ignored){}if(chosen.size()>MAX_RULES)throw new IOException("Too many armor CIT rules");}
   }
  }catch(Exception e){org.apache.logging.log4j.LogManager.getLogger("Zyon").warn("Some armor CIT rules could not be loaded",e);}
  List<ArmorCitRule> sorted=new ArrayList<ArmorCitRule>(chosen.values());Collections.sort(sorted,new Comparator<ArmorCitRule>(){public int compare(ArmorCitRule a,ArmorCitRule b){int n=Integer.compare(b.priority,a.priority);if(n==0)n=Integer.compare(b.weight,a.weight);return n!=0?n:a.source.toString().compareTo(b.source.toString());}});rules=Collections.unmodifiableList(sorted);
  int index=0;for(ArmorCitRule rule:rules)if(!rule.armor){ResourceLocation texture=rule.textures.get("texture"),sprite=new ResourceLocation("zyon","cit/armor_"+index++);sprites.put(sprite,texture);ruleSprites.put(rule,sprite);atlas.registerSprite(sprite);}
  org.apache.logging.log4j.LogManager.getLogger("Zyon").info("Armor CIT: {} active rules, {} custom icons",rules.size(),ruleSprites.size());
 }
 public static ResourceLocation atlasResource(ResourceLocation vanilla,ResourceLocation sprite,int level){ResourceLocation found=sprites.get(sprite);return found!=null&&level==0?found:vanilla;}
 public static ResourceLocation armorTexture(ResourceLocation vanilla,ItemStack stack,int slot){if(stack==null||!(stack.getItem() instanceof ItemArmor))return vanilla;for(ArmorCitRule rule:rules)if(rule.armor&&rule.matches(stack)){ResourceLocation texture=rule.armorTexture(stack,slot,vanilla);if(texture!=null)return texture;}return vanilla;}
 public static IBakedModel itemModel(IBakedModel vanilla,ItemStack stack){if(vanilla==null||stack==null||!(stack.getItem() instanceof ItemArmor))return vanilla;for(ArmorCitRule rule:rules)if(!rule.armor&&rule.matches(stack)){Map<IBakedModel,IBakedModel> cache=models.get(rule);if(cache==null){cache=new IdentityHashMap<IBakedModel,IBakedModel>();models.put(rule,cache);}IBakedModel ready=cache.get(vanilla);if(ready!=null)return ready;try{ready=bake(vanilla,ruleSprites.get(rule));cache.put(vanilla,ready);return ready;}catch(Exception e){cache.put(vanilla,vanilla);org.apache.logging.log4j.LogManager.getLogger("Zyon").warn("Custom armor icon unavailable: "+rule.source,e);return vanilla;}}return vanilla;}
 private static final class CitModelBlock extends ModelBlock {CitModelBlock(Map<String,String> textures,ItemCameraTransforms transforms){super(Collections.<BlockPart>emptyList(),textures,false,false,transforms);}}
 private static IBakedModel bake(IBakedModel original,ResourceLocation spriteId){
  TextureMap atlas=Minecraft.getMinecraft().getTextureMapBlocks();TextureAtlasSprite sprite=atlas.getAtlasSprite(spriteId.toString());if(sprite==atlas.getMissingSprite())return original;
  Map<String,String> textures=new HashMap<String,String>();textures.put("layer0",spriteId.toString());ModelBlock source=new CitModelBlock(textures,original.getItemCameraTransforms());ModelBlock generated=new ItemModelGenerator().makeItemModel(atlas,source);
  List<BakedQuad> quads=new ArrayList<BakedQuad>();FaceBakery bakery=new FaceBakery();for(BlockPart part:generated.getElements())for(Map.Entry<EnumFacing,BlockPartFace> face:part.mapFaces.entrySet())quads.add(bakery.makeBakedQuad(part.positionFrom,part.positionTo,face.getValue(),sprite,face.getKey(),ModelRotation.X0_Y0,part.partRotation,false,part.shade));
  List<List<BakedQuad>> faces=new ArrayList<List<BakedQuad>>();for(int i=0;i<6;i++)faces.add(Collections.<BakedQuad>emptyList());return new SimpleBakedModel(quads,faces,false,false,sprite,original.getItemCameraTransforms());
 }
}
