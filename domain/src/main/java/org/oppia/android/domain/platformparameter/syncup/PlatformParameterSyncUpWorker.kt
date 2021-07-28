package org.oppia.android.domain.platformparameter.syncup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.oppia.android.app.model.PlatformParameter
import org.oppia.android.app.utility.getVersionName
import org.oppia.android.data.backends.gae.api.PlatformParameterService
import org.oppia.android.domain.oppialogger.OppiaLogger
import org.oppia.android.domain.platformparameter.PlatformParameterController
import org.oppia.android.util.threading.BackgroundDispatcher
import javax.inject.Inject

/** Worker class that fetches anc cache the latest platform parameters from the remote service. */
class PlatformParameterSyncUpWorker private constructor(
  context: Context,
  params: WorkerParameters,
  private val platformParameterController: PlatformParameterController,
  private val platformParameterService: PlatformParameterService,
  private val oppiaLogger: OppiaLogger,
  @BackgroundDispatcher private val backgroundDispatcher: CoroutineDispatcher
) : CoroutineWorker(context, params) {

  companion object {
    const val WORKER_TYPE_KEY = "worker_type_key"
    const val TAG = "PlatformParameterWorker.tag"
    const val PLATFORM_PARAMETER_WORKER = "platform_parameter_worker"
  }

  override suspend fun doWork(): Result {
    return when (inputData.getString(WORKER_TYPE_KEY)) {
      PLATFORM_PARAMETER_WORKER -> withContext(backgroundDispatcher) { refreshPlatformParameters() }
      else -> Result.failure()
    }
  }

  // It's used to parse the Map of Platform Parameters to a List of Platform Parameter
  private fun parseNetworkResponse(response: Map<String, Any>): List<PlatformParameter> {
    val platformParameterList: MutableList<PlatformParameter> = mutableListOf()
    for (entry in response.entries) {
      val platformParameter = PlatformParameter.newBuilder().setName(entry.key)
      when (val value = entry.value) {
        is String -> platformParameter.string = value
        is Int -> platformParameter.integer = value
        is Boolean -> platformParameter.boolean = value
        else -> continue
      }
      platformParameterList.add(platformParameter.build())
    }
    return platformParameterList
  }

  /** Extracts platform parameters from the remote service and store them in the cache store */
  private fun refreshPlatformParameters(): Result {
    return try {
      val response = platformParameterService.getPlatformParametersByVersion(
        applicationContext.getVersionName()
      ).execute()
      val responseBody = checkNotNull(response.body())
      val platformParameterList = parseNetworkResponse(responseBody)
      platformParameterController.updatePlatformParameterDatabase(platformParameterList)
      Result.success()
    } catch (e: Exception) {
      oppiaLogger.e(TAG, "Failed to fetch the Platform Parameters", e)
      Result.failure()
    }
  }

  /** Creates an instance of [PlatformParameterSyncUpWorker] by properly injecting dependencies. */
  class Factory @Inject constructor(
    private val platformParameterController: PlatformParameterController,
    private val platformParameterService: PlatformParameterService,
    private val oppiaLogger: OppiaLogger,
    @BackgroundDispatcher private val backgroundDispatcher: CoroutineDispatcher
  ) {
    fun create(context: Context, params: WorkerParameters): CoroutineWorker {
      return PlatformParameterSyncUpWorker(
        context,
        params,
        platformParameterController,
        platformParameterService,
        oppiaLogger,
        backgroundDispatcher
      )
    }
  }
}
