Pushwoosh Android SDK
=====================
[![GitHub release](https://img.shields.io/github/release/Pushwoosh/pushwoosh-andorid-sdk.svg?style=flat-square)](https://github.com/Pushwoosh/pushwoosh-android-sdk/releases) 
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.pushwoosh/pushwoosh/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.pushwoosh/pushwoosh)

The SDK uses JobIntentService. You must include the following maven repository URL in build.gradle:

	allprojects {
	    repositories {
	        jcenter ()
	        maven {
	            url "https://maven.google.com"
	         }
	    } 
	}
	
In addition, add the following dependencies.

	dependencies {
	   ...
	   compile 'com.android.support:support-compat:26.+'
	   compile 'com.google.android.gms:play-services-gcm:11.+'
	}

[Pushwoosh.aar](https://github.com/Pushwoosh/pushwoosh-android-sdk/blob/master/pushwoosh.aar) - compiled version of Pushwoosh Android SDK. Includes all necessary AndroidManifest.xml changes required for receiving Amazon and GCM push notifications.

Maven integration:

	<dependency>
  		<groupId>com.pushwoosh</groupId>
  		<artifactId>pushwoosh</artifactId>
  		<version>5.1.1</version>
	</dependency>

Gradle integration:

	compile 'com.pushwoosh:pushwoosh:5.1.1'


The guide for SDK integration is available on Pushwoosh website:  
http://docs.pushwoosh.com/docs/android-sdk

Documentation:
https://rawgit.com/Pushwoosh/pushwoosh-android-sdk/master/Documentation/index.html

Samples:
https://github.com/Pushwoosh/pushwoosh-android-sdk/tree/master/Samples

Pushwoosh team
http://www.pushwoosh.com
