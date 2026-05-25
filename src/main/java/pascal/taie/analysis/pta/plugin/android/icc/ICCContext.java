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

package pascal.taie.analysis.pta.plugin.android.icc;

import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.plugin.android.AndroidContext;
import pascal.taie.language.classes.JClass;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;

/**
 * Contains information used by ICC analysis handlers.
 */
public class ICCContext extends AndroidContext {

    /** Component-level ICC graph derived from matched source/target components. */
    private final MultiMap<JClass, JClass> componentICCGraph = Maps.newMultiMap();

    /** Intent object -> attributes written by Intent constructors/setters. */
    private final MultiMap<CSObj, IntentAttribute> intent2IntentAttribute = Maps.newMultiMap();

    /** ComponentName object -> variable carrying its class-name component. */
    private final MultiMap<CSObj, CSVar> componentName2Info = Maps.newMultiMap();

    /** IntentFilter object -> dynamically-added filter attributes. */
    private final MultiMap<CSObj, IntentAttribute> intentFilter2Attribute = Maps.newMultiMap();

    /** Messenger object -> Handler variable passed to Messenger(Handler). */
    private final MultiMap<CSObj, CSVar> messenger2Handler = Maps.newMultiMap();

    /** Messenger object -> IBinder variable passed to Messenger(IBinder). */
    private final MultiMap<CSObj, CSVar> messenger2IBinder = Maps.newMultiMap();

    /** Dispatch object -> Message send facts. */
    private final MultiMap<CSObj, ICCInfo> sendMessage = Maps.newMultiMap();

    /** Dispatch object -> Handler.handleMessage target facts. */
    private final MultiMap<CSObj, ICCInfo> handleMessage = Maps.newMultiMap();

    /** Source component variable -> outgoing ICC facts. */
    private final MultiMap<CSVar, ICCInfo> sourceComponent2ICCInfo = Maps.newMultiMap();

    /** Target component variable -> incoming ICC facts. */
    private final MultiMap<CSVar, ICCInfo> targetComponent2ICCInfo = Maps.newMultiMap();

    /** Service component variable -> Messenger variables returned from onBind(...). */
    private final MultiMap<CSVar, CSVar> serviceComponent2Messenger = Maps.newMultiMap();

    /** Service component variable -> IBinder objects returned from onBind(...). */
    private final MultiMap<CSVar, CSObj> serviceComponent2IBinder = Maps.newMultiMap();

    /** bindService Intent variable -> onServiceConnected IBinder parameter variable. */
    private final MultiMap<CSVar, CSVar> intent2IBinder = Maps.newMultiMap();

    /** bindService Intent variable -> ServiceConnection argument variable. */
    private final MultiMap<CSVar, CSVar> intents2ServiceConnection = Maps.newMultiMap();

    /** IBinder variable -> generated AIDL proxy variable returned from Stub.asInterface(...). */
    private final MultiMap<CSVar, CSVar> iBinder2AidlInvoke = Maps.newMultiMap();

    public MultiMap<JClass, JClass> componentICCGraph() {
        return componentICCGraph;
    }

    public MultiMap<CSObj, IntentAttribute> intent2IntentAttribute() {
        return intent2IntentAttribute;
    }

    public MultiMap<CSObj, CSVar> componentName2Info() {
        return componentName2Info;
    }

    public MultiMap<CSObj, IntentAttribute> intentFilter2Attribute() {
        return intentFilter2Attribute;
    }

    public MultiMap<CSObj, CSVar> messenger2Handler() {
        return messenger2Handler;
    }

    public MultiMap<CSObj, CSVar> messenger2IBinder() {
        return messenger2IBinder;
    }

    public MultiMap<CSObj, ICCInfo> sendMessage() {
        return sendMessage;
    }

    public MultiMap<CSObj, ICCInfo> handleMessage() {
        return handleMessage;
    }

    public MultiMap<CSVar, ICCInfo> sourceComponent2ICCInfo() {
        return sourceComponent2ICCInfo;
    }

    public MultiMap<CSVar, ICCInfo> targetComponent2ICCInfo() {
        return targetComponent2ICCInfo;
    }

    public MultiMap<CSVar, CSVar> serviceComponent2Messenger() {
        return serviceComponent2Messenger;
    }

    public MultiMap<CSVar, CSObj> serviceComponent2IBinder() {
        return serviceComponent2IBinder;
    }

    public MultiMap<CSVar, CSVar> intent2IBinder() {
        return intent2IBinder;
    }

    public MultiMap<CSVar, CSVar> intents2ServiceConnection() {
        return intents2ServiceConnection;
    }

    public MultiMap<CSVar, CSVar> iBinder2Aidl() {
        return iBinder2AidlInvoke;
    }

    public ICCContext(AndroidContext context) {
        super(context);
    }

}
