package mirw.spherotest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.att.android.speech.ATTSpeechError;
import com.att.android.speech.ATTSpeechErrorListener;
import com.att.android.speech.ATTSpeechResult;
import com.att.android.speech.ATTSpeechResultListener;
import com.att.android.speech.ATTSpeechService;
import com.att.android.speech.ATTSpeechError.ErrorType;

import example.simplespeech.SpeechAuth;
import orbotix.robot.app.CalibrationActivity;
import orbotix.robot.app.StartupActivity;
import orbotix.robot.base.FrontLEDOutputCommand;
import orbotix.robot.base.RGBLEDOutputCommand;
import orbotix.robot.base.Robot;
import orbotix.robot.base.RobotProvider;
import orbotix.robot.base.RollCommand;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.support.v4.app.NavUtils;

public class MainActivity extends Activity {

  private static final String TAG = MainActivity.class.getName();
  
    private TextView mTextView;
    private Vocabulary mVocabulary;
    private boolean mPaused = false;
    private List<Command> mCommandList = new ArrayList<Command>();
    private ListView mListView;
    private ArrayAdapter mArrayAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.e(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main);
        mTextView = (TextView)findViewById(R.id.textview);
        ((ImageButton)findViewById(R.id.imageButton1)).setOnClickListener(new OnClickListener() {
          public void onClick(View arg0) {
            startSpeechService();
          }
        });
        mVocabulary = new Vocabulary(getSharedPreferences(MainActivity.class.getName(), MODE_PRIVATE));
        mListView = (ListView)findViewById(R.id.commandlist);
        mArrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, mCommandList);
        mListView.setAdapter(mArrayAdapter);
    }

    @Override
    public void onStart() {
      Log.e(TAG, "onStart");
        super.onStart();
        Intent j = new Intent(this, StartupActivity.class);  
        startActivityForResult(j, STARTUP_ACTIVITY);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
      Log.e(TAG, "onCreateOptionsMenu");
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
      if (!mPaused) 
        menu.findItem(R.id.pause_play).setTitle("Pause"); 
      else 
        menu.findItem(R.id.pause_play).setTitle("Play"); 
      return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
      Log.e(TAG, "onOptionsItemSelected");
        switch (item.getItemId()) {
        case R.id.connect:          
          Intent j = new Intent(this, StartupActivity.class);  
          startActivityForResult(j, STARTUP_ACTIVITY);
          return true;
        case R.id.calibrate:
          Intent i = new Intent(this, CalibrationActivity.class);
          i.putExtra(CalibrationActivity.ROBOT_ID_EXTRA, mRobot.getUniqueId());
          i.putExtra(CalibrationActivity.EXTRA_TITLE_ICON_RESOURCE_ID, R.drawable.ic_launcher);
          startActivity(i);
          return true;
        case R.id.forget_vocab:
          mVocabulary.forget();
          return true;
        case R.id.pause_play:
          if (mPaused) {
            mPaused = false;
          } else {
            mPaused = true;
          }
          return true;
        case R.id.repeat:
          executeSequence(mCommandList);
          return true;
        case R.id.demo:
          clearCommands();
          addCommand(mVocabulary.getCommand("red"));
          addCommand(mVocabulary.getCommand("forward"));
          addCommand(mVocabulary.getCommand("right"));
          addCommand(mVocabulary.getCommand("green"));
          addCommand(mVocabulary.getCommand("forward"));
          addCommand(mVocabulary.getCommand("right"));
          addCommand(mVocabulary.getCommand("blue"));
          addCommand(mVocabulary.getCommand("forward"));
          addCommand(mVocabulary.getCommand("forward"));
          addCommand(mVocabulary.getCommand("left"));
          addCommand(mVocabulary.getCommand("red"));
          addCommand(mVocabulary.getCommand("forward"));
          addCommand(mVocabulary.getCommand("left"));
          addCommand(mVocabulary.getCommand("green"));
          addCommand(mVocabulary.getCommand("forward"));
          addCommand(mVocabulary.getCommand("left"));
          addCommand(mVocabulary.getCommand("blue"));
          addCommand(mVocabulary.getCommand("forward"));
          addCommand(mVocabulary.getCommand("forward"));
          addCommand(mVocabulary.getCommand("right"));
          addCommand(mVocabulary.getCommand("red"));
          executeSequence(mCommandList);
          return true;
        case R.id.clear:
          clearCommands();
          return true;
        default:
          return super.onOptionsItemSelected(item);
        }
    }

    private void clearCommands() {
      mCommandList.clear();
      mArrayAdapter.notifyDataSetChanged();
    }
    
    private void executeSequence(final List<Command> commands) {
      if (!commands.isEmpty()) {
        mListView.setSelection(mCommandList.size() - commands.size());
        mListView.smoothScrollToPosition(mCommandList.size() - commands.size());
        final Command command = commands.get(0);
        final List<Command> remainingCommands = new ArrayList<Command>(commands);
        remainingCommands.remove(0); // Very inefficient!
        command.execute(mRobot);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                command.finish(mRobot);
                executeSequence(remainingCommands);                
            }
        }, command.getDuration());        
      }      
    }
    
    private final static int STARTUP_ACTIVITY = 1;
    private Robot mRobot;
    public String mOauthToken;

    protected void onActivityResult(int requestCode, int resultCode, Intent data){
      Log.e(TAG, "onActivityResult " + requestCode + " " + resultCode + " " + data);
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == STARTUP_ACTIVITY && resultCode == RESULT_OK) && (mRobot == null)) {
            //Get the connected Robot
            final String robot_id = data.getStringExtra(StartupActivity.EXTRA_ROBOT_ID);
            if(robot_id != null && !robot_id.equals("")){
                mRobot = RobotProvider.getDefaultProvider().findRobot(robot_id);
                FrontLEDOutputCommand.sendCommand(mRobot, 1.0f);
            }
            SpeechAuth auth = SpeechAuth.forService(SpeechConfig.oauthUrl(), 
                SpeechConfig.oauthKey(), SpeechConfig.oauthSecret());
                auth.fetchTo(new OAuthResponseListener());
        }
    }

    protected void onStop() {
      Log.e(TAG, "onStop");
      super.onStop();
      if (mRobot != null) {
        FrontLEDOutputCommand.sendCommand(mRobot, 0.0f);
      }
      mRobot = null;
      RobotProvider.getDefaultProvider().removeAllControls();
    }
    
    /**
     * Handle the result of an asynchronous OAuth check.
     **/
    private class OAuthResponseListener implements SpeechAuth.Client {
      public void handleResponse(String token, Exception error)
      {
        Log.e(TAG, "handleResponse");
        if (token != null) {
          mOauthToken = token;
        }
        else {
          Log.v("SimpleSpeech", "OAuth error: "+error);
          // There was either a network error or authentication error.
          // Show alert for the latter.
          alert("Speech Unavailable", 
              "This app was rejected by the speech service.  Contact the developer for an update.");
        }
      }
    }


    private void alert(String header, String message) {
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setMessage(message)
      .setTitle(header)
      .setCancelable(true)
      .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
          dialog.dismiss();
        }
      });
      AlertDialog alert = builder.create();
      alert.show();
    }    

    /** 
     * Called by the Speak button in the sample activity.
     * Starts the SpeechKit service that listens to the microphone and returns
     * the recognized text.
    **/
    private void startSpeechService() {
      Log.e(TAG, "startSpeechService");
        // The ATTSpeechKit uses a singleton object to interface with the 
        // speech server.
        ATTSpeechService speechService = ATTSpeechService.getSpeechService(this);
        
        // Register for the success and error callbacks.
        speechService.setSpeechResultListener(new ResultListener());
        speechService.setSpeechErrorListener(new ErrorListener());
        // Next, we'll put in some basic parameters.
        // First is the Request URL.  This is the URL of the speech recognition 
        // service that you were given during onboarding.
        try {
            speechService.setRequestUrl(new URI(SpeechConfig.serviceUrl()));
        }
        catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
        
        // Specify the speech context for this app.
        speechService.setSpeechContext("Sms");
        
        // Set the OAuth token that was fetched in the background.
        speechService.setBearerAuthToken(mOauthToken);
        
        // Finally we have all the information needed to start the speech service.  
        speechService.start();
        Log.v("SimpleSpeech", "Starting speech interaction");
    }

    /**
     * This callback object will get all the speech success notifications.
    **/
    private class ResultListener implements ATTSpeechResultListener {

        public void onResult(ATTSpeechResult result) {
          Log.e(TAG, "onResult");
            // The hypothetical recognition matches are returned as a list of strings.
            List<String> textList = result.getTextList();
            if ((textList != null) && (textList.size() > 0) && (textList.get(0).length() > 0)) {
              String suggestions = getSuggestionSummary(textList);
              mTextView.setText(suggestions);
              Command command = mVocabulary.getCommand(textList);
              if (command == null) {
                mTextView.setText(suggestions);
                showCommandChooser(textList);
              }
              else {
                chosenCommand(suggestions, command);
              }
            }
            else {
                // The speech service did not recognize what was spoken.
                Log.v("SimpleSpeech", "Recognized no hypotheses.");
                mTextView.setText("<unrecognized>");
            }
        }

    }

    private void showCommandChooser(final List<String> textList) {
      Log.e(TAG, "showCommandChooser");
      AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
      final String suggestions = getSuggestionSummary(textList);
      dialogBuilder.setTitle("What did you mean by " + suggestions);
      final String[] commandNames = mVocabulary.getCommandNames();
      dialogBuilder.setItems(commandNames, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
          Log.e(TAG, "Chosen " + which + " of " + commandNames + " = " + commandNames[which]);
          Command command = mVocabulary.getCommand(commandNames[which]);
          for (String text : textList) {
            mVocabulary.learnWord(text, commandNames[which]);
          }
          chosenCommand(suggestions, command);
        }
      });
      dialogBuilder.create().show();
    }
    
    private String getSuggestionSummary(List<String> textList) {
      Log.e(TAG, "getSuggestionSummary");
      StringBuilder builder = new StringBuilder();
      for (String string : textList) {
        if (builder.length() != 0) {
          builder.append(" or ");
        }
        builder.append("\"");
        builder.append(string);
        builder.append("\"");
      }
      return builder.toString();
    }

    private void chosenCommand(final String suggestions, Command command) {
      Log.e(TAG, "chosenCommand");
      if (!command.getShortDescription().toLowerCase().equals(suggestions.replace("\"", "").toLowerCase())) {
        mTextView.setText(command.getShortDescription() + " (" + suggestions + ")");
      } else {
        mTextView.setText(command.getShortDescription());
      }
      addCommand(command);
      if (!mPaused) {
        List<Command> commandList = new ArrayList<Command>(1);
        commandList.add(command);
        executeSequence(commandList);
      }
    }

    private void addCommand(Command command) {
      mCommandList.add(command);
      mArrayAdapter.notifyDataSetChanged();
    }    
    
    /**
     * This callback object will get all the speech error notifications.
    **/
    private class ErrorListener implements ATTSpeechErrorListener {
        public void onError(ATTSpeechError error) {
          Log.e(TAG, "onError " + error);

            ErrorType resultCode = error.getType();
            if (resultCode == ErrorType.USER_CANCELED) {
                // The user canceled the speech interaction.
                // This can happen through several mechanisms:
                // pressing a cancel button in the speech UI;
                // pressing the back button; starting another activity;
                // or locking the screen.
                
                // In all these situations, the user was instrumental
                // in canceling, so there is no need to put up a UI alerting 
                // the user to the fact.
                Log.v("SimpleSpeech", "User canceled.");
            }
            else {
                // Any other value for the result code means an error has occurred.
                // The argument includes a message to help the programmer 
              // diagnose the issue.
                String errorMessage = error.getMessage();
                Log.v("SimpleSpeech", "Recognition error #"+resultCode+": "+errorMessage);
                
                mTextView.setText("<server error>");
            }
        }
    }
}

/*
import orbotix.robot.app.StartupActivity;
import orbotix.robot.base.RGBLEDOutputCommand;
import orbotix.robot.base.Robot;
import orbotix.robot.base.RobotProvider;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;

public class MainActivity extends Activity {

  private static final int STARTUP_ACTIVITY = 0;
  private Robot mRobot;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
  }

  @Override
  public void onStart() {
    super.onStart();
    Intent i = new Intent(this, StartupActivity.class);
    startActivityForResult(i, STARTUP_ACTIVITY);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.activity_main, menu);
    return true;
  }

  protected void onActivityResult(int requestCode, int resultCode, Intent data){
    super.onActivityResult(requestCode, resultCode, data);
    if(requestCode == STARTUP_ACTIVITY && resultCode == RESULT_OK){
      //Get the connected Robot
      final String robot_id = data.getStringExtra(StartupActivity.EXTRA_ROBOT_ID);  // 1
      if(robot_id != null && !robot_id.equals("")){
        mRobot = RobotProvider.getDefaultProvider().findRobot(robot_id);          // 2
      }
      //Start blinking
      blink(false);                                                                 // 3
    }
  }

  private void blink(final boolean lit){

    if(mRobot != null){

      //If not lit, send command to show blue light, or else, send command to show no light
      if (lit) {
        RGBLEDOutputCommand.sendCommand(mRobot, 0, 0, 0);
      } else {
        RGBLEDOutputCommand.sendCommand(mRobot, 0, 0, 255);
      }

      //Send delayed message on a handler to run blink again
      final Handler handler = new Handler();
      handler.postDelayed(new Runnable() {
        public void run() {
          blink(!lit);
        }
      }, 1000);
    }
  }
}
*/