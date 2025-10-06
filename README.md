```
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="288dp"
    android:height="288dp"
    android:viewportWidth="288"
    android:viewportHeight="288">

    <!-- Coi logo gốc đang vẽ ở toạ độ (0..108 x 0..28).
         Ta chỉ tịnh tiến để nằm giữa khung 288:
         (288-108)/2 = 90, (288-28)/2 = 130 -->
    <group
        android:translateX="90"
        android:translateY="130">
        <!-- DÁN CÁC <path> CỦA LOGO 108x28 VÀO ĐÂY (KHÔNG scaleX/scaleY) -->
        <!-- Ví dụ minh hoạ: -->
        <!-- <path android:fillColor="#FFF" android:pathData="..." /> -->
    </group>
</vector>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:drawable="@color/bootsplash_background"/>

    <!-- Chèn logo vector 108x28, không scale, chỉ canh giữa bằng inset -->
    <item>
        <inset
            android:insetLeft="0dp"
            android:insetRight="0dp"
            android:insetTop="0dp"
            android:insetBottom="0dp">
            <bitmap
                android:src="@drawable/bootsplash_wordmark_108x28"  <!-- vector hoặc png mdpi -->
                android:gravity="center"/>
        </inset>
    </item>
</layer-list>
  ```
