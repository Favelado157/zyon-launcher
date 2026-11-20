package dev.zyon;
import org.junit.Test;
import static org.junit.Assert.*;
public class NametagPolicyTest {
 @Test public void bothThirdPersonViewsShowOwnName(){for(int view=1;view<=2;view++)assertTrue(NametagPolicy.selfVisible(true,view,true,false,false,false,false,false));}
 @Test public void firstPersonAndOtherPlayersDoNotDuplicateNames(){assertFalse(NametagPolicy.selfVisible(true,0,true,false,false,false,false,false));assertFalse(NametagPolicy.selfVisible(true,1,false,false,false,false,false,false));}
 @Test public void respectsHiddenHudAndPlayerVisibility(){
  assertFalse(NametagPolicy.selfVisible(false,1,true,false,false,false,false,false));
  assertFalse(NametagPolicy.selfVisible(true,1,true,true,false,false,false,false));
  assertFalse(NametagPolicy.selfVisible(true,1,true,false,true,false,false,false));
  assertFalse(NametagPolicy.selfVisible(true,1,true,false,false,true,false,false));
  assertFalse(NametagPolicy.selfVisible(true,1,true,false,false,false,true,false));
  assertFalse(NametagPolicy.selfVisible(true,1,true,false,false,false,false,true));
 }
 @Test public void badgeAndTextAreCenteredTogether(){assertEquals(-30,NametagPolicy.labelLeft(60,false));assertEquals(-36,NametagPolicy.labelLeft(60,true));}
}
