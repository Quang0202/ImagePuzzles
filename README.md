import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri

object ShareUtils {
    /**
     * Share 1 đường link dạng text/plain qua Android Sharesheet
     * @return true nếu có app xử lý được, false nếu không
     */
    fun shareLink(context: Context, url: String, subject: String? = null, chooserTitle: String? = null): Boolean {
        val cleanUrl = url.trim().let {
            if (it.startsWith("http://") || it.startsWith("https://")) it else "https://$it"
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, cleanUrl)
            subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
            // (Tuỳ chọn) Với Android 12+, EXTRA_TITLE có thể hiện trong một số app
            putExtra(Intent.EXTRA_TITLE, subject ?: chooserTitle ?: "Share link")
        }

        // Kiểm tra có app nhận hay không
        val pm = context.packageManager
        val resolve = sendIntent.resolveActivity(pm) != null
        if (!resolve) return false

        val chooser = Intent.createChooser(sendIntent, chooserTitle ?: "Share link")

        // Nếu context không phải Activity, cần NEW_TASK
        if (context !is Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context.startActivity(chooser)
        return true
    }
}
