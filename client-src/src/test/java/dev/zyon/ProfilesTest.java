package dev.zyon;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import static org.junit.Assert.*;
public class ProfilesTest {
 @Rule public TemporaryFolder temporary=new TemporaryFolder();private Feature crosshair,zoom;private Profiles profiles;private File folder;
 @Before public void setup()throws IOException{Zyon client=new Zyon();crosshair=new Feature("crosshair","Mira","",Feature.Category.RENDER,Setting.number("size","Tamanho",5,2,16,1),Setting.bool("dot","Ponto",true));zoom=new Feature("zoom","Zoom","",Feature.Category.RENDER,Setting.number("factor","Ampliacao",3,1.5,6,.5));client.features.add(crosshair);client.features.add(zoom);folder=temporary.newFolder("profiles");profiles=new Profiles(client,folder);}
 @Test public void roundTrip()throws IOException{crosshair.key=19;crosshair.setting("size").set(9);crosshair.setting("dot").set(0);profiles.save("Visual");crosshair.key=0;crosshair.setting("size").set(2);crosshair.setting("dot").set(1);profiles.load("visual");assertEquals(19,crosshair.key);assertEquals(9,crosshair.number("size"),0);assertFalse(crosshair.flag("dot"));assertEquals(java.util.Arrays.asList("visual"),profiles.list());}
 @Test public void invalidLaterModuleDoesNotChangeEarlierSettings()throws IOException{write("bad","{\"schema\":1,\"modules\":{\"crosshair\":{\"key\":19,\"settings\":{\"size\":9}},\"zoom\":{\"key\":-4}}}");invalid("bad");assertEquals(0,crosshair.key);assertEquals(5,crosshair.number("size"),0);}
 @Test public void nonFiniteValuesAreRejectedAtomically()throws IOException{write("bad","{\"schema\":1,\"modules\":{\"crosshair\":{\"settings\":{\"size\":9}},\"zoom\":{\"settings\":{\"factor\":\"NaN\"}}}}");invalid("bad");assertEquals(5,crosshair.number("size"),0);}
 @Test public void editedValuesStayWithinLimits()throws IOException{write("edit","{\"schema\":1,\"modules\":{\"crosshair\":{\"settings\":{\"size\":999}}}}");profiles.load("edit");assertEquals(16,crosshair.number("size"),0);}
 @Test public void pathTraversalCannotEscapeFolder()throws IOException{for(String n:new String[]{"../escape","/tmp/escape","a/b","a\\b","","a.b"})try{profiles.save(n);fail(n);}catch(IllegalArgumentException expected){assertTrue(expected.getMessage().contains("nome"));}assertEquals(0,folder.listFiles().length);}
 @Test public void brokenJsonAndUnknownSchemaAreRejected()throws IOException{write("broken","{not json");invalid("broken");write("future","{\"schema\":99,\"modules\":{}}");invalid("future");assertEquals(5,crosshair.number("size"),0);}
 @Test public void missingProfilePreservesState(){crosshair.key=19;invalid("missing");assertEquals(19,crosshair.key);}
 @Test public void draggedPositionAndStyleSurviveProfileReload()throws IOException{Zyon client=new Zyon();Feature keys=new Feature("keystrokes","Keystrokes","",Feature.Category.CLIENT,Setting.number("x","X",2,0,100,.1),Setting.number("y","Y",35,0,100,.1),Setting.number("scale","Tamanho",1,.5,2,.1),Setting.choice("color","Cor",0,"Tema","Branco"));client.features.add(keys);Profiles layouts=new Profiles(client,folder);keys.setting("x").set(HudLayout.percent(124,320,72));keys.setting("y").set(100);keys.setting("scale").set(1.8);keys.setting("color").set(1);layouts.save("layout");keys.setting("x").set(0);keys.setting("y").set(0);keys.setting("scale").set(.5);keys.setting("color").set(0);layouts.load("layout");assertEquals(50,keys.number("x"),0);assertEquals(100,keys.number("y"),0);assertEquals(1.8,keys.number("scale"),.00001);assertEquals(1,keys.setting("color").integer());}
 @Test public void oldUnknownModulesCannotRegisterOrEnableActions()throws IOException{write("legacy","{\"schema\":1,\"modules\":{\"killaura\":{\"enabled\":true},\"fly\":{\"enabled\":true},\"crosshair\":{\"settings\":{\"size\":7}}}}");profiles.load("legacy");assertEquals(7,crosshair.number("size"),0);assertFalse(crosshair.enabled());assertFalse(zoom.enabled());}
 private void write(String name,String json)throws IOException{Files.write(new File(folder,name+".json").toPath(),json.getBytes(StandardCharsets.UTF_8));}
 private void invalid(String name){try{profiles.load(name);fail(name);}catch(IOException expected){assertNotNull(expected.getMessage());}}
}
