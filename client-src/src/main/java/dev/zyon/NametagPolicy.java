package dev.zyon;

/** Visibility of the local player's third-person label, independent of rendering. */
public final class NametagPolicy {
 private NametagPolicy(){}
 public static boolean selfVisible(boolean enabled,int perspective,boolean ownPlayer,boolean hiddenHud,
                                   boolean invisible,boolean sneaking,boolean spectator,boolean sleeping){
  return enabled&&perspective>0&&ownPlayer&&!hiddenHud&&!invisible&&!sneaking&&!spectator&&!sleeping;
 }
 public static int labelLeft(int textWidth,boolean badge){return -(textWidth+(badge?12:0))/2;}
}
