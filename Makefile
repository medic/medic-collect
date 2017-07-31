ADB = ${ANDROID_HOME}/platform-tools/adb
EMULATOR = ${ANDROID_HOME}/tools/emulator
GRADLEW = ./gradlew --daemon --parallel

ifdef ComSpec	 # Windows
  # Use `/` for all paths, except `.\`
  ADB := $(subst \,/,${ADB})
  EMULATOR := $(subst \,/,${EMULATOR})
  GRADLEW := $(subst /,\,${GRADLEW})
endif

.PHONY: android-emulator android-logs base clean default deploy deploy-unbranded update-bikram

default: deploy-unbranded android-logs

android-emulator:
	nohup ${EMULATOR} -avd test -wipe-data > emulator.log 2>&1 &
	${ADB} wait-for-device
android-logs:
	${ADB} shell logcat | tee android.log

deploy-unbranded:
	${GRADLEW} installDemoDebug

deploy: clean
	${GRADLEW} assembleDebug
	rm -f build/outputs/apk/*-unaligned.apk
	ls build/outputs/apk/*-debug.apk | \
					xargs -n1 ${ADB} install -r

clean:
	rm -rf build/outputs/apk/

base: clean
	${GRADLEW} --daemon --parallel installBaseDebug

update-bikram:
	echo "TODO these dependencies should be released to a public Maven repo and downloaded from there."
	(cd ../bikram-sambat && make assemble-java)
	cp ../bikram-sambat/java/android-lib/build/outputs/aar/android-lib-release.aar libs/bikram-sambat-android-SNAPSHOT.aar
	cp ../bikram-sambat/java/lib/build/libs/bikram-sambat.jar libs/
	echo "Bikram updated with local SNAPSHOTs."
