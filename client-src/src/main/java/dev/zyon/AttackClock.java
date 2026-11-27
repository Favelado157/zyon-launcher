package dev.zyon;
/** A monotonic timer shared by automatic attacks; no burst after a pause or target switch. */
public final class AttackClock {
 private long next;private boolean started;
 public boolean ready(long now,double cps){if(!Double.isFinite(cps)||cps<1||cps>20)return false;if(started&&now-next<0)return false;next=now+(long)(1000000000d/cps);started=true;return true;}
 public void reset(){started=false;}
}
