# keep classes used for Json deserializing
-keep class org.unifiedpush.distributor.nextpush.api.response.** { *; }
# keep classes used for Nextcloud SSO
-keep class org.unifiedpush.distributor.nextpush.api.provider.** { *; }

# preserve line numbers for crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile