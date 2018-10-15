# mtag-sdk-android
Android SDK for the mTag Platform (v2)

## Installation

Currently this library is only available through Github.  Feel free to either clone the repo, or use `jitpack.io`.

##### Using jitpack.io
1. Add the JitPack repository to your build file Add it in your build.gradle at the end of repositories:
```
repositories {
    // ...
    maven { url "https://jitpack.io" }
}
```
2. Add the dependency in the form
```
dependencies {
    implementation 'com.github.BlueBiteLLC:mtag-sdk-android'
}
```


## Usage

1. Add the library to your target's Gradle file.
2. Extend the target Activity with `BlueBiteInteractionDelegate`.
3. Implement the two delegate methods, `interactionDataWasReceived` and `interactionDidFail`.
4. Create an instance of `API` using the `API(BlueBiteInteractionDelegate mDelegate)` constructor, with the Activity from Step 2 as the argument.
5. Upon finding a potential verifiable URL, call `API.interactionWasReceived` and pass the target URL as a String.
6. Handle the response in the `BlueBiteInteractionDelegate` methods.

## More information
There is an example app included in this repo.  Simply clone it and run the `app` target in Android Studio.  The app will attempt to validate any http(s) format NFC tag, displaying output from the Interaction Delegate both in console and in UI.

## License
This SDK is licensed under Apache 2.0, please see the LICENSE.txt file for more information.