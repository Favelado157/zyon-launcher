package dev.zyon;
/** Only a new physical key press can select a message. Releases, repeat events and menus never send. */
public final class AutoTextTrigger {
 private final boolean[] held=new boolean[256];
 public int input(int key,boolean down,boolean repeat,boolean active,AutoTextStore store){
  if(key<=0||key>=held.length)return -1;if(!down){held[key]=false;return -1;}if(held[key])return -1;held[key]=true;if(repeat||!active)return -1;
  for(int i=0;i<AutoTextStore.SLOTS;i++)if(store.key(i)==key&&!store.message(i).trim().isEmpty())return i;return -1;
 }
 public void released(int key){if(key>0&&key<held.length)held[key]=false;}
}
