package dev.zyon;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
public class FolderOpenerTest {
 @Rule public TemporaryFolder temp=new TemporaryFolder();
 @Test public void windowsPathWithSpacesIsOneUnmodifiedArgument(){String path="C:\\Users\\Win 11 Pro\\AppData\\Roaming\\ZyonLauncher\\instances\\1.8.9\\resourcepacks";assertArrayEquals(new String[]{"C:\\Windows\\explorer.exe",path},FolderOpener.command(path,"Windows 11","C:\\Windows"));}
 @Test public void specialCharactersAreNotExpandedOrPassedToAShell(){String path="D:\\Texturas & mapas\\100% diversão\\resourcepacks";String[] command=FolderOpener.command(path,"Windows 10",null);assertArrayEquals(new String[]{"explorer.exe",path},command);assertFalse(command[0].contains("cmd"));assertFalse(command[1].startsWith("\""));}
 @Test public void platformsUseTheirNativeFolderAction(){assertArrayEquals(new String[]{"/usr/bin/open","/Users/Nome Sobrenome/packs"},FolderOpener.command("/Users/Nome Sobrenome/packs","Mac OS X",null));assertArrayEquals(new String[]{"xdg-open","/home/Nome Sobrenome/packs"},FolderOpener.command("/home/Nome Sobrenome/packs","Linux",null));assertArrayEquals(new String[]{"D:\\Windows\\explorer.exe","E:\\packs"},FolderOpener.command("E:\\packs","Windows 11","D:\\Windows\\"));}
 @Test public void createsMissingDirectoryAndLeavesExistingTexturesUntouched()throws Exception{File directory=new File(temp.getRoot(),"Win 11 Pro/instancia/resourcepacks");final String[][] received={null};File opened=FolderOpener.open(directory,"Windows 11","C:\\Windows",args->received[0]=args);assertTrue(directory.isDirectory());assertEquals(opened.getCanonicalPath(),received[0][1]);File texture=new File(directory,"Minha textura.zip");Files.write(texture.toPath(),"keep".getBytes(StandardCharsets.UTF_8));FolderOpener.open(directory,"Windows 11",null,args->{});assertEquals("keep",new String(Files.readAllBytes(texture.toPath()),StandardCharsets.UTF_8));}
 @Test public void existingFileCannotBeReplacedByADirectory()throws Exception{File file=temp.newFile("resourcepacks");Files.write(file.toPath(),new byte[]{1,2,3});try{FolderOpener.open(file,"Windows 11",null,args->fail("Must not launch"));fail();}catch(IOException expected){assertArrayEquals(new byte[]{1,2,3},Files.readAllBytes(file.toPath()));}}
 @Test public void processFailureIsReportedWithoutASecondShellAttempt()throws Exception{File directory=temp.newFolder("texturas");int[] calls={0};try{FolderOpener.open(directory,"Windows 11",null,args->{calls[0]++;throw new IOException("Explorer indisponivel");});fail();}catch(IOException expected){assertEquals(1,calls[0]);assertEquals("Explorer indisponivel",expected.getMessage());}}
}
