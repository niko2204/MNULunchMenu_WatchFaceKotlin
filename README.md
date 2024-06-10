# MNU Lunch Menu watch face
## 개요
##### 늘 학교식당을 갈때마다 무슨 메뉴일지 모르고 가서 답답하였다. 식당에 가서도 주메뉴가 무엇인지 모르기 때문에, 뒤에 자장면이 나오는데 모르고 밥을 많이 담기도 해서 난감할때가 많았다. 스마트폰으로 대학 홈페이지에 들어가 볼 수도 있지만, 그렇게까지 점심 메뉴를 알아내기 위해 노력하고 싶지는 않았다. 이런 문제를 해결하기 위해 휴일 동안 스마트워치로 점심메뉴를 가져오는 기능을 구현했다. 미적 감각이 없어서 이쁘지 않아 다른 분들이 이쁘게 만들어 주면 좋겠다. 
##### 안드로이드 watchface 샘플을 이용하여 목포대학교 홈페이지에서 점심 메뉴를 가져오는 코드입니다. 
## 주요기능
### watchface는 코틀린 예제를 사용함
### 홈페이지에 접속하여 메뉴를 가져오는 코루틴 작성
### 시간과 요일에 따라 watchface에 정보를 표현하는 함수 작성
## 예시
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
