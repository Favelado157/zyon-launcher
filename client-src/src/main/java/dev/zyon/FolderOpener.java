package dev.zyon;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;

/** Opens a directory as one native argument; never concatenates a shell command. */
public final class FolderOpener {
 public interface Starter {void start(String[] arguments)throws IOException;}
 private FolderOpener(){}
 public static String[] command(String directory,String osName,String windowsRoot){
  String os=osName.toLowerCase(Locale.ROOT);
  if(os.startsWith("windows")){String explorer=windowsRoot==null||windowsRoot.trim().isEmpty()?"explorer.exe":windowsRoot+(windowsRoot.endsWith("\\")||windowsRoot.endsWith("/")?"":"\\")+"explorer.exe";return new String[]{explorer,directory};}
  if(os.contains("mac")||os.contains("darwin"))return new String[]{"/usr/bin/open",directory};
  return new String[]{"xdg-open",directory};
 }
 public static File open(File directory,String osName,String windowsRoot,Starter starter)throws IOException{
  File actual=directory.getCanonicalFile();Files.createDirectories(actual.toPath());if(!actual.isDirectory())throw new IOException("O caminho de texturas nao e uma pasta.");
  starter.start(command(actual.getAbsolutePath(),osName,windowsRoot));return actual;
 }
 public static File open(File directory)throws IOException{
  return open(directory,System.getProperty("os.name",""),System.getenv("SystemRoot"),new Starter(){public void start(String[] arguments)throws IOException{
   // Explorer may reuse an existing process and return a nonzero status after opening successfully.
   // Starting it must not wait on, or interpret, that process's exit code.
   new ProcessBuilder(arguments).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.INHERIT).start();
  }});
 }
}
