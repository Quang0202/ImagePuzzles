```
adb shell am start -W \
  -a android.intent.action.VIEW \
  -c android.intent.category.BROWSABLE \
  -d "https://example.equix.com/equix-dev?type=order_detail&order-id=123"
  ```
