README file for chatStandard

INTRODUCTION
============
This package contains a demo of a chat application to run on standard platform.

LICENSE
=======
see file License.

FEEDBACK
=======
As you know already, this is still an on-going project. 
We are still working on the framework and new versions will be distributed 
as soon as available.
Your feedback as users is very important to us. Please, if you have new 
requirements that you would like to see implemented or if you have examples 
of usage or if you discover some bugs, send us information.
Check the website http://jade.tilab.com/
for how to report bugs and send suggestions.  

SYSTEM REQUIREMENTS
===================
To build the the executable libraries at least a Java Development Kit version 1.5 is required. 


KNOWN BUGS
==========
See http://jade.tilab.com/  ('Bugs' page)  for the full list of reported bugs


CONTACT
=======
Michele Izzo - Telecomitalia S.p.A.
e-mail: michele.izzo@telecomitalia.it

Giovanni Iavarone - Telecomitalia S.p.A.
e-mail: giovanni.iavarone@telecomitalia.it 


INSTALLATION AND TEST
==============================
You can compile the sources includes or use the pre-compiled binaries (JAR files). 


Software requirements
=========================
The only software requirement to execute the system is the Java Run Time 
Environment version 1.5.
The Java Compiler version 1.5, to build the and the ANT program to compile the source code of 
chatStandard with build.xml file, ANT is available from http://jakarta.apache.org.

Getting the software
========================
All the software is distributed under the LGPL license limitations. 
It can be downloaded from the JADE web site 
http://jade.tilab.com/ Five compressed files are available:
1. the source code of JADE
2. the source code of the demo
3. the documentation, including the javadoc of the JADE API and 
this programmer's guide
4. the binary of JADE, i.e. the jar files with all the Java classes
5. a full distribution with all the previous files


Running chatStandard from the binary distribution
=============================================
Having uncompressed the archive file, a directory tree is generated whose 
root is jade and with a lib and a bin subdirectory. The lib subdirectory contains 
the JAR files of the JADE release 4.1 platform, the Ontology that is the
same for client and server and the chatStandard itself.

To run the server side platform you need to enter the bin subdirectory and type:
   startChatServer.bat (or startChatServer.sh on unix systems)

To run a participant to you chat in the same bin directory you can type:
   startChatParticipant.bat (or startChatParticipiant.sh on unix)

You can start more participants simply repeating the last step.
 
In windows systems you can also simply double click on the bat 
in the folder from the files explorer.