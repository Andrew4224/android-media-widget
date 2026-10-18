# Glance instantiates ActionCallback implementations reflectively by class name.
-keep class * implements androidx.glance.appwidget.action.ActionCallback {
    public <init>();
}

# Manifest components (receiver, service, activities) are kept by AAPT-generated rules.
