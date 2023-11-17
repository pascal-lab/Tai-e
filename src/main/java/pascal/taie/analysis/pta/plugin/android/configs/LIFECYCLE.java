/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.analysis.pta.plugin.android.configs;

/**
 * well-known Android lifecycle methods with subsignature constant
 */
public interface LIFECYCLE {
    String[] TYPES = new String[]{
            "android.app.Application",
            "android.app.Activity",
            "android.app.Service",
            "android.content.BroadcastReceiver",
            "android.content.ContentProvider",
            "android.app.Fragment"
            };

    enum  APPLICATION implements LIFECYCLE {
        APPLICATION_ONCREATE("void onCreate()"),
        APPLICATION_ONTERMINATE("void onTerminate()");

        private final String value;

        APPLICATION(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    enum ACTIVITY implements LIFECYCLE {
        ACTIVITY_ONCREATE("void onCreate(android.os.Bundle)"),
        ACTIVITY_ONSTART("void onStart()"),
        ACTIVITY_ONPOSTRESUME("void onPostResume()"),
        ACTIVITY_ONCREATEDESCRIPTION("java.lang.CharSequence onCreateDescription()"),
        ACTIVITY_ONSAVEINSTANCESTATE("void onSaveInstanceState(android.os.Bundle)"),
        ACTIVITY_ONPAUSE("void onPause()"),
        ACTIVITY_ONSTOP("void onStop()"),
        ACTIVITY_ONRESTART("void onRestart()"),
        ACTIVITY_ONRESUME("void onResume()"),
        ACTIVITY_ONDESTROY("void onDestroy()"),
        ACTIVITY_ONATTACHFRAGMENT("void onAttachFragment(android.app.Fragment)"),
        ACTIVITY_BACKUPPRESSED("void onBackPressed()"),
        ACTIVITY_ONRESTOREINSTANCESTATE("void onRestoreInstanceState(android.os.Bundle)"),
        ACTIVITY_ONPOSTCREATE("void onPostCreate(android.os.Bundle)");
        private final String value;
        ACTIVITY(String value) {
            this.value = value;
        }
        public String getValue() {
            return value;
        }
    }

    enum SERVICE implements LIFECYCLE {
        CLASS_TYPE2("android.app.IntentService"),
        SERVICE_ONCREATE("void onCreate()"),
        SERVICE_ONSTART1("void onStart(android.content.Intent,int)"),
        SERVICE_ONSTART2("int onStartCommand(android.content.Intent,int,int)"),
        SERVICE_ONBIND("android.os.IBinder onBind(android.content.Intent)"),
        SERVICE_ONREBIND("void onRebind(android.content.Intent)"),
        SERVICE_ONUNBIND("boolean onUnbind(android.content.Intent)"),
        SERVICE_ONDESTROY("void onDestroy()");
        private final String value;
        SERVICE(String value) {
            this.value = value;
        }
        public String getValue() {
            return value;
        }
    }

    enum RECEIVE implements LIFECYCLE {
        BROADCAST_ONRECEIVE("void onReceive(android.content.Context,android.content.Intent)");
        private final String value;
        RECEIVE(String value) {
            this.value = value;
        }
        public String getValue() {
            return value;
        }
    }

    enum PROVIDER implements LIFECYCLE {
        CONTENTPROVIDER_ONCREATE("boolean onCreate()");
        private final String value;
        PROVIDER(String value) {
            this.value = value;
        }
        public String getValue() {
            return value;
        }
    }

    enum Fragment implements LIFECYCLE {
        Fragment_ONCREATE("void onCreate(android.os.Bundle)"),
        Fragment_ONSTART("void onStart()"),
        Fragment_ONRESUME("void onResume()"),
        Fragment_ONPAUSE("void onPause()"),
        Fragment_ONSTOP("void onStop()"),
        Fragment_ONATTACH("void onAttach(android.content.Context)"),
        Fragment_ONATTACH2("void onAttach(android.app.Activity)"),
        Fragment_ONCREATEVIEW("void onViewCreated(android.view.View,android.os.Bundle)"),
        Fragment_ONCREATEVIEW2("android.view.View onCreateView(android.view.LayoutInflater,android.view.ViewGroup,android.os.Bundle)"),
        Fragment_ONDESTROY("void onDestroy()"),
        Fragment_ONDESTROYVIEW("void onDestroyView()"),
        Fragment_ONDETACH("void onDetach()"),
        Fragment_ONACTIVITYCREATED("void onActivityCreated(android.os.Bundle)"),
        Fragment_ONACTIVITYRESULT("void onActivityResult(int,int,android.content.Intent)"),
        Fragment_ONATTACHFRAFMENT("void onAttachFragment(androidx.fragment.app.Fragment)");
        private final String value;
        Fragment(String value) {
            this.value = value;
        }
        public String getValue() {
            return value;
        }
    }
}
