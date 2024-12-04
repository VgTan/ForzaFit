import android.content.Context
import java.util.Properties

fun Context.getApiKey(): String {
    val properties = Properties()
    assets.open("local.properties").use { properties.load(it) }
    return properties.getProperty("MAPS_API_KEY") ?: ""
}
