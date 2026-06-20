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

package pascal.taie.analysis.pta.plugin.android.icc.intentattribute;

import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.plugin.android.AndroidModelEdge;
import pascal.taie.analysis.pta.plugin.android.icc.ICCContext;
import pascal.taie.analysis.pta.plugin.android.icc.ICCHandler;
import pascal.taie.analysis.pta.plugin.android.icc.SendAndReplyICCHandler;
import pascal.taie.analysis.pta.plugin.util.InvokeHandler;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;

import java.util.List;

import static pascal.taie.analysis.pta.plugin.android.icc.intentattribute.IntentAttributeKind.ACTION;
import static pascal.taie.analysis.pta.plugin.android.icc.intentattribute.IntentAttributeKind.CATEGORY;
import static pascal.taie.analysis.pta.plugin.android.icc.intentattribute.IntentAttributeKind.DATA_HOST;
import static pascal.taie.analysis.pta.plugin.android.icc.intentattribute.IntentAttributeKind.DATA_PATH;
import static pascal.taie.analysis.pta.plugin.android.icc.intentattribute.IntentAttributeKind.DATA_PORT;
import static pascal.taie.analysis.pta.plugin.android.icc.intentattribute.IntentAttributeKind.DATA_SCHEME;
import static pascal.taie.analysis.pta.plugin.android.icc.intentattribute.IntentAttributeKind.MIME_TYPE;
import static pascal.taie.analysis.pta.plugin.util.InvokeUtils.BASE;

/**
 * Tracks dynamically-created IntentFilter attributes.
 *
 * <p>These facts are used by {@link SendAndReplyICCHandler} to match
 * dynamically-registered receivers against outgoing implicit Intents.
 */
public class IntentFilterAttributeModel extends ICCHandler {

    private static final String INIT_WITH_INTENT_FILTER = "<android.content.IntentFilter: void <init>(android.content.IntentFilter)>";

    private static final String INIT_WITH_ACTION = "<android.content.IntentFilter: void <init>(java.lang.String)>";

    private static final String ADD_ACTION = "<android.content.IntentFilter: void addAction(java.lang.String)>";

    private static final String ADD_CATEGORY = "<android.content.IntentFilter: void addCategory(java.lang.String)>";

    private static final String ADD_DATA_SCHEME = "<android.content.IntentFilter: void addDataScheme(java.lang.String)>";

    private static final String ADD_DATA_PATH = "<android.content.IntentFilter: void addDataPath(java.lang.String)>";

    private static final String ADD_DATA_TYPE = "<android.content.IntentFilter: void addDataType(java.lang.String)>";

    private static final String ADD_DATA_AUTHORITY = "<android.content.IntentFilter: void addDataAuthority(java.lang.String,java.lang.String)>";

    private static final String ADD_DATA_SCHEME_SPECIFIC_PART = "<android.content.IntentFilter: void addDataSchemeSpecificPart(java.lang.String,int)>";

    public IntentFilterAttributeModel(ICCContext context) {
        super(context);
    }

    @InvokeHandler(signature = {
            INIT_WITH_INTENT_FILTER,
            INIT_WITH_ACTION,
            ADD_ACTION,
            ADD_CATEGORY,
            ADD_DATA_SCHEME,
            ADD_DATA_PATH,
            ADD_DATA_TYPE,
            ADD_DATA_AUTHORITY},
            argIndexes = {BASE})
    public void intentFilterCommonInvoke(Context context, Invoke invoke, PointsToSet pts) {
        JMethod method = invoke.getMethodRef().resolve();
        InvokeExp invokeExp = invoke.getInvokeExp();
        IntentAttributeCollector collector =
                new IntentAttributeCollector(context, csManager, invokeExp);
        CSVar base = csManager.getCSVar(context, InvokeUtils.getVar(invoke, BASE));
        if (INIT_WITH_INTENT_FILTER.equals(method.getSignature())) {
            solver.addPFGEdge(new AndroidModelEdge(collector.arg(0), base));
        }
        List<IntentAttribute> intentAttributes =
                collectFilterAttributes(method.getSignature(), collector);
        if (!intentAttributes.isEmpty()) {
            pts.forEach(csObj -> handlerContext.intentFilter2Attribute().putAll(csObj, intentAttributes));
        }
    }

    private List<IntentAttribute> collectFilterAttributes(String signature,
                                                          IntentAttributeCollector collector) {
        switch (signature) {
            case INIT_WITH_ACTION, ADD_ACTION -> collector.addSingle(0, ACTION);
            case ADD_CATEGORY -> collector.addSingle(0, CATEGORY);
            case ADD_DATA_SCHEME -> collector.addSingle(0, DATA_SCHEME);
            case ADD_DATA_PATH -> collector.addSingle(0, DATA_PATH);
            case ADD_DATA_TYPE -> collector.addSingle(0, MIME_TYPE);
            case ADD_DATA_AUTHORITY -> {
                collector.addSingle(0, DATA_HOST);
                collector.addSingle(1, DATA_PORT);
            }
        }
        return collector.attributes();
    }

}
