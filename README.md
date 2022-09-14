# ConFlake and FlakReaper

This repository contains our flaky test dataset ConFlake and the reproducing tool FlakReaper.

### [ConFlake Artifacts: Benchmark, RootCauses, and RunTime Data](https://flakytestreproducer.github.io/)

### How to use FlakReaper
1. APK Instrumentation
    - cd FlakReaper/AndroidAppInstrument
	- mvn compile
	- mvn exec:java -Dexec.mainClass="Main" -Dexec.args="apkDir(the location of the apk to be instrumented)"

2. Install the instrumented apk(FlakReaper/AndroidAppInstrument/sootOutput/*.apk) on the emulator. Before installing the apk, you should align and sign it by zipalign and apksigner.

3. Reproduce Flaky Test
    - mvn exec:java -Dexec.mainClass="RunTest" -Dexec.args="test command(e.g., de.test.antennapod.ui.MainActivityTest#testAddFeed de.test.antennapod/androidx.test.runner.AndroidJUnitRunner de.danoeh.antennapod.debug)"



