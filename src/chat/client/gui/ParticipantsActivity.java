package chat.client.gui;

import java.util.ArrayList;
import java.util.logging.Level;

import chat.client.agent.ChatClientInterface;
import jade.core.MicroRuntime;
import jade.util.Logger;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.provider.ContactsContract;
import android.widget.Toast;

public class ParticipantsActivity extends ListActivity {
  private Logger logger = Logger.getJADELogger(this.getClass().getName());

  private MyReceiver myReceiver;

  private String nickname;
  private ChatClientInterface chatClientInterface;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Bundle extras = getIntent().getExtras();
    if (extras != null) {
      nickname = extras.getString("nickname");
    }

    try {
      chatClientInterface = MicroRuntime.getAgent(nickname).getO2AInterface(
          ChatClientInterface.class);
    } catch (StaleProxyException e) {
      e.printStackTrace();
    } catch (ControllerException e) {
      e.printStackTrace();
    }

    myReceiver = new MyReceiver();

    IntentFilter refreshParticipantsFilter = new IntentFilter();
    refreshParticipantsFilter.addAction("jade.demo.chat.REFRESH_PARTICIPANTS");
    registerReceiver(myReceiver, refreshParticipantsFilter);

    setContentView(R.layout.participants);

    setListAdapter(new ArrayAdapter<String>(this, R.layout.participant,
        chatClientInterface.getParticipantNames(getContentResolver())));

    ListView listView = getListView();
    listView.setTextFilterEnabled(true);
    listView.setOnItemClickListener(listViewtListener);
  }

  private OnItemClickListener listViewtListener = new OnItemClickListener() {
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

      final String participant = chatClientInterface.getParticipantNames(getContentResolver())[position];

      if (participant.charAt(participant.length() - 2) == 'N') {
        new AlertDialog.Builder(ParticipantsActivity.this)
            .setTitle("Create Contact")
            .setMessage(
                "Create contact for " + participant.substring(0, participant.length() - 4) + " ?")
            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int which) {
                addContact(participant.substring(0, participant.length() - 4));

                Context context = getApplicationContext();
                CharSequence text = "Contact created!";
                int duration = Toast.LENGTH_SHORT;

                Toast toast = Toast.makeText(context, text, duration);
                toast.show();

                finish();
              }
            }).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int which) {
                finish();
              }
            }).setIcon(android.R.drawable.ic_dialog_alert).show();

      } else {
        finish();
      }
    }
  };

  private void addContact(String participant) {
    String DisplayName = participant;

    ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

    ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
        .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
        .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null).build());

    if (DisplayName != null) {
      ops.add(ContentProviderOperation
          .newInsert(ContactsContract.Data.CONTENT_URI)
          .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
          .withValue(ContactsContract.Data.MIMETYPE,
              ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
          .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, DisplayName)
          .build());
    }

    try {
      getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    unregisterReceiver(myReceiver);

    logger.log(Level.INFO, "Destroy activity!");
  }

  private class MyReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      logger.log(Level.INFO, "Received intent " + action);
      if (action.equalsIgnoreCase("jade.demo.chat.REFRESH_PARTICIPANTS")) {
        setListAdapter(new ArrayAdapter<String>(ParticipantsActivity.this, R.layout.participant,
            chatClientInterface.getParticipantNames(getContentResolver())));
      }
    }
  }

}
