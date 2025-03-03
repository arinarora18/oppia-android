package org.oppia.android.app.home.recentlyplayed

import android.content.Context
import android.content.Intent
import android.os.Bundle
import org.oppia.android.app.activity.InjectableAppCompatActivity
import org.oppia.android.app.home.RouteToExplorationListener
import org.oppia.android.app.model.ExplorationCheckpoint
import org.oppia.android.app.player.exploration.ExplorationActivity
import org.oppia.android.app.resumelesson.ResumeLessonActivity
import org.oppia.android.app.topic.RouteToResumeLessonListener
import javax.inject.Inject

/** Activity for recent stories. */
class RecentlyPlayedActivity :
  InjectableAppCompatActivity(),
  RouteToExplorationListener,
  RouteToResumeLessonListener {

  @Inject
  lateinit var recentlyPlayedActivityPresenter: RecentlyPlayedActivityPresenter

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    activityComponent.inject(this)
    val internalProfileId = intent.getIntExtra(
      RECENTLY_PLAYED_ACTIVITY_INTERNAL_PROFILE_ID_KEY,
      -1
    )
    recentlyPlayedActivityPresenter.handleOnCreate(internalProfileId)
  }

  companion object {
    // TODO(#1655): Re-restrict access to fields in tests post-Gradle.
    const val RECENTLY_PLAYED_ACTIVITY_INTERNAL_PROFILE_ID_KEY =
      "RecentlyPlayedActivity.internal_profile_id"

    /** Returns a new [Intent] to route to [RecentlyPlayedActivity]. */
    fun createRecentlyPlayedActivityIntent(context: Context, internalProfileId: Int): Intent {
      val intent = Intent(context, RecentlyPlayedActivity::class.java)
      intent.putExtra(RECENTLY_PLAYED_ACTIVITY_INTERNAL_PROFILE_ID_KEY, internalProfileId)
      return intent
    }
  }

  override fun routeToExploration(
    internalProfileId: Int,
    topicId: String,
    storyId: String,
    explorationId: String,
    backflowScreen: Int?,
    isCheckpointingEnabled: Boolean
  ) {
    startActivity(
      ExplorationActivity.createExplorationActivityIntent(
        this,
        internalProfileId,
        topicId,
        storyId,
        explorationId,
        backflowScreen,
        isCheckpointingEnabled
      )
    )
  }

  override fun routeToResumeLesson(
    internalProfileId: Int,
    topicId: String,
    storyId: String,
    explorationId: String,
    backflowScreen: Int?,
    explorationCheckpoint: ExplorationCheckpoint
  ) {
    startActivity(
      ResumeLessonActivity.createResumeLessonActivityIntent(
        this,
        internalProfileId,
        topicId,
        storyId,
        explorationId,
        backflowScreen,
        explorationCheckpoint
      )
    )
  }
}
