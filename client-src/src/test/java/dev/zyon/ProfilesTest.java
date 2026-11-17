package dev.zyon;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import static org.junit.Assert.*;
public class ProfilesTest {
 @Rule public TemporaryFolder temporary=new TemporaryFolder();private Feature aura,speed;private Profiles profiles;private File folder;
 @Before public void setup()throws IOException{Zyon client=new Zyon();aura=new Feature("killaura","KillAura","",Feature.Category.COMBAT,Setting.number("range","Alcance",3,1,6,.1),Setting.bool("players","Jogadores",true));speed=new Feature("speed","Speed","",Feature.Category.MOVEMENT,Setting.number("speed","Velocidade",.35,.1,1.2,.01));client.features.add(aura);client.features.add(speed);folder=temporary.newFolder("profiles");profiles=new Profiles(client,folder);}
 @Test public void roundTrip()throws IOException{aura.key=19;aura.setting("range").set(4.2);aura.setting("players").set(0);profiles.save("Pvp");aura.key=0;aura.setting("range").set(2);aura.setting("players").set(1);profiles.load("pvp");assertEquals(19,aura.key);assertEquals(4.2,aura.number("range"),.00001);assertFalse(aura.flag("players"));assertEquals(java.util.Arrays.asList("pvp"),profiles.list());}
 @Test public void invalidLaterModuleDoesNotChangeEarlierSettings()throws IOException{write("bad","{\"schema\":1,\"modules\":{\"killaura\":{\"key\":19,\"settings\":{\"range\":5}},\"speed\":{\"key\":-4}}}");invalid("bad");assertEquals(0,aura.key);assertEquals(3,aura.number("range"),0);}
 @Test public void nonFiniteValuesAreRejectedAtomically()throws IOException{write("bad","{\"schema\":1,\"modules\":{\"killaura\":{\"settings\":{\"range\":5}},\"speed\":{\"settings\":{\"speed\":\"NaN\"}}}}");invalid("bad");assertEquals(3,aura.number("range"),0);}
 @Test public void editedValuesStayWithinLimits()throws IOException{write("edit","{\"schema\":1,\"modules\":{\"killaura\":{\"settings\":{\"range\":999}}}}");profiles.load("edit");assertEquals(6,aura.number("range"),0);}
 @Test public void pathTraversalCannotEscapeFolder()throws IOException{for(String n:new String[]{"../escape","/tmp/escape","a/b","a\\b","","a.b"})try{profiles.save(n);fail(n);}catch(IllegalArgumentException expected){assertTrue(expected.getMessage().contains("nome"));}assertEquals(0,folder.listFiles().length);}
 @Test public void brokenJsonAndUnknownSchemaAreRejected()throws IOException{write("broken","{not json");invalid("broken");write("future","{\"schema\":99,\"modules\":{}}");invalid("future");assertEquals(3,aura.number("range"),0);}
 @Test public void missingProfilePreservesState(){aura.key=19;invalid("missing");assertEquals(19,aura.key);}
 private void write(String name,String json)throws IOException{Files.write(new File(folder,name+".json").toPath(),json.getBytes(StandardCharsets.UTF_8));}
 private void invalid(String name){try{profiles.load(name);fail(name);}catch(IOException expected){assertNotNull(expected.getMessage());}}
}
