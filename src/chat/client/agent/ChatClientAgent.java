package chat.client.agent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.logging.Level;

import jade.content.ContentManager;
import jade.content.Predicate;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;
import jade.util.leap.Iterator;
import jade.util.leap.Set;
import jade.util.leap.SortedSetImpl;
import chat.ontology.ChatOntology;
import chat.ontology.Joined;
import chat.ontology.Left;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.CallLog;
import android.provider.ContactsContract;

public class ChatClientAgent extends Agent implements ChatClientInterface {
  private static final long serialVersionUID = 1594371294421614291L;

  private Logger logger = Logger.getJADELogger(this.getClass().getName());

  private static final String CHAT_ID = "__chat__";
  private static final String CHAT_MANAGER_NAME = "manager";

  private Set participants = new SortedSetImpl();
  private Codec codec = new SLCodec();
  private Ontology onto = ChatOntology.getInstance();
  private ACLMessage spokenMsg;

  private Context context;

  private boolean isFirstLoad;

  List<MissedCall> urgentMissedCalls = new ArrayList<MissedCall>();

  protected void setup() {
    Object[] args = getArguments();
    if (args != null && args.length > 0) {
      if (args[0] instanceof Context) {
        context = (Context) args[0];
      }
    }

    ContentManager cm = getContentManager();
    cm.registerLanguage(codec);
    cm.registerOntology(onto);
    cm.setValidationMode(false);

    addBehaviour(new ParticipantsManager(this));
    addBehaviour(new ChatListener(this));

    spokenMsg = new ACLMessage(ACLMessage.INFORM);
    spokenMsg.setConversationId(CHAT_ID);

    registerO2AInterface(ChatClientInterface.class, this);

    Intent broadcast = new Intent();
    broadcast.setAction("jade.demo.chat.SHOW_CHAT");
    logger.log(Level.INFO, "Sending broadcast " + broadcast.getAction());
    context.sendBroadcast(broadcast);
  }

  protected void takeDown() {
  }

  private void notifyParticipantsChanged() {
    Intent broadcast = new Intent();
    broadcast.setAction("jade.demo.chat.REFRESH_PARTICIPANTS");
    logger.log(Level.INFO, "Sending broadcast " + broadcast.getAction());
    context.sendBroadcast(broadcast);
  }

  private void notifySpoken(String speaker, String sentence) {
    Intent broadcast = new Intent();
    broadcast.setAction("jade.demo.chat.REFRESH_CHAT");
    if (sentence.equals(" joined the chat*")) {
      sentence = sentence.replace(sentence.substring(sentence.length() - 1), "");
      broadcast.putExtra("sentence", speaker + sentence + "\n");
    } else {
      broadcast.putExtra("sentence", speaker + ": " + sentence + "\n");
    }
    logger.log(Level.INFO, "Sending broadcast " + broadcast.getAction());
    context.sendBroadcast(broadcast);
  }

  @SuppressWarnings("unused")
  private void notifyEntry(String speaker, String sentence) {
    Intent broadcast = new Intent();
    broadcast.setAction("jade.demo.chat.NOTIFY_CHAT");
    broadcast.putExtra("sentence", sentence + speaker + " joined the chat" + "\n");
    logger.log(Level.INFO, "Sending broadcast " + broadcast.getAction());
    context.sendBroadcast(broadcast);
  }

  private void notifyExit(String speaker, String sentence) {
    Intent broadcast = new Intent();
    broadcast.setAction("jade.demo.chat.REFRESH_CHAT");
    broadcast.putExtra("sentence", speaker + " left the chat" + "\n");
    logger.log(Level.INFO, "Sending broadcast " + broadcast.getAction());
    context.sendBroadcast(broadcast);
  }

  class ParticipantsManager extends CyclicBehaviour {
    private static final long serialVersionUID = -4845730529175649756L;
    private MessageTemplate template;

    ParticipantsManager(Agent a) {
      super(a);
    }

    public void onStart() {
      ACLMessage subscription = new ACLMessage(ACLMessage.SUBSCRIBE);
      subscription.setLanguage(codec.getName());
      subscription.setOntology(onto.getName());
      String convId = "C-" + myAgent.getLocalName();
      subscription.setConversationId(convId);
      subscription.addReceiver(new AID(CHAT_MANAGER_NAME, AID.ISLOCALNAME));
      myAgent.send(subscription);
      template = MessageTemplate.MatchConversationId(convId);

      isFirstLoad = true;
    }

    public void action() {
      ACLMessage msg = myAgent.receive(template);
      if (msg != null) {
        if (msg.getPerformative() == ACLMessage.INFORM) {
          try {
            Predicate p = (Predicate) myAgent.getContentManager().extractContent(msg);
            if (p instanceof Joined) {
              Joined joined = (Joined) p;
              if (isFirstLoad) {
                isFirstLoad = false;
              } else {
              }

              List<AID> aid = (List<AID>) joined.getWho();
              for (AID a : aid) {
                participants.add(a);
              }
              notifyParticipantsChanged();
            }
            if (p instanceof Left) {
              Left left = (Left) p;
              List<AID> aid = (List<AID>) left.getWho();
              notifyExit(left.getWho().get(0).getLocalName(), "");
              for (AID a : aid)
                participants.remove(a);
              notifyParticipantsChanged();
            }
          } catch (Exception e) {
            Logger.println(e.toString());
            e.printStackTrace();
          }
        } else {
          handleUnexpected(msg);
        }
      } else {
        block();
      }
    }
  }

  class ChatListener extends CyclicBehaviour {
    private static final long serialVersionUID = 741233963737842521L;
    private MessageTemplate template = MessageTemplate.MatchConversationId(CHAT_ID);

    ChatListener(Agent a) {
      super(a);
    }

    public void action() {
      ACLMessage msg = myAgent.receive(template);
      if (msg != null) {
        if (msg.getPerformative() == ACLMessage.INFORM) {
          processMessage(msg.getContent(), msg);
        } else {
          handleUnexpected(msg);
        }
      } else {
        block();
      }
    }
  }

  private void processMessage(String content, ACLMessage msg) {
    String message[] = content.split(":");
    if (message[0].equals("Urgent")) {
      boolean isValidMissedCall = isUrgentMissedCall(message[1]);

      if (isValidMissedCall) {
        notifyUrgent("A missed call by " + message[1] + " was URGENT!");
      }
    } else if (message[0].equals("Notify")) {
      notifyUrgent(message[1]);
    } else if (message[0].equals("RFL")) {
      boolean isPrivateEvent = isPrivateEvent();
      if (!isPrivateEvent) {
        Intent broadcast = new Intent();
        broadcast.setAction("jade.demo.chat.SEND_LOCATION");
        broadcast.putExtra("sentence", message[1]);
        logger.log(Level.INFO, "Sending location to " + message[1] + broadcast.getAction());
        context.sendBroadcast(broadcast);
      }
    } else if (message[0].equals("Location")) {
      Intent broadcast = new Intent();
      broadcast.setAction("jade.demo.chat.CHECK_VICINITY");
      broadcast.putExtra("locationFrom", message[1]);
      broadcast.putExtra("Latitude", message[3]);
      broadcast.putExtra("Longitude", message[5]);
      logger.log(Level.INFO, "Checking vicinity " + broadcast.getAction());
      context.sendBroadcast(broadcast);
    } else {
      notifySpoken(msg.getSender().getLocalName(), msg.getContent());
    }
  }

  private boolean isPrivateEvent() {
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

      Cursor cursor = context.getContentResolver().query(CalendarContract.Events.CONTENT_URI,
          projection, selection, null, null);

      if (cursor.moveToFirst()) {
        do {
          Long timeNow = System.currentTimeMillis();
          if (cursor.getString(0).trim() == "Jog") {
            Timer _timer;
            _timer = new Timer();

            if (timeNow < cursor.getLong(3)) {
              if (cursor.getLong(2) < timeNow
                  && (cursor.getInt(1) == CalendarContract.Events.ACCESS_PRIVATE)) {
                return true;
              }
            }
          }
        } while (cursor.moveToNext());
      }
    } catch (Exception ex) {
      logger.log(Logger.WARNING, "ERROR - Event method: " + ex.toString());
      return false;
    } finally {
    }

    return false;
  }

  private void notifyUrgent(String s) {
    Intent broadcast = new Intent();
    broadcast.setAction("jade.demo.chat.URGENT_CALL_MISSED");
    broadcast.putExtra("sentence", s);
    logger.log(Level.INFO, "Sending broadcast " + broadcast.getAction());
    context.sendBroadcast(broadcast);
  }

  private boolean isUrgentMissedCall(String s) {
    final String[] projection = null;
    final String selection = null;
    final String[] selectionArgs = null;
    final String sortOrder = android.provider.CallLog.Calls.DATE + " DESC";
    Cursor cursor = null;
    Integer MISSED_CALL_TYPE = android.provider.CallLog.Calls.MISSED_TYPE;
    try {
      cursor = context.getContentResolver().query(Uri.parse("content://call_log/calls"),
          projection, selection, selectionArgs, sortOrder);
      while (cursor.moveToNext()) {
        MissedCall missedCall = new MissedCall();

        missedCall.callNumber = cursor.getString(cursor
            .getColumnIndex(android.provider.CallLog.Calls.NUMBER));
        String callDate = cursor.getString(cursor
            .getColumnIndex(android.provider.CallLog.Calls.DATE));
        missedCall.name = cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NAME));
        String callType = cursor.getString(cursor
            .getColumnIndex(android.provider.CallLog.Calls.TYPE));

        missedCall.callDateLong = Long.parseLong(callDate);
        long x = missedCall.callDateLong - System.currentTimeMillis();

        boolean isAlreadyAUrgentCall = false;
        for (MissedCall m : urgentMissedCalls) {
          if (m.callDateLong == missedCall.callDateLong) {
            isAlreadyAUrgentCall = true;
          }
        }

        if (!isAlreadyAUrgentCall && missedCall.name.equals(s)
            && Integer.parseInt(callType) == MISSED_CALL_TYPE && x < 300000) {
          urgentMissedCalls.add(missedCall);
          return true;
        }
      }
    } catch (Exception ex) {
      logger.log(Logger.WARNING, "ERROR - Missed call method: " + ex.toString());
    } finally {
      cursor.close();
    }

    return false;
  }

  private List<String> getCasualMissedCallNames() {
    List<MissedCall> casualMissedCalls = getCasualMissedCalls();
    List<String> names = new ArrayList<String>();
    String participantNames[] = getParticipantNames(context.getContentResolver());
    for (MissedCall missedCall : casualMissedCalls) {
      if (Arrays.asList(participantNames).contains(missedCall.name)
          && !names.contains(missedCall.name)) {
        names.add(missedCall.name);
      }
    }
    return names;
  }

  private List<MissedCall> getCasualMissedCalls() {
    List<MissedCall> casualMissedCalls = new ArrayList<MissedCall>();

    final String[] projection = null;
    final String selection = null;
    final String[] selectionArgs = null;
    final String sortOrder = android.provider.CallLog.Calls.DATE + " DESC";
    Cursor cursor = null;
    Integer MISSED_CALL_TYPE = android.provider.CallLog.Calls.MISSED_TYPE;
    try {
      cursor = context.getContentResolver().query(Uri.parse("content://call_log/calls"),
          projection, selection, selectionArgs, sortOrder);
      while (cursor.moveToNext()) {
        MissedCall missedCall = new MissedCall();

        missedCall.callNumber = cursor.getString(cursor
            .getColumnIndex(android.provider.CallLog.Calls.NUMBER));
        String callDate = cursor.getString(cursor
            .getColumnIndex(android.provider.CallLog.Calls.DATE));
        missedCall.name = cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NAME));
        String callType = cursor.getString(cursor
            .getColumnIndex(android.provider.CallLog.Calls.TYPE));

        missedCall.callDateLong = Long.parseLong(callDate);
        long x = missedCall.callDateLong - System.currentTimeMillis();

        boolean isCasualCall = true;
        for (MissedCall m : urgentMissedCalls) {
          if (m.callDateLong == missedCall.callDateLong) {
            isCasualCall = false;
          }
        }

        String participantNames[] = getParticipantNames(context.getContentResolver());
        if (isCasualCall && Integer.parseInt(callType) == MISSED_CALL_TYPE && x < 86400000) {
          if (Arrays.asList(participantNames).contains(missedCall.name)) {
            casualMissedCalls.add(missedCall);
          }
        }
      }
    } catch (Exception ex) {
      logger.log(Logger.WARNING, "ERROR - Missed call method: " + ex.toString());
    } finally {
      cursor.close();
    }

    return casualMissedCalls;
  }

  private class ChatSpeaker extends OneShotBehaviour {
    private static final long serialVersionUID = -1426033904935339194L;
    private String sentence;
    private String messageTo;

    private ChatSpeaker(Agent a, String s) {
      super(a);
      sentence = s;
      messageTo = "";
    }

    private ChatSpeaker(Agent a, String s, String ss) {
      super(a);
      sentence = s;
      messageTo = ss;
    }

    public void action() {
      spokenMsg.clearAllReceiver();
      Iterator it = participants.iterator();
      if (messageTo.equals("")) {
        while (it.hasNext()) {
          spokenMsg.addReceiver((AID) it.next());
        }
      } else {
        spokenMsg.addReceiver(new AID(messageTo, AID.ISLOCALNAME));
      }
      spokenMsg.setContent(sentence);
      if (messageTo.equals("")) {
        notifySpoken(myAgent.getLocalName(), sentence);
      }
      send(spokenMsg);
    }
  }

  public void handleSpoken(String s) {
    addBehaviour(new ChatSpeaker(this, s));
  }

  public void handleSpoken(String s, String ss) {
    addBehaviour(new ChatSpeaker(this, s, ss));
  }

  public void handleMissedCalls(String s, String nickname) {
    logger.log(Logger.INFO, "in handleMissedCalls ");
    String repeatedCallBy = "";
    logger.log(Logger.INFO, "going in isRepeatedCall");
    repeatedCallBy = isRepeatedCall(s);
    logger.log(Logger.INFO, "repeatedCallBy: " + repeatedCallBy);
    if (repeatedCallBy != null && !repeatedCallBy.equals("")) {
      handleSpoken("Notify:Repeated Calls-Send Email to " + nickname + "!", repeatedCallBy);
    }
  }

  public void requestForLocation(String nickname) {
    List<String> names = getCasualMissedCallNames();
    for (String name : names) {
      handleSpoken("RFL:" + nickname, name);
    }
  }

  private String isRepeatedCall(String s) {
    int callCount = 0;
    String by = "";
    for (MissedCall m : urgentMissedCalls) {
      long x = m.callDateLong - System.currentTimeMillis();
      if (m.callNumber.equals(s) && x < 300000) {
        callCount++;
        by = m.name;
        if (callCount > 2) {
          return by;
        }
      }
    }

    List<MissedCall> casualCalls = getCasualMissedCalls();
    for (MissedCall m : casualCalls) {
      long x = System.currentTimeMillis() - m.callDateLong;
      if (m.callNumber.equals(s) && x < 300000) {
        callCount++;
        by = m.name;
        if (callCount > 2) {
          return by;
        }
      }
    }
    if (callCount > 3) {
      return by;
    } else {
      return "";
    }
  }

  public String[] getParticipantNames(ContentResolver cr) {
    ArrayList<String> contactsList = GetContacts(cr);
    String[] pp = new String[participants.size()];
    Iterator it = participants.iterator();
    int i = 0;
    while (it.hasNext()) {
      AID id = (AID) it.next();
      String appendYorN;

      pp[i++] = id.getLocalName();
    }
    return pp;
  }

  private ArrayList GetContacts(ContentResolver cr) {
    ArrayList<String> contactsList = new ArrayList<String>();

    Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
    if (cur.getCount() > 0) {
      while (cur.moveToNext()) {
        String name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
        contactsList.add(name);
      }
    }
    return contactsList;
  }

  private void handleUnexpected(ACLMessage msg) {
    if (logger.isLoggable(Logger.WARNING)) {
      logger.log(Logger.WARNING, "Unexpected message received from " + msg.getSender().getName());
      logger.log(Logger.WARNING, "Content is: " + msg.getContent());
    }
  }

}
