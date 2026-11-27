package dev.zyon;

import java.nio.FloatBuffer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;

/** Restores both OpenGL state and Minecraft's cache after drawing our HUD or previews. */
public final class HudRenderState implements AutoCloseable {
 private final boolean alpha=on(GL11.GL_ALPHA_TEST),depth=on(GL11.GL_DEPTH_TEST),blend=on(GL11.GL_BLEND),lighting=on(GL11.GL_LIGHTING),material=on(GL11.GL_COLOR_MATERIAL),cull=on(GL11.GL_CULL_FACE),normal=on(GL11.GL_NORMALIZE),rescale=on(32826),depthWrite=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
 private final int alphaFunc=get(GL11.GL_ALPHA_TEST_FUNC),depthFunc=get(GL11.GL_DEPTH_FUNC),src=get(GL14.GL_BLEND_SRC_RGB),dst=get(GL14.GL_BLEND_DST_RGB),srcAlpha=get(GL14.GL_BLEND_SRC_ALPHA),dstAlpha=get(GL14.GL_BLEND_DST_ALPHA),face=get(GL11.GL_COLOR_MATERIAL_FACE),materialMode=get(GL11.GL_COLOR_MATERIAL_PARAMETER),cullMode=get(GL11.GL_CULL_FACE_MODE),shade=get(GL11.GL_SHADE_MODEL),active=get(GL13.GL_ACTIVE_TEXTURE),matrix=get(GL11.GL_MATRIX_MODE);
 private final float alphaRef=GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF),lightX=dev.zyon.platform.Platform.lastBrightnessX,lightY=dev.zyon.platform.Platform.lastBrightnessY;
 private final FloatBuffer color=BufferUtils.createFloatBuffer(16),clear=BufferUtils.createFloatBuffer(16);private final java.nio.ByteBuffer colorMask=BufferUtils.createByteBuffer(16);private final double clearDepth=GL11.glGetDouble(GL11.GL_DEPTH_CLEAR_VALUE);private final boolean[] lights=new boolean[8],textures=new boolean[2];private final int[] bound=new int[2];private boolean closed;
 private static boolean on(int capability){return GL11.glIsEnabled(capability);}private static int get(int name){return GL11.glGetInteger(name);}
 public HudRenderState(){
  GL11.glGetFloat(GL11.GL_CURRENT_COLOR,color);GL11.glGetFloat(GL11.GL_COLOR_CLEAR_VALUE,clear);GL11.glGetBoolean(GL11.GL_COLOR_WRITEMASK,colorMask);for(int i=0;i<8;i++)lights[i]=on(GL11.GL_LIGHT0+i);
  // This also retains uncached light positions, material properties and texture parameters.
  GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
  GlStateManager.matrixMode(GL11.GL_MODELVIEW);GlStateManager.pushMatrix();GlStateManager.matrixMode(GL11.GL_PROJECTION);GlStateManager.pushMatrix();
  for(int i=0;i<2;i++){GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit+i);textures[i]=on(GL11.GL_TEXTURE_2D);bound[i]=get(GL11.GL_TEXTURE_BINDING_2D);GlStateManager.matrixMode(GL11.GL_TEXTURE);GlStateManager.pushMatrix();}
  GlStateManager.setActiveTexture(active);GlStateManager.matrixMode(matrix);
 }
 public void close(){if(closed)return;closed=true;
  for(int i=0;i<2;i++){GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit+i);GlStateManager.matrixMode(GL11.GL_TEXTURE);GlStateManager.popMatrix();GlStateManager.bindTexture(bound[i]);if(textures[i])GlStateManager.enableTexture2D();else GlStateManager.disableTexture2D();}
  GlStateManager.matrixMode(GL11.GL_PROJECTION);GlStateManager.popMatrix();GlStateManager.matrixMode(GL11.GL_MODELVIEW);GlStateManager.popMatrix();
  if(alpha)GlStateManager.enableAlpha();else GlStateManager.disableAlpha();GlStateManager.alphaFunc(alphaFunc,alphaRef);
  if(depth)GlStateManager.enableDepth();else GlStateManager.disableDepth();GlStateManager.depthFunc(depthFunc);GlStateManager.depthMask(depthWrite);
  if(blend)GlStateManager.enableBlend();else GlStateManager.disableBlend();GlStateManager.tryBlendFuncSeparate(src,dst,srcAlpha,dstAlpha);
  if(lighting)GlStateManager.enableLighting();else GlStateManager.disableLighting();for(int i=0;i<8;i++)if(lights[i])GlStateManager.enableLight(i);else GlStateManager.disableLight(i);
  if(material)GlStateManager.enableColorMaterial();else GlStateManager.disableColorMaterial();GlStateManager.colorMaterial(face,materialMode);
  if(cull)GlStateManager.enableCull();else GlStateManager.disableCull();GlStateManager.cullFace(cullMode);
  if(normal)GlStateManager.enableNormalize();else GlStateManager.disableNormalize();if(rescale)GlStateManager.enableRescaleNormal();else GlStateManager.disableRescaleNormal();GlStateManager.shadeModel(shade);
  GlStateManager.clearColor(clear.get(0),clear.get(1),clear.get(2),clear.get(3));GlStateManager.clearDepth(clearDepth);GlStateManager.colorMask(colorMask.get(0)!=0,colorMask.get(1)!=0,colorMask.get(2)!=0,colorMask.get(3)!=0);
  GlStateManager.color(color.get(0),color.get(1),color.get(2),color.get(3));OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,lightX,lightY);
  GlStateManager.setActiveTexture(active);GlStateManager.matrixMode(matrix);
  // Cache setters above must run before popping attributes, so both layers agree afterwards.
  GL11.glPopAttrib();
 }
}
