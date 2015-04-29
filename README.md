To build the application you need to download and install in your local maven repository the project
[votingsystem-android-bouncycastle](https://github.com/votingsystem/votingsystem-android-bouncycastle)

Make sure that you've downloaded the latest extras as well as the Android 5.0 SDK via the SDK-Manager.


--------------------------------------------------------------------------------------------------


There's a bug with Android 5.0 SSL that prevents websocket secured connections (wss) with Tyrus and
pre-9 Jetty libraries
https://java.net/projects/tyrus/lists/users/archive/2015-01/message/0
https://code.google.com/p/android/issues/detail?id=103251#c10
https://code.google.com/p/android/issues/detail?id=93740