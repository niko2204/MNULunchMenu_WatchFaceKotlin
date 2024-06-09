# MNU Lunch Menu watch face
## 안드로이드 watchface 샘플을 이용하여 목포대학교 홈페이지에서 점심 메뉴를 가져오는 코드입니다. 
### 예시
![example] <img src="https://github.com/niko2204/MNULunchMenu_WatchFaceKotlin/assets/5626825/a3c76983-07b0-4b89-a4ea-2fc37c3001b9">

<img src="https://github.com/niko2204/MNULunchMenu_WatchFaceKotlin/assets/5626825/fee52453-afb7-4ba2-a9ab-4d117ce3910b">

WatchFace Sample (Kotlin)
===============================
Demonstrates watch faces using the androidX libraries (Kotlin). **The newer [Watch Face Format][1]
(WFF) is recommended in preference to using these libraries.**

Introduction
------------
The AndroidX watch face libraries allow you to develop a watch face service in Kotlin. However,
[WFF][1] is strongly recommended in preference to this approach.

Steps to build in Android Studio
--------------------------------
Because a watch face only contains a service, that is, there is no Activity, you first need to turn
off the launch setting that opens an Activity on your device.

To do that (and once the project is open) go to Run -> Edit Configurations. Select the **app**
module and the **General** tab. In the Launch Options, change **Launch:** to **Nothing**. This will
allow you to successfully install the watch face on the Wear device.

When installed, you will need to select the watch face in the watch face picker, i.e., the watch
face will not launch on its own like a regular app.

Screenshots
-------------

<img src="screenshots/analog-face.png" width="400" alt="Analog Watchface"/>
<img src="screenshots/analog-watch-side-config-all.png" width="400" alt="Analog Watchface Config"/>
<img src="screenshots/analog-watch-side-config-1.png" width="400" alt="Analog Watchface Config"/>
<img src="screenshots/analog-watch-side-config-2.png" width="400" alt="Analog Watchface"/>

Getting Started
---------------

This sample uses the Gradle build system. To build this project, use the "gradlew build" command or
use "Import Project" in Android Studio.

Support
-------

- Stack Overflow: https://stackoverflow.com/questions/tagged/wear-os

If you've found an error in this sample, please file an issue:
https://github.com/android/wear-os-samples

Patches are encouraged, and may be submitted by forking this project and
submitting a pull request through GitHub. Please see CONTRIBUTING.md for more details.

[1]: https://developer.android.com/training/wearables/wff
