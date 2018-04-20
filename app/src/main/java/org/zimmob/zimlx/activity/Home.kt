package org.zimmob.zimlx.activity

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.support.v4.widget.DrawerLayout
import android.util.Log
import android.view.View
import android.widget.AdapterView
import cat.ereza.customactivityoncrash.CustomActivityOnCrash
import com.afollestad.materialdialogs.MaterialDialog
import kotlinx.android.synthetic.main.activity_home.*
import net.gsantner.opoc.util.ContextUtils
import org.zimmob.zimlx.App
import org.zimmob.zimlx.BuildConfig
import org.zimmob.zimlx.R
import org.zimmob.zimlx.core.activity.CoreHome
import org.zimmob.zimlx.core.interfaces.AbstractApp
import org.zimmob.zimlx.core.interfaces.DialogListener
import org.zimmob.zimlx.core.interfaces.SettingsManager
import org.zimmob.zimlx.core.manager.Setup
import org.zimmob.zimlx.core.model.Item
import org.zimmob.zimlx.core.util.*
import org.zimmob.zimlx.core.viewutil.DesktopGestureListener
import org.zimmob.zimlx.core.viewutil.ItemGestureListener
import org.zimmob.zimlx.util.AppManager
import org.zimmob.zimlx.util.AppSettings
import org.zimmob.zimlx.util.LauncherAction
import org.zimmob.zimlx.viewutil.DialogHelper
import org.zimmob.zimlx.viewutil.IconListAdapter

class Home : CoreHome(), DrawerLayout.DrawerListener {

    companion object {
        var launcher: Home? = null
        var _resources: Resources? = null
    }

    fun getDrawerLayout(): DrawerLayout = drawer_layout

    override fun onCreate(savedInstanceState: Bundle?) {
        launcher = this
        _resources = this.resources

        ContextUtils(applicationContext).setAppLanguage(AppSettings.get().language) // before setContentView
        super.onCreate(savedInstanceState)



        if (BuildConfig.IS_GPLAY_BUILD) {
            CustomActivityOnCrash.setShowErrorDetails(true)
            CustomActivityOnCrash.setEnableAppRestart(false)
            CustomActivityOnCrash.setDefaultErrorActivityDrawable(R.drawable.rip)
            CustomActivityOnCrash.install(this)
        }
    }

    override fun onStartApp(context: Context, intent: Intent, view: View?) =
            if (intent.component!!.packageName == "org.zimmob.zimlx") {
                LauncherAction.RunAction(LauncherAction.Action.LauncherSettings, context)
                consumeNextResume = true
            } else
                super.onStartApp(context, intent, view)

    override fun onStartApp(context: Context, app: AbstractApp, view: View?) =
            if (app.packageName == "org.zimmob.zimlx") {
                LauncherAction.RunAction(LauncherAction.Action.LauncherSettings, context)
                consumeNextResume = true
            } else
                super.onStartApp(context, app, view)

    override fun initAppManager() {
        super.initAppManager()
        AppManager.getInstance(this).init()

        //Create Default DockItems
        //addDockDialer(Intent.ACTION_DIAL, 0)
        //addDockApps(Intent.CATEGORY_APP_MESSAGING, 1)
        //addDockCamera(3);
        //addDockApps(Intent.CATEGORY_APP_BROWSER, 4)

    }

    private fun addDockDialer(appIntent: String, position: Int) {
        val intent = Intent(Intent.ACTION_DIAL, null)
        intent.addCategory(Intent.CATEGORY_DEFAULT)

        val activitiesInfo = packageManager.queryIntentActivities(intent, 0)

        for (info in activitiesInfo) {
            val app = AppManager.App(this, info, packageManager)
            if (app.label.equals("Phone")) {
                Log.i("HOME", app.className)
                val itemP = Item.newAppItem(app)
                itemP.x = position
                itemP.type = Item.Type.APP
                //val myPers= Array<String>(size = 2){android.Manifest.permission.CALL_PHONE}

                //requestPermissions(myPers, REQUEST_PERMISSION_CALL_PHONE)
                db.saveItem(itemP, 0, Definitions.ItemPosition.Dock)
            }


        }

    }


    private fun addDockCamera(position: Int) {
        val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        val activitiesInfo = packageManager.queryIntentActivities(intent, 0)
        for (info in activitiesInfo) {
            val app = AppManager.App(this, info, packageManager)
            Log.i("HOME", app.packageName)
            val item = Item.newAppItem(app)
            item.x = position
            db.saveItem(item, 0, Definitions.ItemPosition.Dock)
        }

    }

    private fun addDockApps(appCategory: String, position: Int) {
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(appCategory)
        val activitiesInfo = packageManager.queryIntentActivities(intent, 0)
        for (info in activitiesInfo) {
            val app = AppManager.App(this, info, packageManager)
            Log.i("HOME", app.packageName)
            val item = Item.newAppItem(app)
            item.x = position
            db.saveItem(item, 0, Definitions.ItemPosition.Dock)
        }

    }

    override fun initViews() {
        super.initViews()

        initMinibar()
    }

    override fun initSettings() {
        super.initSettings()
        drawer_layout.setDrawerLockMode(if (AppSettings.get().minibarEnable) DrawerLayout.LOCK_MODE_UNLOCKED else DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    override fun onRemovePage() = if (!getDesktop().isCurrentPageEmpty)
        DialogHelper.alertDialog(this, getString(R.string.dialog_title__remove),
                "This page is not empty. Those item will also be removed.",
                MaterialDialog.SingleButtonCallback { _, _ ->
                    super@Home.onRemovePage()
                }
        )
    else
        super@Home.onRemovePage()

    fun initMinibar() {
        val labels = ArrayList<String>()
        val icons = ArrayList<Int>()

        for (act in AppSettings.get().minibarArrangement) {
            if (act.length > 1 && act[0] == '0') {
                val item = LauncherAction.getActionItemFromString(act.substring(1))
                if (item != null) {
                    labels.add(item.label.toString())
                    icons.add(item.icon)
                }
            }
        }

        minibar.adapter = IconListAdapter(this, labels, icons)
        minibar.onItemClickListener = AdapterView.OnItemClickListener { _, _, i, _ ->
            val action = LauncherAction.Action.valueOf(labels[i])
            if (action == LauncherAction.Action.DeviceSettings || action == LauncherAction.Action.LauncherSettings || action == LauncherAction.Action.EditMinBar) {
                consumeNextResume = true
            }
            LauncherAction.RunAction(action, this@Home)
            if (action != LauncherAction.Action.DeviceSettings && action != LauncherAction.Action.LauncherSettings && action != LauncherAction.Action.EditMinBar) {
                drawer_layout.closeDrawers()
            }
        }
        // frame layout spans the entire side while the minibar container has gaps at the top and bottom
        minibar_background.setBackgroundColor(AppSettings.get().minibarBackgroundColor)
    }

    override fun onBackPressed() {
        drawer_layout.closeDrawers()
        super.onBackPressed()
    }

    // search button in the search bar is clicked
    fun onSearch(view: View) {
        var i: Intent
        try {
            i = Intent(Intent.ACTION_MAIN)
            i.setClassName("com.google.android.googlequicksearchbox", "com.google.android.googlequicksearchbox.SearchActivity")
            this@Home.startActivity(i)
        } catch (e: Exception) {
            i = Intent(Intent.ACTION_WEB_SEARCH)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        this@Home.startActivity(i)
    }

    // voice button in the search bar clicked
    fun onVoiceSearch(view: View) {
        try {
            val i = Intent(Intent.ACTION_MAIN)
            i.setClassName("com.google.android.googlequicksearchbox", "com.google.android.googlequicksearchbox.VoiceSearchActivity")
            this@Home.startActivity(i)
        } catch (e: Exception) {
            Tool.toast(this@Home, "Can not find google search app")
        }

    }

    override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}

    override fun onDrawerOpened(drawerView: View) {

    }

    override fun onDrawerClosed(drawerView: View) {}

    override fun onDrawerStateChanged(newState: Int) {}

    override fun initStaticHelper() {
        val settingsManager = AppSettings.get()
        val imageLoader = object : Setup.ImageLoader<AppManager.App> {
            override fun createIconProvider(drawable: Drawable?): BaseIconProvider = SimpleIconProvider(drawable)

            override fun createIconProvider(icon: Int): BaseIconProvider = SimpleIconProvider(icon)
        }
        val desktopGestureCallback = DesktopGestureListener.DesktopGestureCallback { desktop, event ->
            when (event) {
                DesktopGestureListener.Type.SwipeUp -> {
                    if (Integer.parseInt(AppSettings.get().gestureSwipeUp) != 0) {
                        val gesture = LauncherAction.getActionItem(Integer.parseInt(AppSettings.get().gestureSwipeUp) - 1)
                        if (gesture != null && AppSettings.get().isGestureFeedback) {
                            Tool.vibrate(desktop)
                        }
                        LauncherAction.RunAction(gesture, desktop.context)
                    }
                    true
                }
                DesktopGestureListener.Type.SwipeDown -> {
                    if (Integer.parseInt(AppSettings.get().gestureSwipeDown) != 0) {
                        val gesture = LauncherAction.getActionItem(Integer.parseInt(AppSettings.get().gestureSwipeDown) - 1)
                        if (gesture != null && AppSettings.get().isGestureFeedback) {
                            Tool.vibrate(desktop)
                        }
                        LauncherAction.RunAction(gesture, desktop.context)
                    }
                    true
                }
                DesktopGestureListener.Type.SwipeLeft -> false
                DesktopGestureListener.Type.SwipeRight -> false
                DesktopGestureListener.Type.Pinch -> {
                    if (Integer.parseInt(AppSettings.get().gesturePinch) != 0) {
                        val gesture = LauncherAction.getActionItem(Integer.parseInt(AppSettings.get().gesturePinch) - 1)
                        if (gesture != null && AppSettings.get().isGestureFeedback) {
                            Tool.vibrate(desktop)
                        }
                        LauncherAction.RunAction(gesture, desktop.context)
                    }
                    true
                }
                DesktopGestureListener.Type.Unpinch -> {
                    if (Integer.parseInt(AppSettings.get().gestureUnpinch) != 0) {
                        val gesture = LauncherAction.getActionItem(Integer.parseInt(AppSettings.get().gestureUnpinch) - 1)
                        if (gesture != null && AppSettings.get().isGestureFeedback) {
                            Tool.vibrate(desktop)
                        }
                        LauncherAction.RunAction(gesture, desktop.context)
                    }
                    true
                }
                DesktopGestureListener.Type.DoubleTap -> {
                    if (Integer.parseInt(AppSettings.get().gestureDoubleTap) != 0) {
                        val gesture = LauncherAction.getActionItem(Integer.parseInt(AppSettings.get().gestureDoubleTap) - 1)
                        if (gesture != null && AppSettings.get().isGestureFeedback) {
                            Tool.vibrate(desktop)
                        }
                        LauncherAction.RunAction(gesture, desktop.context)
                    }
                    true
                }
                else -> {
                    throw RuntimeException("Type not handled!")
                }
            }
        }
        val itemGestureCallback: ItemGestureListener.ItemGestureCallback = ItemGestureListener.ItemGestureCallback { _, _ -> false }
        val dataManager = DatabaseHelper(this)
        val appLoader = AppManager.getInstance(this)
        val eventHandler = object : Setup.EventHandler {
            override fun showLauncherSettings(context: Context) {
                LauncherAction.RunAction(LauncherAction.Action.LauncherSettings, context)
            }

            override fun showPickAction(context: Context, listener: DialogListener.OnAddAppDrawerItemListener) {
                DialogHelper.addActionItemDialog(context, MaterialDialog.ListCallback { _, _, position, _ ->
                    when (position) {
                        0 -> listener.onAdd()
                    }
                })
            }

            override fun showEditDialog(context: Context, item: Item, listener: DialogListener.OnEditDialogListener) {
                DialogHelper.editItemDialog("Edit Item", item.label, context, object : DialogHelper.OnItemEditListener {
                    override fun itemLabel(label: String) {
                        listener.onRename(label)
                    }
                })
            }

            override fun showDeletePackageDialog(context: Context, item: Item) {
                DialogHelper.deletePackageDialog(context, item)
            }
        }
        val logger = object : Setup.Logger {
            override fun log(source: Any, priority: Int, tag: String?, msg: String, vararg args: Any) {
                Log.println(priority, tag, String.format(msg, *args))
            }
        }
        Setup.init(object : Setup<AppManager.App>() {
            override fun getAppContext(): Context = App.get()!!.applicationContext

            override fun getAppSettings(): SettingsManager = settingsManager

            override fun getDesktopGestureCallback(): DesktopGestureListener.DesktopGestureCallback = desktopGestureCallback

            override fun getItemGestureCallback(): ItemGestureListener.ItemGestureCallback = itemGestureCallback

            override fun getImageLoader(): ImageLoader<AppManager.App> = imageLoader

            override fun getDataManager(): Setup.DataManager = dataManager

            override fun getAppLoader(): AppManager = appLoader

            override fun getEventHandler(): Setup.EventHandler = eventHandler

            override fun getLogger(): Setup.Logger = logger
        })
    }

    override fun onResume() {
        super.onResume()

        val user = AppSettings.get().getBool(R.string.pref_key__desktop_rotate, false)
        var system = false
        try {
            system = Settings.System.getInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION) == 1
        } catch (e: Settings.SettingNotFoundException) {
            Log.d(Home::class.java.simpleName, "Unable to read settings", e)
        }

        val rotate: Boolean
        if (resources.getBoolean(R.bool.isTablet)) { // tables has no user option to disable rotate
            rotate = system
        } else {
            rotate = user && system
        }
        if (rotate)
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        else
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

}
