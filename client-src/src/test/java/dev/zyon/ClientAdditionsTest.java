package dev.zyon;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.junit.Test;
import static org.junit.Assert.*;
public class ClientAdditionsTest {
 @Test public void attacksAreRateLimitedAcrossTargetsAndDelayedTicks(){AttackClock c=new AttackClock();assertTrue(c.ready(0,10));assertFalse(c.ready(99999999,10));assertTrue(c.ready(100000000,10));assertTrue(c.ready(10000000000L,10));assertFalse(c.ready(10000000001L,10));c.reset();assertTrue(c.ready(10000000002L,10));assertFalse(c.ready(10000000003L,Double.NaN));assertFalse(c.ready(10000000003L,0));assertFalse(c.ready(10000000003L,21));}
 @Test public void timerSurvivesNanoTimeWraparound(){AttackClock c=new AttackClock();assertTrue(c.ready(Long.MAX_VALUE-50,20));assertFalse(c.ready(Long.MAX_VALUE,20));assertTrue(c.ready(Long.MIN_VALUE+50000000L,20));}
 @Test public void capeAtlasPreservesPixelsAndTransparentPadding()throws Exception{BufferedImage original=new BufferedImage(46,22,BufferedImage.TYPE_INT_ARGB);original.setRGB(3,4,0xFF249BFF);ByteArrayOutputStream b=new ByteArrayOutputStream();ImageIO.write(original,"PNG",b);BufferedImage atlas=CapeImages.decode(new ByteArrayInputStream(b.toByteArray()));assertEquals(64,atlas.getWidth());assertEquals(32,atlas.getHeight());assertEquals(0xFF249BFF,atlas.getRGB(3,4));assertEquals(0,atlas.getRGB(63,31));assertEquals(128,CapeImages.atlas(new BufferedImage(100,40,2)).getWidth());}
 @Test public void capeRejectsHugeImagesAndInvalidData()throws Exception{ByteArrayOutputStream b=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(1025,1,2),"PNG",b);try{CapeImages.decode(new ByteArrayInputStream(b.toByteArray()));fail();}catch(IOException expected){}try{CapeImages.decode(new ByteArrayInputStream(new byte[1048577]));fail();}catch(IOException expected){}try{CapeImages.decode(new ByteArrayInputStream("Not an image".getBytes("UTF-8")));fail();}catch(IOException expected){}}
 @Test public void capeNicknameCannotEscapeCacheOrRemotePath(){assertEquals("Player_1",CapeImages.nickname("Player_1"));for(String invalid:new String[]{"../escape","bad/name","x?token=abc","bad nick","ab",null})try{CapeImages.nickname(invalid);fail();}catch(IllegalArgumentException expected){}}
}
