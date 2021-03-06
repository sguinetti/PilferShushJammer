# PilferShush Jammer

Research and project page : https://www.cityfreqs.com.au/pilfer.php

Android microphone checker and jamming application built for AOSP LineageOS.  

Application for low battery requirement microphone passive jamming.

Calls audioRecord.startRecording() but DOES NOT READ THE AUDIO BUFFER.

Holds microphone access and should block user apps from gaining focus of microphone.

System telephone calls will override and bump the Jammer from the microphone. 

Adds a notification as a reminder for running while in background.

Tested and blocks Google Voice search (user) app.

Active jammer - tone and white noise versions, boost EQ for higher amplitude.

Scan user installed apps for key features, possible NUHF/ACR SDK package name matches and services/receivers running.

Jammers run as a foreground service

Build update: compile API 29 (Q, 10.0), Android Studio 4.0.1 stable, Gradle 6.1.1

Note: On devices running Android 9 (API level 28) or higher, apps running in the background cannot access the microphone. 
Therefore, your app should record audio only when it's in the foreground or 
**when you include an instance of MediaRecorder in a foreground service.**

Note: Android 10 has new concurrent audio capture policy that means other recording apps can bump a prior recording audio app from the microphone.
see https://source.android.com/compatibility/android-cdd#5_4_5_concurrent_capture

Note: Android 11 (API 30) changes to foreground services access to microphone - "while-in-use access" only.
see https://developer.android.com/preview/privacy/foreground-services

**TODO:**
- URGENT Android 10 concurrent audio test and fix
- check against Concurrent Capture 5.4.5 [C-1-1] AudioSource.VOICE_RECOGNITION
- Android 11 changes to foreground services and mic access, remember its just a trigger
- Android 11 requires manifest dec and foregroundServiceType(80) (microphone)
- Android 11 get ex at mediaRecorder VOICE_COMM as source, Pixel 2(PassiveJammer.java:195) ?
- inconsistent behaviour with widget and jammer service state
- check buffer size reported as being 2048 instead of device actual 8192, overridden with device actual
- Android 10 getting android.app.RemoteServiceException: notify small icon ref, +5 sec delay, invalid notify channel?
- Android 8.x service crashes specifically to its API
- .
- test android.permission.BIND_ACCESSIBILITY_SERVICE (API >= 16 (4.1))
- android 5.1 emu fail to restart jamming activity from dismissed (possible crap emu, inconsistent, gets an anr on service)
- consider optional jammer state persistence over boot <- adding to 4.5.0
- consider min API bump to 23 (6.x)
- rebuild the active jammer
- consider user app summary include and print package name of NUHF/ACR if found


**Changes:**
PENDING 4.5.0
- added passive control appwidget in prep for Android 11
- add boot receiver for auto restart app at device reboot
- add receive boot permission
- androidx deps update

RELEASE 4.4.2
- upgrade build target to API 29, Google Play comply
- update buildtools, platform-tools
- remove lockscreen notify as can cause dupe activity
- getActivity npe bugfix
- auto-backup to false
- remove old drawable xml, possible ex cause
- update gradle dep
- add new SDK
- prep code for Android 11

   vers. 4.4.2
   - min API 18 (4.3)
   - target API 29 (10.x)
   - compiled API 29 (10.x)

   testing devices
   - EMU : Galaxy Nexus 4.3 (18) (Android Studio AVD, no GApps)
   - EMU : Nexus 4 5.1 (22) (Android Studio AVD, no GApps)
   - EMU : Nexus 5X 7.0 (24) (Android Studio AVD, GApps)
   - EMU : Galaxy Nexus Oreo (27) (Android Studio AVD, GApps)
   - EMU : Pixel 3a 10.0 (29) (Android Studio AVD, GApps)
   - LOW : s4 I9195 (deprecated) 4.3.1 (18)(CyanogenMod 10.2, F-Droid)
   - SLO : Mts 5045D (tainted) 6.0.1 (23) (CyanogenMod 13.0, GApps)
   - DEV : s5 G900I (tainted) 10.0 (29)(LineageOS 17.1, GApps)
   - PROD: s5 G900P 7.1.2 (25) (LineageOS 14.1, F-Droid)
 
 
**App screenshots:**
- Home fragment
<img src="https://github.com/kaputnikGo/PilferShushJammer/blob/master/images/PS_Jammer-v4_home.jpg" height="612px" />

- Inspector fragment
<img src="https://github.com/kaputnikGo/PilferShushJammer/blob/master/images/PS_Jammer-v4_inspector.jpg" height="612px" />

- Settings fragment
<img src="https://github.com/kaputnikGo/PilferShushJammer/blob/master/images/PS_Jammer-v4_settings.jpg" height="612px" />
 
**Active Jammer frequency analysis:**
- Active tone, full NUHF range with random scatter drift test
<img src="https://github.com/kaputnikGo/PilferShushJammer/blob/master/images/PS_Jammer-tone_full_nuhf_scatter_drift-test.jpg" height="182px" />
 
- Active tone, slow speed, limited drift test
<img src="https://github.com/kaputnikGo/PilferShushJammer/blob/master/images/PS_Jammer-carrier_limit_drift_speed-test.jpg" height="168px" />

- Active tone, carrier and drift limited, fast
<img src="https://github.com/kaputnikGo/PilferShushJammer/blob/master/images/PS_Jammer-carrier_limit_conform-test.jpg" height="164px" />

- Active jammer (19 kHz carrier, 1000 Hz limit, EQ on) versus ramp-up audio beacon-like signal : scatter jamming demo
<img src="https://github.com/kaputnikGo/PilferShushJammer/blob/master/images/PS_Jammer-vs-html5_synth.jpg" height="138px" />

# 2020 Kaputnik Go


Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
