# NextPush - Android

UnifiedPush provider for Nextcloud - android application 

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/org.unifiedpush.distributor.nextpush/)

## Requirements

**Nextcloud Server**

[Server App Install](https://github.com/UP-NextPush/server-app)

**Android Apps**

[1] [Nextcloud Application](https://f-droid.org/packages/com.nextcloud.client/) - **Required**

[2] [NextPush Client](https://f-droid.org/en/packages/org.unifiedpush.distributor.nextpush/) - This app

[3] [Applications supporting UnifiedPush](https://unifiedpush.org/users/apps/)

[4] [UP-Example](https://f-droid.org/en/packages/org.unifiedpush.example/) - UnifiedPush Test Client - For testing purposes only. Not required for operation.

## Usage

1. Install and sign into your Nextcloud account using the official Nextcloud Application [1].
2. Install the NextPush client [2] and sign into your Nextcloud account.
3. Install one application supporting UnifiedPush [3], or UP-Example [4]. Login into the application if you need to, for instance with your mastodon account or with your matrix account.
4. The application will automatically detect NextPush and use it to send notifications.

## Credit

This application has been inspired by [Nextcloud Push Notifier](https://gitlab.com/Nextcloud-Push/nextcloud-push-notifier)
