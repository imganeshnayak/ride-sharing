<p align="center">
  <img src="./app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" />
</p>

# RiiDE
RiiDE is a carpooling app that lets users search through, join or create trips to any given destination, and also helps managing the pricing, passengers, contact info and timings.

This app was roughly developed in a month from week-end to week-end as an end of cycle CS Bachelor's degree project, so it will not get any further updates after it's done.

# Screenshots

|![screenshot0](./screenshots/screenshot0.jpg)|![screenshot1](./screenshots/screenshot1.jpg)|![screenshot2](./screenshots/screenshot2.jpg)|
|---|---|---|

# Features
- Auth
- Trip management
- Passanger management
- User profiles
- Search capabilities & indexing
- Rating system
- English & French localization

# Build
Simply cloning and building with android studio should do the trick, make sure to use the latest androidx libraries.

# Hosting
This app is hosted on a Firebase instance, uses its auth system (that i slightly modified to include usernames), and the Firestore Database.

To host it with another Firebase instance, delete `./app/google-services.json` and replace it with your own generated one, no other configurations needed other than the database rules, as the code will automatically initialize the database and no cloud functions are used.

# DISCLAIMER
This project being my first android project, and because of the very limiting deadline and my lack of experience wth android, the code has a few major issues:
- Since i don't count on updating this on the future, i did not follow code architecture standards such as MVVM.
- This is NOT a service, just a small project, and due to the above reasons i do not advice using the code as an example and cannot guarantee anything.
