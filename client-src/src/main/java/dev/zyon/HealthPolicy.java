package dev.zyon;
import java.util.Locale;
public final class HealthPolicy {
 private HealthPolicy(){}
 public static boolean visible(boolean enabled,boolean self,int view,boolean hidden,boolean invisible,boolean sneaking,boolean sleeping,boolean spectator,double distanceSquared,boolean allowed){return enabled&&(!self||view!=0)&&!hidden&&!invisible&&!sneaking&&!sleeping&&!spectator&&distanceSquared<=1024&&allowed;}
 public static double height(double scale,boolean scoreBelow){return .55+.3*scale+(scoreBelow?.3:0);}
 public static String value(float health,float max,float absorption,int mode){if(!Float.isFinite(health)||!Float.isFinite(max)||!Float.isFinite(absorption))return "?";health=Math.max(0,health);absorption=Math.max(0,absorption);if(mode==1)return Math.round(health/Math.max(1,max)*100)+"%";String value=String.format(Locale.ROOT,health==Math.round(health)?"%.0f":"%.1f",health);return mode==2?value+" + "+Math.round(absorption):value;}
}
