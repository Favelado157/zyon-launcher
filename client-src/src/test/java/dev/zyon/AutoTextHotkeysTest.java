package dev.zyon;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
public class AutoTextHotkeysTest {
 @Rule public TemporaryFolder temp=new TemporaryFolder();
 private AutoTextStore store(){AutoTextStore s=new AutoTextStore(new File(temp.getRoot(),"text.json"));s.setMessage(0,"gg");s.setKey(0,34);return s;}
 @Test public void holdingAKeySendsOnlyOnceUntilRelease(){AutoTextTrigger trigger=new AutoTextTrigger();AutoTextStore s=store();assertEquals(0,trigger.input(34,true,false,true,s));for(int i=0;i<20;i++){assertEquals(-1,trigger.input(34,true,true,true,s));assertEquals(-1,trigger.input(34,true,false,true,s));}assertEquals(-1,trigger.input(34,false,false,true,s));assertEquals(0,trigger.input(34,true,false,true,s));}
 @Test public void menusChatAndDisabledContextNeverSend(){AutoTextTrigger t=new AutoTextTrigger();AutoTextStore s=store();assertEquals(-1,t.input(34,true,false,false,s));assertEquals(-1,t.input(34,true,true,true,s));t.released(34);assertEquals(0,t.input(34,true,false,true,s));}
 @Test public void emptyMessagesAndInvalidOrUnboundKeysDoNotSend(){AutoTextTrigger t=new AutoTextTrigger();AutoTextStore s=store();s.setMessage(0," ");for(int key:new int[]{-1,0,34,35,256})assertEquals(-1,t.input(key,true,false,true,s));}
 @Test public void legacyTextsSurviveMigrationWithoutAutomaticallyAssigningKeys()throws Exception{File file=temp.newFile();Files.write(file.toPath(),"{\"schema\":1,\"slots\":[{\"name\":\"Atalho antigo\",\"message\":\"/spawn\"}]}".getBytes(StandardCharsets.UTF_8));AutoTextStore s=new AutoTextStore(file);s.load();assertEquals("/spawn",s.message(0));assertEquals(0,s.key(0));s.setKey(0,34);s.save();AutoTextStore next=new AutoTextStore(file);next.load();assertEquals("/spawn",next.message(0));assertEquals(34,next.key(0));}
 @Test public void duplicateBindingsCannotSendMultipleMessages(){AutoTextStore s=store();s.setMessage(1,"outro");try{s.setKey(1,34);fail();}catch(IllegalArgumentException expected){assertEquals(0,s.key(1));}s.setKey(0,0);s.setKey(1,34);assertEquals(1,new AutoTextTrigger().input(34,true,false,true,s));}
 @Test public void malformedKeysAreRejectedWithoutChangingMessages()throws Exception{File file=temp.newFile();AutoTextStore s=store();for(String key:new String[]{"-1","256","2.5"}){Files.write(file.toPath(),("{\"schema\":2,\"slots\":[{\"message\":\"bad\",\"key\":"+key+"}]}").getBytes(StandardCharsets.UTF_8));AutoTextStore loaded=new AutoTextStore(file);loaded.setMessage(0,"preserve");try{loaded.load();fail(key);}catch(IOException expected){assertEquals("preserve",loaded.message(0));}}}
 @Test public void repeatedEventsCannotSendImmediatelyAfterFocusReturns(){AutoTextTrigger t=new AutoTextTrigger();AutoTextStore s=store();assertEquals(-1,t.input(34,true,true,true,s));t.input(34,false,false,true,s);assertEquals(0,t.input(34,true,false,true,s));}
}
