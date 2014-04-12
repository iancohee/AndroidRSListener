Android Reverse Shell Listener
==============================
Ian Cohee
--------

Listener designed for my Android Reverse Shell. 

Usage

    java -classpath /path/to/classFile:/path/to/bcprov-jdk15on-146.jar Listener /path/to/server.keystore port

Where the password is
    
    thepasswordgoes

for the default key in the keystore. If you want to use a custom key you need to import it into the Reverse Shell's TrustStore.  
