package xyz.darkphoenix.pointer;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.ViewConfiguration;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.callbacks.XCallback;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public class KeyIntercept implements IXposedHookLoadPackage {

    private Context context;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {

        if (!loadPackageParam.packageName.equals("android"))
            return;

        findAndHookMethod(Constants.PHONE_WINDOW_MANAGER,
                loadPackageParam.classLoader,
                "init",
                Context.class, "android.view.IWindowManager", "android.view.WindowManagerPolicy.WindowManagerFuncs",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        context = (Context) param.args[0];
                    }
                });

        findAndHookMethod(Constants.PHONE_WINDOW_MANAGER,
                loadPackageParam.classLoader,
                "interceptKeyBeforeQueueing",
                KeyEvent.class, int.class,
                new KeyInterceptMethodHook(XCallback.PRIORITY_HIGHEST));

    }

    private class KeyInterceptMethodHook extends XC_MethodHook {

        private boolean volumeUpPressed = false;
        private boolean volumeDownPressed = false;
        private boolean powerPressed = false;
        private long time;

        public KeyInterceptMethodHook(int priority) {
            super(priority);
        }

        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

            if (isServiceRunning()) {

                KeyEvent event = (KeyEvent) param.args[0];
                int keyCode = event.getKeyCode();
                int keyAction = event.getAction();

                if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {

                    if (keyAction == KeyEvent.ACTION_UP) {
                        volumeUpPressed = false;
                    } else if (keyAction == KeyEvent.ACTION_DOWN)
                        volumeUpPressed = true;

                    Intent intent = new Intent();
                    intent.setAction(Constants.VOLUME_KEY_EVENT);
                    intent.putExtra(Constants.VOLUME_UP_PRESSED, volumeUpPressed);
                    context.sendBroadcast(intent);

                    param.setResult(0);

                } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {

                    if (keyAction == KeyEvent.ACTION_UP) {
                        volumeDownPressed = false;
                    } else if (keyAction == KeyEvent.ACTION_DOWN)
                        volumeDownPressed = true;

                    Intent intent = new Intent();
                    intent.setAction(Constants.VOLUME_KEY_EVENT);
                    intent.putExtra(Constants.VOLUME_DOWN_PRESSED, volumeDownPressed);
                    context.sendBroadcast(intent);

                    param.setResult(0);

                } else if (keyCode == KeyEvent.KEYCODE_POWER) {

                    if (keyAction == KeyEvent.ACTION_DOWN) {
                        powerPressed = true;
                        time = System.currentTimeMillis();
                    } else if (powerPressed && keyAction == KeyEvent.ACTION_UP) {

                        Intent intent = new Intent();

                        if ((System.currentTimeMillis() - time) >= ViewConfiguration.getLongPressTimeout())
                            intent.setAction(Constants.LONG_CLICK_AND_FINISH);
                        else
                            intent.setAction(Constants.CLICK_AND_FINISH);

                        context.sendBroadcast(intent);

                        time = 0;
                        powerPressed = false;

                    }

                    param.setResult(0);
                }

            }
        }

        private boolean isServiceRunning() {

            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
                if (HeadService.class.getName().equals(service.service.getClassName()))
                    return true;

            return false;
        }

    }
}
