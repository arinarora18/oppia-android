package org.oppia.android.app.settings.profile

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.test.core.app.ActivityScenario.launch
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isFocusable
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.Component
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.oppia.android.R
import org.oppia.android.app.activity.ActivityComponent
import org.oppia.android.app.administratorcontrols.AdministratorControlsActivity
import org.oppia.android.app.application.ActivityComponentFactory
import org.oppia.android.app.application.ApplicationComponent
import org.oppia.android.app.application.ApplicationInjector
import org.oppia.android.app.application.ApplicationInjectorProvider
import org.oppia.android.app.application.ApplicationModule
import org.oppia.android.app.application.ApplicationStartupListenerModule
import org.oppia.android.app.devoptions.DeveloperOptionsModule
import org.oppia.android.app.devoptions.DeveloperOptionsStarterModule
import org.oppia.android.app.model.ProfileId
import org.oppia.android.app.shim.ViewBindingShimModule
import org.oppia.android.app.topic.PracticeTabModule
import org.oppia.android.app.utility.OrientationChangeAction.Companion.orientationLandscape
import org.oppia.android.data.backends.gae.NetworkConfigProdModule
import org.oppia.android.data.backends.gae.NetworkModule
import org.oppia.android.domain.classify.InteractionsModule
import org.oppia.android.domain.classify.rules.continueinteraction.ContinueModule
import org.oppia.android.domain.classify.rules.dragAndDropSortInput.DragDropSortInputModule
import org.oppia.android.domain.classify.rules.fractioninput.FractionInputModule
import org.oppia.android.domain.classify.rules.imageClickInput.ImageClickInputModule
import org.oppia.android.domain.classify.rules.itemselectioninput.ItemSelectionInputModule
import org.oppia.android.domain.classify.rules.multiplechoiceinput.MultipleChoiceInputModule
import org.oppia.android.domain.classify.rules.numberwithunits.NumberWithUnitsRuleModule
import org.oppia.android.domain.classify.rules.numericinput.NumericInputRuleModule
import org.oppia.android.domain.classify.rules.ratioinput.RatioInputModule
import org.oppia.android.domain.classify.rules.textinput.TextInputRuleModule
import org.oppia.android.domain.exploration.lightweightcheckpointing.ExplorationStorageModule
import org.oppia.android.domain.hintsandsolution.HintsAndSolutionConfigModule
import org.oppia.android.domain.hintsandsolution.HintsAndSolutionProdModule
import org.oppia.android.domain.onboarding.ExpirationMetaDataRetrieverModule
import org.oppia.android.domain.oppialogger.LogStorageModule
import org.oppia.android.domain.oppialogger.loguploader.LogUploadWorkerModule
import org.oppia.android.domain.platformparameter.PlatformParameterModule
import org.oppia.android.domain.profile.ProfileManagementController
import org.oppia.android.domain.question.QuestionModule
import org.oppia.android.domain.topic.PrimeTopicAssetsControllerModule
import org.oppia.android.domain.workmanager.WorkManagerConfigurationModule
import org.oppia.android.testing.AccessibilityTestRule
import org.oppia.android.testing.TestLogReportingModule
import org.oppia.android.testing.profile.ProfileTestHelper
import org.oppia.android.testing.robolectric.RobolectricModule
import org.oppia.android.testing.threading.TestCoroutineDispatchers
import org.oppia.android.testing.threading.TestDispatcherModule
import org.oppia.android.testing.time.FakeOppiaClockModule
import org.oppia.android.util.accessibility.AccessibilityTestModule
import org.oppia.android.util.caching.testing.CachingTestModule
import org.oppia.android.util.data.DataProviders.Companion.toLiveData
import org.oppia.android.util.gcsresource.GcsResourceModule
import org.oppia.android.util.logging.LoggerModule
import org.oppia.android.util.logging.firebase.FirebaseLogUploaderModule
import org.oppia.android.util.networking.NetworkConnectionDebugUtilModule
import org.oppia.android.util.networking.NetworkConnectionUtilDebugModule
import org.oppia.android.util.parser.html.HtmlParserEntityTypeModule
import org.oppia.android.util.parser.image.GlideImageLoaderModule
import org.oppia.android.util.parser.image.ImageParsingModule
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import javax.inject.Inject
import javax.inject.Singleton

/** Tests for [ProfileEditActivity]. */
@RunWith(AndroidJUnit4::class)
@LooperMode(LooperMode.Mode.PAUSED)
@Config(
  application = ProfileEditActivityTest.TestApplication::class,
  qualifiers = "port-xxhdpi"
)
class ProfileEditActivityTest {
  @get:Rule
  val accessibilityTestRule = AccessibilityTestRule()

  @Inject
  lateinit var context: Context

  @Inject
  lateinit var profileTestHelper: ProfileTestHelper

  @Inject
  lateinit var profileManagementController: ProfileManagementController

  @Inject
  lateinit var testCoroutineDispatchers: TestCoroutineDispatchers

  @Before
  fun setUp() {
    Intents.init()
    setUpTestApplicationComponent()
    testCoroutineDispatchers.registerIdlingResource()
    profileTestHelper.initializeProfiles()
  }

  @After
  fun tearDown() {
    testCoroutineDispatchers.unregisterIdlingResource()
    Intents.release()
  }

  private fun setUpTestApplicationComponent() {
    ApplicationProvider.getApplicationContext<TestApplication>().inject(this)
  }

  @Test
  fun testProfileEdit_updateName_checkNewNameDisplayed() {
    profileManagementController.updateName(
      ProfileId.newBuilder().setInternalId(1).build(),
      newName = "Akshay"
    )
    launch<ProfileEditActivity>(
      ProfileEditActivity.createProfileEditActivity(
        context = context,
        profileId = 1
      )
    ).use {
      testCoroutineDispatchers.runCurrent()
      onView(withId(R.id.profile_edit_toolbar)).check(matches(hasDescendant(withText("Akshay"))))
      onView(withId(R.id.profile_edit_name)).check(matches(withText("Akshay")))
    }
  }

  @Test
  fun testProfileEdit_startWithAdminProfile_checkAdminInfoIsDisplayed() {
    launch<ProfileEditActivity>(
      ProfileEditActivity.createProfileEditActivity(
        context = context,
        profileId = 0
      )
    ).use {
      testCoroutineDispatchers.runCurrent()
      onView(withId(R.id.profile_edit_toolbar)).check(matches(hasDescendant(withText("Admin"))))
      onView(withId(R.id.profile_edit_name)).check(matches(withText("Admin")))
      onView(withId(R.id.profile_edit_allow_download_heading)).check(matches(not(isDisplayed())))
      onView(withId(R.id.profile_edit_allow_download_sub)).check(matches(not(isDisplayed())))
      onView(withId(R.id.profile_edit_allow_download_switch)).check(matches(not(isDisplayed())))
      onView(withId(R.id.profile_delete_button)).check(matches(not(isDisplayed())))
    }
  }

  @Test
  fun testProfileEdit_configChange_startWithAdminProfile_checkAdminInfoIsDisplayed() {
    launch<ProfileEditActivity>(
      ProfileEditActivity.createProfileEditActivity(
        context = context,
        profileId = 0
      )
    ).use {
      testCoroutineDispatchers.runCurrent()
      onView(isRoot()).perform(orientationLandscape())
      onView(withId(R.id.profile_edit_toolbar)).check(matches(hasDescendant(withText("Admin"))))
      onView(withId(R.id.profile_edit_name)).check(matches(withText("Admin")))
      onView(withId(R.id.profile_edit_allow_download_heading)).check(matches(not(isDisplayed())))
      onView(withId(R.id.profile_edit_allow_download_sub)).check(matches(not(isDisplayed())))
      onView(withId(R.id.profile_edit_allow_download_switch)).check(matches(not(isDisplayed())))
      onView(withId(R.id.profile_delete_button)).check(matches(not(isDisplayed())))
    }
  }

  @Test
  fun testProfileEdit_startWithUserProfile_checkUserInfoIsDisplayed() {
    launch<ProfileEditActivity>(
      ProfileEditActivity.createProfileEditActivity(
        context = context,
        profileId = 1
      )
    ).use {
      testCoroutineDispatchers.runCurrent()
      onView(withId(R.id.profile_edit_toolbar)).check(matches(hasDescendant(withText("Ben"))))
      onView(withId(R.id.profile_edit_name)).check(matches(withText("Ben")))
      onView(withId(R.id.profile_edit_allow_download_heading)).check(matches((isDisplayed())))
      onView(withId(R.id.profile_edit_allow_download_sub)).check(matches((isDisplayed())))
      onView(withId(R.id.profile_edit_allow_download_switch)).check(matches((isDisplayed())))
      onView(withId(R.id.profile_delete_button)).check(matches((isDisplayed())))
    }
  }

  @Test
  fun testProfileEdit_configChange_startWithUserProfile_checkUserInfoIsDisplayed() {
    launch<ProfileEditActivity>(
      ProfileEditActivity.createProfileEditActivity(
        context = context,
        profileId = 1
      )
    ).use {
      testCoroutineDispatchers.runCurrent()
      onView(isRoot()).perform(orientationLandscape())
      onView(withId(R.id.profile_edit_toolbar)).check(matches(hasDescendant(withText("Ben"))))
      onView(withId(R.id.profile_edit_name)).check(matches(withText("Ben")))
      onView(withId(R.id.profile_edit_allow_download_sub)).perform(scrollTo())
        .check(matches((isDisplayed())))
      onView(withId(R.id.profile_edit_allow_download_switch)).perform(scrollTo())
        .check(matches((isDisplayed())))
      onView(withId(R.id.profile_delete_button)).perform(scrollTo()).check(matches(isDisplayed()))
      onView(withId(R.id.profile_delete_button))
        .perform(scrollTo())
        .check(
          matches(
            (
              isDisplayed()
              )
          )
        )
    }
  }

  @Test
  fun testProfileEdit_startWithUserProfile_clickRenameButton_checkOpensProfileRename() {
    launch<ProfileEditActivity>(
      ProfileEditActivity.createProfileEditActivity(
        context = context,
        profileId = 1
      )
    ).use {
      onView(withId(R.id.profile_rename_button)).perform(click())
      intended(hasComponent(ProfileRenameActivity::class.java.name))
    }
  }

  @Test
  fun testProfileEdit_configChange_startWithUserProfile_clickRename_checkOpensProfileRename() {
    launch<ProfileEditActivity>(
      ProfileEditActivity.createProfileEditActivity(
        context = context,
        profileId = 1
      )
    ).use {
      onView(isRoot()).perform(orientationLandscape())
      onView(withId(R.id.profile_rename_button)).perform(click())
      intended(hasComponent(ProfileRenameActivity::class.java.name))
    }
  }

  @Test
  fun testProfileEdit_startWithUserProfile_clickResetPin_checkOpensProfileResetPin() {
    launch<ProfileEditActivity>(
      ProfileEditActivity.createProfileEditActivity(
        context = context,
        profileId = 1
      )
    ).use {
      onView(withId(R.id.profile_reset_button)).perform(click())
      intended(hasComponent(ProfileResetPinActivity::class.java.name))
    }
  }

  @Test
  fun testProfileEdit_configChange_startWithUserProfile_clickResetPin_checkOpensProfileResetPin() {
    launch<ProfileEditActivity>(
      ProfileEditActivity.createProfileEditActivity(
        context = context,
        profileId = 1
      )
    ).use {
      onView(isRoot()).perform(orientationLandscape())
      onView(withId(R.id.profile_reset_button)).perform(scrollTo()).perform(click())
      intended(hasComponent(ProfileResetPinActivity::class.java.name))
    }
  }

  @Test
  fun testProfileEdit_startWithUserProfile_clickProfileDeletionButton_checkOpensDeletionDialog() {
    launch<ProfileEditActivity>(
      ProfileEditActivity.createProfileEditActivity(
        context = context,
        profileId = 1
      )
    ).use {
      onView(withId(R.id.profile_delete_button)).perform(click())
      onView(withText(R.string.profile_edit_delete_dialog_message))
        .inRoot(isDialog())
        .check(
          matches(
            isDisplayed()
          )
        )
    }
  }

  @Test
  fun testProfileEdit_configChange_startWithUserProfile_clickDelete_checkOpensDeletionDialog() {
    launch<ProfileEditActivity>(
      ProfileEditActivity.createProfileEditActivity(
        context = context,
        profileId = 1
      )
    ).use {
      onView(isRoot()).perform(orientationLandscape())
      onView(withId(R.id.profile_delete_button)).perform(scrollTo()).perform(click())
      testCoroutineDispatchers.runCurrent()
      onView(withText(R.string.profile_edit_delete_dialog_message))
        .inRoot(isDialog())
        .check(
          matches(
            isDisplayed()
          )
        )
    }
  }

  @Test
  fun testProfileEdit_startWithUserProfile_clickDelete_configChange_checkDeletionDialogIsVisible() {
    launch<ProfileEditActivity>(
      ProfileEditActivity.createProfileEditActivity(
        context,
        profileId = 1
      )
    ).use {
      onView(withId(R.id.profile_delete_button)).perform(scrollTo()).perform(click())
      onView(isRoot()).perform(orientationLandscape())
      testCoroutineDispatchers.runCurrent()
      onView(withText(R.string.profile_edit_delete_dialog_message))
        .inRoot(isDialog())
        .check(
          matches(
            isCompletelyDisplayed()
          )
        )
    }
  }

  @Test
  fun testProfileEdit_deleteProfile_checkReturnsToProfileListOnPhoneOrAdminControlOnTablet() {
    launch<ProfileEditActivity>(
      ProfileEditActivity.createProfileEditActivity(
        context = context,
        profileId = 1
      )
    ).use {
      onView(withId(R.id.profile_delete_button)).perform(click())
      onView(withText(R.string.profile_edit_delete_dialog_positive))
        .inRoot(isDialog())
        .perform(click())
      testCoroutineDispatchers.runCurrent()
      if (context.resources.getBoolean(R.bool.isTablet)) {
        intended(hasComponent(AdministratorControlsActivity::class.java.name))
      } else {
        intended(hasComponent(ProfileListActivity::class.java.name))
      }
    }
  }

  @Test
  fun testProfileEdit_landscape_deleteProfile_checkReturnsProfileListOnTabletAdminControlOnPhone() {
    launch<ProfileEditActivity>(
      ProfileEditActivity.createProfileEditActivity(
        context = context,
        profileId = 1
      )
    ).use {
      onView(isRoot()).perform(orientationLandscape())
      onView(withId(R.id.profile_delete_button)).perform(scrollTo()).perform(click())
      onView(withText(R.string.profile_edit_delete_dialog_positive))
        .inRoot(isDialog())
        .perform(click())
      testCoroutineDispatchers.runCurrent()
      if (context.resources.getBoolean(R.bool.isTablet)) {
        intended(hasComponent(AdministratorControlsActivity::class.java.name))
      } else {
        intended(hasComponent(ProfileListActivity::class.java.name))
      }
    }
  }

  @Test
  fun testProfileEdit_startWithUserHasDownloadAccess_checkSwitchIsChecked() {
    profileManagementController.addProfile(
      name = "James",
      pin = "123",
      avatarImagePath = null,
      allowDownloadAccess = true,
      colorRgb = -10710042,
      isAdmin = false
    ).toLiveData()
    launch<ProfileEditActivity>(
      ProfileEditActivity.createProfileEditActivity(
        context = context,
        profileId = 4
      )
    ).use {
      testCoroutineDispatchers.runCurrent()
      onView(withId(R.id.profile_edit_allow_download_switch)).check(matches(isChecked()))
    }
  }

  @Test
  fun testProfileEdit_configChange_startWithUserHasDownloadAccess_checkSwitchIsChecked() {
    profileManagementController.addProfile(
      name = "James",
      pin = "123",
      avatarImagePath = null,
      allowDownloadAccess = true,
      colorRgb = -10710042,
      isAdmin = false
    ).toLiveData()
    launch<ProfileEditActivity>(
      ProfileEditActivity.createProfileEditActivity(
        context = context,
        profileId = 4
      )
    ).use {
      testCoroutineDispatchers.runCurrent()
      onView(isRoot()).perform(orientationLandscape())
      onView(withId(R.id.profile_edit_allow_download_switch)).check(matches(isChecked()))
    }
  }

  @Test
  fun testProfileEdit_startWithUserHasDownloadAccess_clickAllowDownloadContainer_checkChanged() {
    profileManagementController.addProfile(
      name = "James",
      pin = "123",
      avatarImagePath = null,
      allowDownloadAccess = true,
      colorRgb = -10710042,
      isAdmin = false
    ).toLiveData()
    launch<ProfileEditActivity>(
      ProfileEditActivity.createProfileEditActivity(
        context = context,
        profileId = 4
      )
    ).use {
      testCoroutineDispatchers.runCurrent()
      onView(withId(R.id.profile_edit_allow_download_switch)).check(matches(isChecked()))
      onView(withId(R.id.profile_edit_allow_download_container)).perform(click())
      onView(withId(R.id.profile_edit_allow_download_switch)).check(matches(not(isChecked())))
    }
  }

  @Test
  fun testProfileEdit_startWithUserHasDownloadAccess_switchIsNotClickable() {
    profileManagementController.addProfile(
      name = "James",
      pin = "123",
      avatarImagePath = null,
      allowDownloadAccess = true,
      colorRgb = -10710042,
      isAdmin = false
    ).toLiveData()
    launch<ProfileEditActivity>(
      ProfileEditActivity.createProfileEditActivity(
        context = context,
        profileId = 4
      )
    ).use {
      testCoroutineDispatchers.runCurrent()
      onView(withId(R.id.profile_edit_allow_download_switch)).check(matches(not(isClickable())))
    }
  }

  @Test
  fun testProfileEdit_startWithUserHasDownloadAccess_switchContainerIsFocusable() {
    profileManagementController.addProfile(
      name = "James",
      pin = "123",
      avatarImagePath = null,
      allowDownloadAccess = true,
      colorRgb = -10710042,
      isAdmin = false
    ).toLiveData()
    launch<ProfileEditActivity>(
      ProfileEditActivity.createProfileEditActivity(
        context = context,
        profileId = 4
      )
    ).use {
      testCoroutineDispatchers.runCurrent()
      onView(withId(R.id.profile_edit_allow_download_container)).check(matches(isFocusable()))
    }
  }

  @Test
  fun testProfileEdit_startWithUserHasDownloadAccess_switchContainerIsDisplayed() {
    profileManagementController.addProfile(
      name = "James",
      pin = "123",
      avatarImagePath = null,
      allowDownloadAccess = true,
      colorRgb = -10710042,
      isAdmin = false
    ).toLiveData()
    launch<ProfileEditActivity>(
      ProfileEditActivity.createProfileEditActivity(
        context = context,
        profileId = 4
      )
    ).use {
      testCoroutineDispatchers.runCurrent()
      onView(withId(R.id.profile_edit_allow_download_container)).check(matches(isDisplayed()))
    }
  }

  @Test
  fun testProfileEdit_startWithUserDoesNotHaveDownloadAccess_switchContainerIsNotDisplayed() {
    launch<ProfileEditActivity>(
      ProfileEditActivity.createProfileEditActivity(
        context = context,
        profileId = 0
      )
    ).use {
      testCoroutineDispatchers.runCurrent()
      onView(withId(R.id.profile_edit_allow_download_container)).check(matches(not(isDisplayed())))
    }
  }

  // TODO(#59): Figure out a way to reuse modules instead of needing to re-declare them.
  @Singleton
  @Component(
    modules = [
      RobolectricModule::class,
      PlatformParameterModule::class,
      TestDispatcherModule::class, ApplicationModule::class,
      LoggerModule::class, ContinueModule::class, FractionInputModule::class,
      ItemSelectionInputModule::class, MultipleChoiceInputModule::class,
      NumberWithUnitsRuleModule::class, NumericInputRuleModule::class, TextInputRuleModule::class,
      DragDropSortInputModule::class, ImageClickInputModule::class, InteractionsModule::class,
      GcsResourceModule::class, GlideImageLoaderModule::class, ImageParsingModule::class,
      HtmlParserEntityTypeModule::class, QuestionModule::class, TestLogReportingModule::class,
      AccessibilityTestModule::class, LogStorageModule::class, CachingTestModule::class,
      PrimeTopicAssetsControllerModule::class, ExpirationMetaDataRetrieverModule::class,
      ViewBindingShimModule::class, RatioInputModule::class, WorkManagerConfigurationModule::class,
      ApplicationStartupListenerModule::class, LogUploadWorkerModule::class,
      HintsAndSolutionConfigModule::class, HintsAndSolutionProdModule::class,
      FirebaseLogUploaderModule::class, FakeOppiaClockModule::class, PracticeTabModule::class,
      DeveloperOptionsStarterModule::class, DeveloperOptionsModule::class,
      ExplorationStorageModule::class, NetworkModule::class, NetworkConfigProdModule::class,
      NetworkConnectionUtilDebugModule::class, NetworkConnectionDebugUtilModule::class
    ]
  )
  interface TestApplicationComponent : ApplicationComponent {
    @Component.Builder
    interface Builder : ApplicationComponent.Builder

    fun inject(profileEditActivityTest: ProfileEditActivityTest)
  }

  class TestApplication : Application(), ActivityComponentFactory, ApplicationInjectorProvider {
    private val component: TestApplicationComponent by lazy {
      DaggerProfileEditActivityTest_TestApplicationComponent.builder()
        .setApplication(this)
        .build() as TestApplicationComponent
    }

    fun inject(profileEditActivityTest: ProfileEditActivityTest) {
      component.inject(profileEditActivityTest)
    }

    override fun createActivityComponent(activity: AppCompatActivity): ActivityComponent {
      return component.getActivityComponentBuilderProvider().get().setActivity(activity).build()
    }

    override fun getApplicationInjector(): ApplicationInjector = component
  }
}
