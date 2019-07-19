# Feedbackr - Android App

The original App was created by Anton Borries as part of his bachelor thesis. Since then most parts
of the source code have been improved, deleted or refactored by Sebastian Meyer as part of his project work.

The App was created at the research group Software Engineering and Programming Languages (stups.hhu.de).

To see which exact components of the app were edited, please check the commit history.

## What is Feedbackr?
Feedbackr allows the user to discover and create Feedback for his current location. The Feedback
can be of possible or negative nature. The Feedback can be enriched by adding a rating between 0-5 stars,
a description, a photo or adding it to a predefined set of available categories.

The user might choose to set the feedback to private, which will hide it from other users.

Using the MapView one is able to check for feedback near his current location. By enabling or
disabling the available filters the feedback can be filtered as needed.

Every user is authenticated anonymously at firebase, were the data is stored.

## Configure Firebase
https://console.firebase.google.com
- update google-services.json, the file can be accessed via the firebase console
    - Create a new project
    - Enable anonymous auth
    - Enable database
    - Update the security rules (the used rules can be found under /firebase_rules
- Anonymous auth
    - userId does not need to be pushed to the firebase we can still query for Feedbacks using the local firebase userId
    - after reinstalling the app a new auth-key is assigned to the user
    - feedback might not be accessed, as the assigned user is not available anymore
    - the feedback can still be deleted via firebase directly
- Database
    - requires network connection
    - with no network connection, the feedback will be transmitted the next time a network connection is available if persisted mode is set
        - firebaseDatabase.setPersistenceEnabled(true)
- Crash Reporting
    - after adding the dependencies the crash reporting is active
    - additional reports can be send manually

## Creating the APK
1. Open the Build Variants inside Android Studio and select the desired Build Variant (debug/release)
2. Make sure you have configured the keystore.properties correctly
    1. Check keystore.properties.example for more information
3. Select Build > Build APK(s)

If you were unable to build the APK make sure your configuration is correct
- the root directory contains keystore.jks and keystore.properties with the correct information
- the build variant is set to release
- the apps build.gradle contains the correct signingsConfigs

Creating a new keystore will result in a different signature and thus the app can only be republished to the
App store but actual updates of the app are not possible.

## What has been done?
1. Firebase
    1. Security Rules adjusted
    2. Crash Reporting updated
    3. Structure refactored to meet new requirements
    4. The firebase configuration is owned by (s.meyer@hhu.de) and (stups@hhu.de)
2. Multithreading
    1. Load Feedback on demand, store information for the session
        1. For feedback list
        2. For the map view
    2. Load image, display image
    3. Map View
        1. The mapView's method getMapAsync needs to be executed on the MainThread and might cause some delay but there seems to be no straight forward solution to this problem
3. Bugfixes
    1. GPS deactivated, start app, cancel, ok, fragment doesn't change
        1. Switch between tab feedback and map to fix the viw
    2. Start App and immediately switch to map -> Crash
    3. Don't request all feedback from the database use geofire to limit radius
    4. Feedback view sometimes does not render completely
        1. check SupportFragmentManager and load fragment on createView?
    5. Add image and stars/rating to the feedback dialog
4. Improvements
    1. Feedback has a image now
        1. Persist Images locally on first demand
        2. All images are stored in firebase storage
    2. Interact with Map position and load new data on demand
        1. Feedback radius is 3km
        2. Update the Feedback center on click
    3. Added about to display general information and the licenses
    4. Responsiveness greatly improved over several activities
    5. Feedback view
        1. The feedback is saved directly after issuing one
        2. On back the changes are saved
        3. Unnecessary buttons removed to improve the user experience
    6. The visual have been improved (SendFeedback, Dialogs, ...)
5. Google Play
    1. Google Play account configured for the app
    2. First version added to google play which is available to a closed Alpha-Tester group
	
## License
The App is licensed and distributed under the MIT license.