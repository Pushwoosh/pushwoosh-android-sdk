# Custom LiveUpdateProgressStyleProvider impls are looked up via Class.forName by FQN from
# AndroidManifest meta-data "com.pushwoosh.LIVE_UPDATE_STYLE_PROVIDER".
-keep class * implements com.pushwoosh.liveupdates.LiveUpdateProgressStyleProvider { *; }

# ManifestValidator.java  Class.forName lookup of the LiveUpdateProgressStyleProvider base interface
# (core module resolves it reflectively because pushwoosh-liveupdates is an optional dependency).
-keep interface com.pushwoosh.liveupdates.LiveUpdateProgressStyleProvider { *; }
