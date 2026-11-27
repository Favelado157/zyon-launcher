package dev.zyon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
/** Reattach the local entity when an initial placeholder chunk is replaced by the server. */
public final class ClientPresence {
 private static EntityPlayerSP previous;private static Chunk checked;
 public static void tick(){
  Minecraft mc=Minecraft.getMinecraft();EntityPlayerSP p=mc.thePlayer;
  if(p==null||mc.theWorld==null){previous=null;checked=null;return;}
  BlockPos pos=new BlockPos(p);if(!mc.theWorld.isBlockLoaded(pos))return;
  Chunk chunk=mc.theWorld.getChunkFromBlockCoords(pos);if(chunk instanceof EmptyChunk)return;
  if(previous==p&&checked==chunk)return;
  int section=MathHelper.clamp_int(MathHelper.floor_double(p.posY/16),0,15);
  // Vanilla can retain addedToChunk=true for an unloaded placeholder at low-altitude spawn.
  if(!chunk.getEntityLists()[section].contains(p))chunk.addEntity(p);
  previous=p;checked=chunk;
 }
}
