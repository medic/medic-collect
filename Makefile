ADB = ${ANDROID_HOME}/platform-tools/adb
EMULATOR = ${ANDROID_HOME}/tools/emulator
GRADLEW = ./gradlew

ifdef ComSpec	 # Windows
  # Use `/` for all paths, except `.\`
  ADB := $(subst \,/,${ADB})
  EMULATOR := $(subst \,/,${EMULATOR})
  GRADLEW := $(subst /,\,${GRADLEW})
endif

default: deploy android-logs

android-emulator:
	nohup ${EMULATOR} -avd test -wipe-data > emulator.log 2>&1 &
	${ADB} wait-for-device
android-logs:
	${ADB} shell logcat

deploy: clean
	${GRADLEW} --daemon --parallel assembleDebug
	rm -f build/outputs/apk/*-unaligned.apk
	ls build/outputs/apk/*-debug.apk | \
					xargs -n1 ${ADB} install -r

clean:
	rm -rf build/outputs/apk/

base: clean
	${GRADLEW} --daemon --parallel installBaseDebug
