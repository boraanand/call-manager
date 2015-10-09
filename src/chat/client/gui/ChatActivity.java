package chat.client.gui;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import jade.core.MicroRuntime;
import jade.util.Logger;
import jade.wrapper.ControllerException;
import jade.wrapper.O2AException;
import jade.wrapper.StaleProxyException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.location.Criteria;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.provider.CalendarContract;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import chat.client.agent.ChatClientInterface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.util.Log;
import android.widget.Toast;

public class ChatActivity extends Activity implements LocationListener {

  protected LocationManager locationManager;
  protected LocationListener locationListener;
  protected Context context;
  String mylocation;

  private Logger logger = Logger.getJADELogger(this.getClass().getName());

  static final int PARTICIPANTS_REQUEST = 0;

  private MyReceiver myReceiver;
  private TeleListener teleListener;

  static boolean ring = false;
  static boolean callReceived = false;

  private String nickname;
  private ChatClientInterface chatClientInterface;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Task 3
    locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0,
        (LocationListener) this);

    Bundle extras = getIntent().getExtras();
    if (extras != null) {
      nickname = extras.getString("nickname");
    }

    try {
      chatClientInterface = MicroRuntime.getAgent(nickname).getO2AInterface(
          ChatClientInterface.class);
    } catch (StaleProxyException e) {
      showAlertDialog(getString(R.string.msg_interface_exc), true);
    } catch (ControllerException e) {
      showAlertDialog(getString(R.string.msg_controller_exc), true);
    }

    myReceiver = new MyReceiver();

    IntentFilter refreshChatFilter = new IntentFilter();
    refreshChatFilter.addAction("jade.demo.chat.REFRESH_CHAT");
    registerReceiver(myReceiver, refreshChatFilter);

    IntentFilter clearChatFilter = new IntentFilter();
    clearChatFilter.addAction("jade.demo.chat.CLEAR_CHAT");
    registerReceiver(myReceiver, clearChatFilter);

    IntentFilter notifyChatFilter = new IntentFilter();
    notifyChatFilter.addAction("jade.demo.chat.NOTIFY_CHAT");
    registerReceiver(myReceiver, notifyChatFilter);

    IntentFilter notifyParticipantFilter = new IntentFilter();
    notifyParticipantFilter.addAction("jade.demo.chat.REFRESH_PARTICIPANTS");
    registerReceiver(myReceiver, notifyParticipantFilter);

    IntentFilter notifyUrgentCallMissed = new IntentFilter();
    notifyUrgentCallMissed.addAction("jade.demo.chat.URGENT_CALL_MISSED");
    registerReceiver(myReceiver, notifyUrgentCallMissed);

    IntentFilter notifyLocation = new IntentFilter();
    notifyLocation.addAction("jade.demo.chat.SEND_LOCATION");
    registerReceiver(myReceiver, notifyLocation);

    IntentFilter notifyVicinity = new IntentFilter();
    notifyVicinity.addAction("jade.demo.chat.CHECK_VICINITY");
    registerReceiver(myReceiver, notifyVicinity);

    setContentView(R.layout.chat);

    Button button = (Button) findViewById(R.id.button_send);
    button.setOnClickListener(buttonSendListener);

    Button urgentButton = (Button) findViewById(R.id.button_urgent);
    urgentButton.setOnClickListener(buttonUrgentListener);

    Button refreshLocationButton = (Button) findViewById(R.id.button_refreshLocation);
    refreshLocationButton.setOnClickListener(buttonRefreshLocationListener);

    UpdateParticipant();

    teleListener = new TeleListener();
    TelephonyManager TelephonyMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
    TelephonyMgr.listen(teleListener, PhoneStateListener.LISTEN_CALL_STATE);

    getCurrentEvent();
  }

  private void testMethod_timer() {
    Timer _timer;
    _timer = new Timer();

    Date nowPlus10sec = new Date();
    Calendar cal = Calendar.getInstance();
    cal.setTime(nowPlus10sec);
    cal.add(Calendar.SECOND, 15);
    nowPlus10sec = cal.getTime();
    _timer.schedule(new AlarmTask(), nowPlus10sec);
    Toast.makeText(getApplicationContext(), "Invoking timer! ", Toast.LENGTH_LONG).show();
  }

  private void UpdateParticipant() {
    Spinner dropdown = (Spinner) findViewById(R.id.spinner_participant_list);
    String[] items = chatClientInterface.getParticipantNames(getContentResolver());
    ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
        android.R.layout.simple_spinner_item, items);
    dropdown.setAdapter(adapter);
    logger.log(Level.INFO, "Update participants!");
  }

  class TeleListener extends PhoneStateListener {
    public void onCallStateChanged(final int state, final String incomingNumber) {
      super.onCallStateChanged(state, incomingNumber);
      ChatActivity.this.runOnUiThread(new Runnable() {
        public void run() {

          logger.log(Logger.INFO, "In TeleListener method ");
          logger.log(Logger.INFO, "super.onCallStateChanged");
          switch (state) {

          case TelephonyManager.CALL_STATE_IDLE:
            logger.log(Logger.INFO, "TelephonyManager.CALL_STATE_IDLE ");
            if (ring == true && callReceived == false) {
              processMissedCalls(incomingNumber);
            }
            break;

          case TelephonyManager.CALL_STATE_OFFHOOK:
            logger.log(Logger.INFO, "TelephonyManager.CALL_STATE_OFFHOOK ");
            callReceived = true;
            break;

          case TelephonyManager.CALL_STATE_RINGING:
            ring = true;
            logger.log(Logger.INFO, "TelephonyManager.CALL_STATE_RINGING");
            break;

          default:
            break;
          }

        }
      });

    }

  }

  class AlarmTask extends TimerTask {
    public void run() {
      ChatActivity.this.runOnUiThread(new Runnable() {
        public void run() {
          Toast.makeText(ChatActivity.this, "Invoking timer for calendar!", Toast.LENGTH_SHORT)
              .show();
          chatClientInterface.requestForLocation(nickname);
        }
      });
    }
  }

  private void processMissedCalls(String incomingNumber) {
    chatClientInterface.handleMissedCalls(incomingNumber, nickname);
  }

  private void getCurrentEvent() {
    try {
      Calendar c = Calendar.getInstance();

      String[] projection = new String[] { CalendarContract.Events.TITLE,
          CalendarContract.Events.ACCESS_LEVEL, CalendarContract.Events.DTSTART,
          CalendarContract.Events.DTEND, CalendarContract.Events.CALENDAR_ID,
          CalendarContract.Events.DESCRIPTION, CalendarContract.Events.ALL_DAY,
          CalendarContract.Events.EVENT_LOCATION };

      SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yy");
      Date dateCC = new Date();
      Calendar startTime = Calendar.getInstance();
      startTime.setTime(dateCC);
      startTime.set(Calendar.HOUR_OF_DAY, 00);
      startTime.set(Calendar.MINUTE, 00);
      startTime.set(Calendar.SECOND, 00);

      SimpleDateFormat formatterr = new SimpleDateFormat("MM/dd/yy hh:mm:ss");
      Calendar endTime = Calendar.getInstance();
      Date dateCCC = new Date();
      endTime.setTime(dateCCC);
      endTime.set(Calendar.HOUR_OF_DAY, 23);
      endTime.set(Calendar.MINUTE, 59);
      endTime.set(Calendar.SECOND, 59);

      String selection = "(( " + CalendarContract.Events.DTSTART + " >= "
          + startTime.getTimeInMillis() + " ) AND ( " + CalendarContract.Events.DTSTART + " <= "
          + endTime.getTimeInMillis() + " ))";

      Cursor cursor = this.getBaseContext().getContentResolver()
          .query(CalendarContract.Events.CONTENT_URI, projection, selection, null, null);

      if (cursor.moveToFirst()) {
        do {
          Long timeNow = System.currentTimeMillis();
          if (cursor.getString(0).contains("Jog")) {
            Timer _timer;
            _timer = new Timer();

            if (timeNow < cursor.getLong(3)) {
              if (cursor.getLong(2) < timeNow) {
                chatClientInterface.requestForLocation(nickname);
              } else {
                Date jogTime = new Date(cursor.getLong(2));
                _timer.schedule(new AlarmTask(), jogTime);
                Toast.makeText(getApplicationContext(),
                    "Invoking timer at Jog time: " + jogTime.toString(), Toast.LENGTH_LONG).show();
              }

            }
          }
        } while (cursor.moveToNext());
        cursor.close();
      }
    } catch (Exception ex) {
      logger.log(Logger.WARNING, "ERROR - Event method: " + ex.toString());
    } finally {
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    unregisterReceiver(myReceiver);

    logger.log(Level.INFO, "Destroy activity!");
  }

  private OnClickListener buttonSendListener = new OnClickListener() {
    public void onClick(View v) {
      final EditText messageField = (EditText) findViewById(R.id.edit_message);

      String message = messageField.getText().toString();
      if (message != null && !message.equals("")) {
        try {
          chatClientInterface.handleSpoken(message);
          messageField.setText("");
        } catch (O2AException e) {
          showAlertDialog(e.getMessage(), false);
        }
      }

    }
  };

  private OnClickListener buttonUrgentListener = new OnClickListener() {
    public void onClick(View v) {

      Spinner dropdown = (Spinner) findViewById(R.id.spinner_participant_list);
      String messageTo = dropdown.getSelectedItem().toString();
      String message = "Urgent:" + nickname + ":" + messageTo;
      try {
        chatClientInterface.handleSpoken(message, messageTo);
      } catch (O2AException e) {
        showAlertDialog(e.getMessage(), false);
      }
    }
  };

  private OnClickListener buttonRefreshLocationListener = new OnClickListener() {
    public void onClick(View v) {
      getCurrentEvent();
    }
  };

  public void createNotification(String s) {

    NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

    String MyText = "Call manager notification";
    Notification mNotification = new Notification(R.drawable.icon, MyText,
        System.currentTimeMillis());

    String MyNotificationTitle = "";
    String MyNotificationText = s;

    Intent MyIntent = new Intent();
    PendingIntent StartIntent = PendingIntent.getActivity(getApplicationContext(), 0, MyIntent,
        PendingIntent.FLAG_CANCEL_CURRENT);

    mNotification.setLatestEventInfo(getApplicationContext(), MyNotificationTitle,
        MyNotificationText, StartIntent);

    int NOTIFICATION_ID = 1;
    notificationManager.notify(NOTIFICATION_ID, mNotification);

  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.chat_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case R.id.menu_participants:
      Intent showParticipants = new Intent(ChatActivity.this, ParticipantsActivity.class);
      showParticipants.putExtra("nickname", nickname);
      startActivityForResult(showParticipants, PARTICIPANTS_REQUEST);
      return true;
    case R.id.menu_clear:
      final TextView chatField = (TextView) findViewById(R.id.chatTextView);
      chatField.setText("");
      return true;
    default:
      return super.onOptionsItemSelected(item);
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == PARTICIPANTS_REQUEST) {
      if (resultCode == RESULT_OK) {
      }
    }
  }

  private class MyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      logger.log(Level.INFO, "Received intent " + action);
      if (action.equalsIgnoreCase("jade.demo.chat.REFRESH_CHAT")) {
        final TextView chatField = (TextView) findViewById(R.id.chatTextView);
        chatField.append(intent.getExtras().getString("sentence"));
        scrollDown();
      }
      if (action.equalsIgnoreCase("jade.demo.chat.NOTIFY_CHAT")) {
        final TextView chatField = (TextView) findViewById(R.id.chatTextView);
        chatField.append(intent.getExtras().getString("sentence"));
        scrollDown();
      }
      if (action.equalsIgnoreCase("jade.demo.chat.CLEAR_CHAT")) {
        final TextView chatField = (TextView) findViewById(R.id.chatTextView);
        chatField.setText("");
      }
      if (action.equalsIgnoreCase("jade.demo.chat.REFRESH_PARTICIPANTS")) {
        UpdateParticipant();
      }
      if (action.equalsIgnoreCase("jade.demo.chat.URGENT_CALL_MISSED")) {
        createNotification(intent.getExtras().getString("sentence"));
      }
      if (action.equalsIgnoreCase("jade.demo.chat.SEND_LOCATION")) {
        String messageTo = intent.getExtras().getString("sentence");
        SendLocation(messageTo);
      }
      if (action.equalsIgnoreCase("jade.demo.chat.CHECK_VICINITY")) {
        String messageFrom = intent.getExtras().getString("locationFrom");
        double Latitude = Double.parseDouble(intent.getExtras().getString("Latitude"));
        double Longitude = Double.parseDouble(intent.getExtras().getString("Longitude"));
        checkVicinity(messageFrom, Latitude, Longitude);
      }

    }
  }

  private void checkVicinity(String messageFrom, double friendLatitude, double friendLongitude) {
    locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    Criteria criteria = new Criteria();
    criteria.setAccuracy(Criteria.ACCURACY_FINE);
    criteria.setAltitudeRequired(false);
    criteria.setBearingRequired(false);
    criteria.setCostAllowed(true);
    criteria.setPowerRequirement(Criteria.POWER_LOW);

    String provider = locationManager.getBestProvider(criteria, true);
    Location location = locationManager.getLastKnownLocation(provider);
    if (location == null) {
      return;
    }
    mylocation = updateWithNewLocation(location);

    String latLongString;

    if (location != null) {
      double lat = location.getLatitude();
      double lng = location.getLongitude();

      double distance = getDistance(lat, lng, friendLatitude, friendLongitude);

      if (distance < 50.00000) {
        createNotification(messageFrom + " is in vicinity!!");
      }
    }
  }

  private void SendLocation(String messageTo) {
    locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    Criteria criteria = new Criteria();
    criteria.setAccuracy(Criteria.ACCURACY_FINE);
    criteria.setAltitudeRequired(false);
    criteria.setBearingRequired(false);
    criteria.setCostAllowed(true);
    criteria.setPowerRequirement(Criteria.POWER_LOW);

    String provider = locationManager.getBestProvider(criteria, true);
    Location location = locationManager.getLastKnownLocation(provider);
    if (location != null)
      mylocation = updateWithNewLocation(location);
    else
      mylocation = "";
    try {
      chatClientInterface.handleSpoken("Location:" + nickname + ":" + mylocation, messageTo);
    } catch (O2AException e) {
      showAlertDialog(e.getMessage(), false);
    }
  }

  private void scrollDown() {
    final TextView chatField = (TextView) findViewById(R.id.chatTextView);
  }

  @Override
  public void onSaveInstanceState(Bundle savedInstanceState) {
    final TextView chatField = (TextView) findViewById(R.id.chatTextView);
    savedInstanceState.putString("chatField", chatField.getText().toString());
    super.onSaveInstanceState(savedInstanceState);
  }

  @Override
  public void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    final TextView chatField = (TextView) findViewById(R.id.chatTextView);
    chatField.setText(savedInstanceState.getString("chatField"));
  }

  private void showAlertDialog(String message, final boolean fatal) {
    AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this);
    builder.setMessage(message).setCancelable(false)
        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id) {
            dialog.cancel();
            if (fatal)
              finish();
          }
        });
    AlertDialog alert = builder.create();
    alert.show();
  }

  @Override
  public void onLocationChanged(Location location) {
  }

  @Override
  public void onProviderDisabled(String provider) {
    Log.d("Latitude", "disable");
  }

  @Override
  public void onProviderEnabled(String provider) {
    Log.d("Latitude", "enable");
  }

  @Override
  public void onStatusChanged(String provider, int status, Bundle extras) {
    Log.d("Latitude", "status");
  }

  private String updateWithNewLocation(Location location) {

    String latLongString;

    if (location != null) {
      double lat = location.getLatitude();
      double lng = location.getLongitude();
      double loc1distance = getDistance(lat, lng, 35.77198, -78.67385);
      double loc2distance = getDistance(lat, lng, 38.90719, -77.03687);
      double loc3distance = getDistance(lat, lng, 48.85661, 2.35222);

      if (loc1distance < 50.00000) {
        latLongString = "Latitude: " + lat + ":Longitude: " + lng + ":This is EBII (Centennial)";
      } else if (loc2distance < 50.00000) {
        latLongString = "Latitude: " + lat + ":Longitude: " + lng + ":This is Washington DC";
      } else if (loc3distance < 50.00000) {
        latLongString = "Latitude: " + lat + ":Longitude: " + lng + ":This is Paris";
      } else {
        latLongString = "Latitude: " + lat + ":Longitude: " + lng;
      }
    } else {
      latLongString = "No location found";
    }

    return latLongString;
  }

  private static double getDistance(double lat1, double lon1, double lat2, double lon2) {
    final double Radius = 6371 * 1E3;
    double dLat = Math.toRadians(lat2 - lat1);
    double dLon = Math.toRadians(lon2 - lon1);
    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1))
        * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return Radius * c;
  }
}
