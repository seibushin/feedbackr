#Feedbackr - Android App

The original App was created by Anton Borries as part of his bachelor thesis. Since then most parts
of the source code have been improved, deleted or refactored by Sebastian Meyer as part of his project work.

To see which parts were edited, please check the commit history.

##Configure Firebase
- update google-services.json, the file can be accessed via the firebase console
-- Create a new project
-- Enable anonymous auth
-- Enable database
-- Update the security rules (the used rules can be found under /firebase_rules
- Anonymous auth
-- userId does not need to be pushed to the firebase we can still query for Feedbacks using the local firebase userId
-- after reinstalling the app a new auth-key is assigned to the user
-- feedback might not be accessed, as the assigned user is not available anymore
-- the feedback can still be deleted via firebase directly
- Database
-- requires network connection
-- with no network connection, the feedback will be transmitted the next time a network connection is available if persisted mode is set
--- firebaseDatabase.setPersistenceEnabled(true)
- Crash Reporting
-- after adding the dependencies the crash reporting is active
-- additional reports can be send manually

##Creating the APK
###Signing Configs
keystore.properties
(keystore.properties.example)
keystore.jks

##What has been done?
1. Firebase
1.1. Security Rules adjusted
1.2. Crash Reporting updated
1.3. Structure refactored to meet new requirements
1.4. The firebase configuration is linked to my private account (s.meyer@hhu.de)

2. Multithreading
2.1. Load Feedback on demand, store information for the session
2.2.1. For feedback list
2.2.2. For the map view
2.2. Load image, display image
2.3. Map View
2.3.1. The mapView's method getMapAsync needs to be executed on the MainThread and might cause some delay but there seems to be no straight forward solution to this problem

3. Bugfixes
3.1. GPS deactivated, start app, cancel, ok, fragment doesn't change
3.1.1. Switch between tab feedback and map to fix the viw
3.2. Start App and immediately switch to map -> Crash
3.2. Don't request all feedback from the database use geofire to limit radius
3.3. Feedback view sometimes does not render completely
3.3.1. check SupportFragmentManager and load fragment on createView?
3.4. Add image and stars/rating to the feedback dialog

4. Improvements
4.1. Feedback has a image now
4.1.1. Persist Images locally on first demand
4.1.2. All images are stored in firebase storage
4.2. Interact with Map position and load new data on demand
4.2.1. Feedback radius is 3km
4.2.2. Update the Feedback center on click
4.3. Added about to display general information and the licenses
4.4. Responsiveness greatly improved over several activities
4.5. Feedback view
4.5.1. The feedback is saved directly after issuing one
4.5.2. On back the changes are saved
4.5.3. Unnecessary buttons removed to improve the user experience

4. Google Play
4.1. Google Play account configured for the app
4.2. First version added to google play which is available to a closed Alpha-Tester group