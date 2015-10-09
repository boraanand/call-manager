# Call Manager
##An Adaptive Interaction Manager Application

As the number of communication channels increase, so should be the opportunities for
individuals to interact. Yet, we all fail to interact efficiently. For example, we fail to return
important calls, read important messages in time, or resort to a phone call when the person
we want to reach is a few feet away. We can attribute the situation to social applications
that focus more on what to do and how to do something, rather than why to do it. Consider
that a user employs an agent to manage his interactions. The agent decides when and how
to remind the user of a missed interaction opportunity. 

###Consider the following scenarios:
a. A user misses a call during work hours. However, the caller's agent noties the user's
(callee's) agent that the call was important. Then, the user's agent reminds the user
immediately.

b. A user missed a call from a friend the previous evening and the user's agent didn't
remind the user of the call as the call was casual. However, today, when the user is
jogging in a park, the friend is also jogging in the same park. The agent, then, reminds
the user of the missed call and the proximity to the friend. Note that the friend's agent
may not always want to disclose the friend's location to the user's agent due to privacy
concerns.

c. A user forgot his phone at home and a friend tries to call him repeatedly. The user's
agent knows about the user forgetting the phone and asks the caller to send an email
to the user.

###Design considerations:
1.	Built as an add-on to the [JADE](https://code.google.com/p/jchat4android/) Chat Client  
2.	Along with the group chat, user can avail new call manager features.
3.	The JADE server and chat-manager should be running for agents to join in. 
(Run: \standard\bin\startPlatform.bat)
4.	A “friend” is someone who is on our contact list and an agent on the JADE sever
5.	Implemented the callee call manager and caller call manager in the same app and any user connecting to JADE chat server can take up any role simultaneously.
6.	App runs in background, and notifications are showed to user as push notifications. App doesn’t need to be in foreground to get these notifications. But do not exit the app by pressing the back button, press the home button to run the app in background.

###Initial Setup:
1.	Start JADE server from \standard\bin\startPlatform.bat
2.	Run app on two emulators or two phones. App has been tested to work on Android 4.1.2 
3.	Let first one have nickname say “Alice” (caller) and second one has nickname “Bob” (callee)
4.	As “Bob” is callee, set up  a contact for Alice in Bob’s  phone with number say “888-888-8888” and same name “Alice”

###Adapting to an urgent missed call:
1.	Simulate missed call from Alice: Using DDMS make a call to Bob using the number saved for Alice
2.	If Alice wants to send a urgent missed call notification to Bob, select Bob’s name from drop down list and click on “Send Urgent Notification Button”
3.	If Bob detects a “Urgent:Alice” message and a missed call from Alice, app will show a push notification that missed call by Alice was urgent.

###Friend in vicinity:
1.	When the app starts, it check the calendar for an event containing the word “Jog”. 
2.	If this Jog event is currently in progress, app will check for and previous non-urgent missed call from any of it’s friend, and send a “RFL” message.
3.	If Jog event starts at a later time, a timer is set to request location when the event starts.
4.	If any changes in calendar, press “Refresh Calendar Events” button to get new Jog events.
5.	When a friend receives a RFL message, it will check it’s calendar. If there is a event going on with its Privacy set as private, it will not send its location, or else send the location.
6.	When location is received, distance between self and friend is calculated, and if it’s less than 50 meters, a push notification tells the user that this friend is in vicinity

###Phone away
1.	If a friend makes repeated missed calls, application will record if there are more than 3 or 4 missed call in the last 5 minutes by same friend, if so it will send a “Send email message”. 
2.	If send email message is received at caller, a push notification will alert caller to contact callee by email.

##Files modified:
###1.	Chat.xml: 
        Added buttons to refresh calendar events and send urgent missed call message.

#####2.	ChatActivity.java: 
