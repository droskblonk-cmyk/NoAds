# LSPosed loads the entry class by the name written in assets/xposed_init.
-keep class io.github.sevincfurkan.noads.ModuleEntry { <init>(); }

# The Xposed API is provided by the framework at runtime.
-dontwarn de.robv.android.xposed.**
