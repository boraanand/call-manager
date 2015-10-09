package chat.client.agent;

import android.content.ContentResolver;

public interface ChatClientInterface {
  public void handleSpoken(String s);

  public void handleSpoken(String s, String ss);

  public String[] getParticipantNames(ContentResolver cr);

  public void handleMissedCalls(String incomingNumber, String nickname);

  public void requestForLocation(String nickname);
}
