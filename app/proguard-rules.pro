# Glance instantiates ActionCallback implementations reflectively by class name.
-keep class * implements androidx.glance.appwidget.action.ActionCallback {
    public <init>();
}

# Glance uses WorkManager, which pulls in an old Room (2.2.x). Room creates the generated
# WorkDatabase_Impl reflectively, but its consumer rule only keeps the class, not the
# constructor, so R8 full mode strips it and the app crashes on startup.
# Room is only a transitive runtime dependency, so the IDE cannot resolve the class here; R8 can.
#noinspection ShrinkerUnresolvedReference
-keep class * extends androidx.room.RoomDatabase {
    <init>();
}

# Manifest components (receiver, service, activities) are kept by AAPT-generated rules.
