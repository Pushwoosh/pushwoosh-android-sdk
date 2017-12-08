Pushwoosh Android SDK
=====================
[![GitHub release](https://img.shields.io/github/release/Pushwoosh/pushwoosh-andorid-sdk.svg?style=flat-square)](https://github.com/Pushwoosh/pushwoosh-android-sdk/releases) 
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.pushwoosh/pushwoosh/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.pushwoosh/pushwoosh)

The SDK uses JobIntentService. You must include the following maven repository URL in build.gradle:

	allprojects {
	    repositories {
	        jcenter ()
	        google()
	    } 
	}
	
In addition, add the following dependencies.

	dependencies {
	   ...
	   compile 'com.android.support:support-compat:26.+'
	   compile 'com.google.firebase:firebase-messaging:11.+'
	   compile 'com.pushwoosh.java7:pushwoosh:5.5.2'
	}
Starting with Pushwoosh SDK 5.1.1, FCM library is used by default, therefore, you need to integrate Firebase into your project correctly. Please refer to [Getting Started](http://docs.pushwoosh.com/docs/fcm-integration) guide.

For GCM integrtion please refer to [GCM Integration (legacy)](http://docs.pushwoosh.com/docs/gcm-integration-legacy) guide.

[Pushwoosh.aar](https://github.com/Pushwoosh/pushwoosh-android-sdk/blob/master/Pushwoosh.aar) - compiled version of Pushwoosh Android SDK. Includes all necessary AndroidManifest.xml changes required for receiving FCM push notifications. For Amazon integration please add the following dependency:

	compile 'com.pushwoosh.java7:pushwoosh-amazon:5.5.2'

Maven integration:

	<dependency>
  		<groupId>com.pushwoosh.java7</groupId>
  		<artifactId>pushwoosh</artifactId>
  		<version>5.5.2</version>
	</dependency>

Documentation:
https://rawgit.com/Pushwoosh/pushwoosh-android-sdk/master/Documentation/index.html

Samples:
https://github.com/Pushwoosh/pushwoosh-android-sdk/tree/master/Samples

Pushwoosh team
http://www.pushwoosh.com
