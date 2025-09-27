```
adb shell cmd package query-intent-activities \
  -a android.intent.action.VIEW \
  -c android.intent.category.BROWSABLE \
  -d "https://dynamic-link.equix.app/equix-dev"
  ```
