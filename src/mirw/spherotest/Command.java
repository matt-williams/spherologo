package mirw.spherotest;

import orbotix.robot.base.Robot;

public interface Command extends Cloneable {
  public String getShortDescription();
  public String getLongDescription();
  public long getDuration();
  public void execute(Robot robot);
  public void finish(Robot robot);
}
