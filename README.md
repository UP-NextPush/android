# NextPush - Android

UnifiedPush provider for Nextcloud - android application 

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/org.unifiedpush.distributor.nextpush/)

## Requirements

**Nextcloud Server**

[Server App Install](https://github.com/UP-NextPush/server-app)

**Android Apps**

[0] [Nextcloud Application](https://f-droid.org/packages/com.nextcloud.client/) - For SSO login (recommended)

[1] [NextPush Client](https://f-droid.org/en/packages/org.unifiedpush.distributor.nextpush/) - This app

[2] [Applications supporting UnifiedPush](https://unifiedpush.org/users/apps/)

[3] [UP-Example](https://f-droid.org/en/packages/org.unifiedpush.example/) - UnifiedPush Test Client - For testing purposes only. Not required for operation.

## Usage

1. (Recommended) Install and sign into your Nextcloud account using the official Nextcloud Application [0].
2. Install the NextPush client [1] and sign into your Nextcloud account.
  a. (Recommended) With the Nextcloud file application (SSO)
  b. Manually, you will need to create an application password for NextPush.
3. Install one application supporting UnifiedPush [2], or UP-Example [3]. Login into the application if you need to, for instance with your mastodon account or with your matrix account.
4. The application will automatically detect NextPush and use it to send notifications.

## Credit

This application has been inspired by [Nextcloud Push Notifier](https://gitlab.com/Nextcloud-Push/nextcloud-push-notifier)
