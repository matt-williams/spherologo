package mirw.spherotest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import orbotix.robot.base.RGBLEDOutputCommand;
import orbotix.robot.base.RawMotorCommand;
import orbotix.robot.base.Robot;
import orbotix.robot.base.RobotControl;
import orbotix.robot.base.RobotProvider;
import orbotix.robot.base.RollCommand;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Handler;

public class Vocabulary {
  private static final String HOMOPHONES = "homophones.";

  private static abstract class BaseCommand implements Command {
    private String mShortDescription;
    private String mLongDescription;
    private long mDuration;
    
    private BaseCommand(String description) {
      this(description, description);
    }
    
    private BaseCommand(String description, long duration) {
      this(description, description, duration);
    }
    
    private BaseCommand(String shortDescription, String longDescription) {
      this(shortDescription, longDescription, 0);
    }
    
    private BaseCommand(String shortDescription, String longDescription, long duration) {
      mShortDescription = shortDescription;
      mLongDescription = longDescription;
      mDuration = duration;
    }
    
    public String getShortDescription() {
      return mShortDescription;
    }
    public String getLongDescription() {
      return mLongDescription;
    }
    public long getDuration() {
      return mDuration;
    }
    public void finish(Robot robot) {
    }
    public String toString() {
      return mShortDescription;
    }
  }
  
  static void registerCommand(String name, Command command) {
    mNameCommandMap.put(name.toLowerCase(), command);
    mCommandNameList.add(name);
  }
  
  static void registerCommand(Command command) {
    registerCommand(command.getShortDescription(), command);
  }
  
  
  private static float sOrientation = 0.0f;
  private static Handler sRainbowHandler = new Handler();
  private static int sRainbowSequence = 0;
  private static Robot sRobot;
  private static Runnable sRainbowRunnable = new Runnable() {
    public void run() {
      int red = (sRainbowSequence < 0x100) ? 0xff :
        (sRainbowSequence < 0x200) ? 0x1ff - sRainbowSequence :
        (sRainbowSequence < 0x400) ? 0 :
          (sRainbowSequence < 0x500) ? sRainbowSequence - 0x400 :
        0xff;
      int green = (sRainbowSequence < 0x100) ? sRainbowSequence :
                (sRainbowSequence < 0x300) ? 0xff :
                (sRainbowSequence < 0x400) ? 0x3ff - sRainbowSequence :
                0;
      int blue = (sRainbowSequence < 0x200) ? 0 :
        (sRainbowSequence < 0x300) ? sRainbowSequence - 0x200:
        (sRainbowSequence < 0x500) ? 0xff :
        0x5ff - sRainbowSequence;
      RGBLEDOutputCommand.sendCommand(sRobot, red, green, blue);
      sRainbowSequence = (sRainbowSequence + 3) % (0x600);
      sRainbowHandler.postDelayed(this, 50);
    }
  };
  private static void startRainbowHandler(Robot robot) {
    sRainbowSequence = 0;
    sRobot = robot;
    sRainbowHandler.postDelayed(sRainbowRunnable, 50);
  }
  private static void cancelRainbowHandler() {
    sRainbowHandler.removeCallbacks(sRainbowRunnable);
  }

  static final List<String> mCommandNameList = new ArrayList<String>();
  static final Map<String, Command> mNameCommandMap = new HashMap<String, Command>();
  static {
    registerCommand(new BaseCommand("Red") {
      public void execute(Robot robot) {
        cancelRainbowHandler();
        RGBLEDOutputCommand.sendCommand(robot, 255, 0, 0);
      }});
    registerCommand(new BaseCommand("Green") {
      public void execute(Robot robot) {
        cancelRainbowHandler();
        RGBLEDOutputCommand.sendCommand(robot, 0, 255, 0);
      }});
    registerCommand(new BaseCommand("Blue") {
      public void execute(Robot robot) {
        cancelRainbowHandler();
        RGBLEDOutputCommand.sendCommand(robot, 0, 0, 255);
      }});
    registerCommand(new BaseCommand("Rainbow") {
      public void execute(Robot robot) {
        startRainbowHandler(robot);
      }});
    registerCommand(new BaseCommand("Off") {
      public void execute(Robot robot) {
        cancelRainbowHandler();
        RGBLEDOutputCommand.sendCommand(robot, 0, 0, 0);
      }});
    registerCommand(new BaseCommand("Forward", 1000) {
      public void execute(Robot robot) {
        RollCommand.sendCommand(robot, sOrientation, 1.0f);
      }
      public void finish(Robot robot) {
        RollCommand.sendStop(robot);
      }});
    registerCommand(new BaseCommand("Reverse", 1000) {
      public void execute(Robot robot) {
        RollCommand.sendCommand(robot, (sOrientation + 180.0f) % 360.0f , 1.0f);
      }
      public void finish(Robot robot) {
        RollCommand.sendStop(robot);
      }});
    registerCommand(new BaseCommand("Left", 0) {
      public void execute(Robot robot) {
        sOrientation = (sOrientation + 270.0f) % 360.f;
      }});
    registerCommand(new BaseCommand("Right") {
      public void execute(Robot robot) {
        sOrientation = (sOrientation + 90.0f) % 360.f;
      }});
    registerCommand(new BaseCommand("Wait", 1000) {
      public void execute(Robot robot) {}});
  };
  private SharedPreferences mPrefs;
  public Vocabulary(SharedPreferences prefs) {
    mPrefs = prefs;
  }
  
  public String[] getCommandNames() {
    return mCommandNameList.toArray(new String[0]);
  }
  
  public Command getCommand(String name) {
    return (name != null) ? mNameCommandMap.get(name.toLowerCase()) : null;
  }
  
  public Command getCommand(List<String> strings) {
    for (String string : strings) {
      StringTokenizer tokener = new StringTokenizer(string, " \r\n,.;:");
      while (tokener.hasMoreElements()) {
        String word = tokener.nextToken().toLowerCase();
        Command command = getCommand(word);
        if (command != null) {
          return command;
        }
        String basicWord = mPrefs.getString(HOMOPHONES + word, null);
        command = getCommand(basicWord);
        if (command != null) {
          return command;
        }
      }
    }
    return null;
  }

  public void learnWord(String text, String commandName) {
    Editor editor = mPrefs.edit();
    StringTokenizer tokener = new StringTokenizer(text, " \r\n,.;:");
    while (tokener.hasMoreElements()) {
      String word = tokener.nextToken().toLowerCase();
      editor.putString(HOMOPHONES + word, commandName);
    }
    editor.commit();
  }

  public void forget() {
    Editor editor = mPrefs.edit();
    for (String key : mPrefs.getAll().keySet()) {
      if (key.startsWith(HOMOPHONES)) {
        editor.remove(key);
      }
    }
    editor.commit();
  }
}
