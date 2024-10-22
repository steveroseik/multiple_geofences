import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.roseik.multiple_geofences.GeofenceService

class RebootJobService : JobService() {
    override fun onStartJob(params: JobParameters?): Boolean {
        Log.d("GEOFENCING JOB REBOOT", "Reregistering geofences!")
        val restartIntent = Intent(applicationContext, GeofenceService::class.java)
        ContextCompat.startForegroundService(applicationContext, restartIntent)
        jobFinished(params, false)
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        return true
    }
}