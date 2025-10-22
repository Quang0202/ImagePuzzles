```
<resources>
    <style name="Theme.MyApp" parent="Theme.Material3.DayNight.NoActionBar">
        <!-- Splash hệ thống: nền trùng màu splash custom -->
        <item name="android:windowSplashScreenBackground">@color/bootsplash_bg</item>

        <!-- Icon trong suốt + 0ms để không thấy gì -->
        <item name="android:windowSplashScreenAnimatedIcon">@drawable/ic_transparent</item>
        <item name="android:windowSplashScreenAnimationDuration">0</item>
        <item name="android:windowSplashScreenExitAnimationDuration">0</item>

        <!-- Theme sau splash -->
        <item name="android:postSplashScreenTheme">@style/Theme.MyApp.PostSplash</item>
    </style>

    <style name="Theme.MyApp.PostSplash" parent="Theme.Material3.DayNight.NoActionBar" />
</resources>

<resources>
    <style name="Theme.MyApp" parent="Theme.Material3.DayNight.NoActionBar">
        <!-- pre-12 chỉ để nền trùng màu (không logo) để tránh “2 lần splash” -->
        <item name="android:windowBackground">@color/bootsplash_bg</item>
    </style>
</resources>
<resources>
    <color name="bootsplash_bg">#111318</color> <!-- bạn đổi theo palette -->
</resources>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="1dp" android:height="1dp"
    android:viewportWidth="1" android:viewportHeight="1">
    <path android:fillColor="@android:color/transparent" android:pathData="M0,0h1v1h-1z"/>
</vector>
```
<img width="761" height="1011" alt="multi_select_photo" src="https://github.com/user-attachments/assets/3ad0878c-6c8b-4cac-b40d-76df727244c2" />


@Composable
fun EqualHeightHorizontalPager(
    pageCount: Int,
    modifier: Modifier = Modifier,
    content: @Composable (Int) -> Unit
) {
    var maxHeight by remember { mutableStateOf(0) }

    // Đo tất cả các trang một lần để tìm maxHeight
    val measurables = remember {
        (0 until pageCount).map { index -> index }
    }

    BoxWithConstraints(modifier = modifier) {
        val constraints = this.constraints

        LaunchedEffect(pageCount, constraints) {
            // Không thể đo trước khi layout, nên chỉ cần update lại sau khi các page render
        }

        // Đo từng page để tìm ra chiều cao lớn nhất
        measurables.forEach { index ->
            SubcomposeLayout { constraints ->
                val measurable = subcompose(index) { content(index) }.first()
                val placeable = measurable.measure(constraints)
                if (placeable.height > maxHeight) {
                    maxHeight = placeable.height
                }
                layout(0, 0) {}
            }
        }

        // Pager thật sự
        if (maxHeight > 0) {
            HorizontalPager(
                pageCount = pageCount,
                modifier = Modifier.height(with(LocalDensity.current) { maxHeight.toDp() })
            ) { page ->
                content(page)
            }
        } else {
            // Tránh flicker frame đầu tiên
            HorizontalPager(pageCount = pageCount) { page ->
                content(page)
            }
        }
    }
}

