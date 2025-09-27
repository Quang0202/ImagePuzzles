```
adb shell am start -W \
  -n com.your.package/.MainActivity \
  -a android.intent.action.VIEW \
  -d "https://dynamic-link.equix.app/equix-dev?type=order_detail&order-id=AGO-123"
  ```
