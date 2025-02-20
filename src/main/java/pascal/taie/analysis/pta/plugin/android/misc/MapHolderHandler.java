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

package pascal.taie.analysis.pta.plugin.android.misc;

import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.heap.ConstantObj;
import pascal.taie.analysis.pta.plugin.android.AndroidTransferEdge;
import pascal.taie.analysis.pta.plugin.util.InvokeHandler;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;

import java.util.List;
import java.util.Set;

import static pascal.taie.analysis.pta.plugin.util.InvokeUtils.BASE;

public class MapHolderHandler extends AndroidMiscHandler {

    /**
     * Maps from the information stored in any map structure.
     */
    private final MultiMap<CSObj, MapHolder> mapHolder = Maps.newMultiMap();

    /**
     * Maps from the map structure get invoke.
     */
    private final MultiMap<CSObj, CSCallSite> map2GetInvoke = Maps.newMultiMap();

    /**
     * Maps from the map structure unresolved get invoke.
     */
    private final MultiMap<CSObj, CSCallSite> unresolvedMapGetInvoke = Maps.newMultiMap();

    public MapHolderHandler(AndroidMiscContext specificContext) {
        super(specificContext);
    }

    @Override
    public void onPhaseFinish() {
        processUnresolvedMapGetInvoke();
    }

    @InvokeHandler(signature = {
            "<android.content.Intent: android.content.Intent putExtra(java.lang.String,boolean)>",
            "<android.content.Intent: android.content.Intent putExtra(java.lang.String,byte)>",
            "<android.content.Intent: android.content.Intent putExtra(java.lang.String,char)>",
            "<android.content.Intent: android.content.Intent putExtra(java.lang.String,short)>",
            "<android.content.Intent: android.content.Intent putExtra(java.lang.String,int)>",
            "<android.content.Intent: android.content.Intent putExtra(java.lang.String,long)>",
            "<android.content.Intent: android.content.Intent putExtra(java.lang.String,float)>",
            "<android.content.Intent: android.content.Intent putExtra(java.lang.String,double)>",
            "<android.content.Intent: android.content.Intent putExtra(java.lang.String,java.lang.String)>",
            "<android.content.Intent: android.content.Intent putExtra(java.lang.String,java.lang.CharSequence)>",
            "<android.content.Intent: android.content.Intent putExtra(java.lang.String,android.os.Parcelable)>",
            "<android.content.Intent: android.content.Intent putExtra(java.lang.String,android.os.Parcelable[])>",
            "<android.content.Intent: android.content.Intent putParcelableArrayListExtra(java.lang.String,java.util.ArrayList)>",
            "<android.content.Intent: android.content.Intent putIntegerArrayListExtra(java.lang.String,java.util.ArrayList)>",
            "<android.content.Intent: android.content.Intent putStringArrayListExtra(java.lang.String,java.util.ArrayList)>",
            "<android.content.Intent: android.content.Intent putCharSequenceArrayListExtra(java.lang.String,java.util.ArrayList)>",
            "<android.content.Intent: android.content.Intent putExtra(java.lang.String,java.io.Serializable)>",
            "<android.content.Intent: android.content.Intent putExtra(java.lang.String,boolean[])>",
            "<android.content.Intent: android.content.Intent putExtra(java.lang.String,byte[])>",
            "<android.content.Intent: android.content.Intent putExtra(java.lang.String,short[])>",
            "<android.content.Intent: android.content.Intent putExtra(java.lang.String,char[])>",
            "<android.content.Intent: android.content.Intent putExtra(java.lang.String,int[])>",
            "<android.content.Intent: android.content.Intent putExtra(java.lang.String,long[])>",
            "<android.content.Intent: android.content.Intent putExtra(java.lang.String,float[])>",
            "<android.content.Intent: android.content.Intent putExtra(java.lang.String,double[])>",
            "<android.content.Intent: android.content.Intent putExtra(java.lang.String,java.lang.String[])>",
            "<android.content.Intent: android.content.Intent putExtra(java.lang.String,java.lang.CharSequence[])>",
            "<android.content.Intent: android.content.Intent putExtra(java.lang.String,android.os.Bundle)>",
            "<android.os.BaseBundle: void putBoolean(java.lang.String,boolean)>",
            "<android.os.BaseBundle: void putInt(java.lang.String,int)>",
            "<android.os.BaseBundle: void putLong(java.lang.String,long)>",
            "<android.os.BaseBundle: void putDouble(java.lang.String,double)>",
            "<android.os.BaseBundle: void putString(java.lang.String,java.lang.String)>",
            "<android.os.BaseBundle: void putBooleanArray(java.lang.String,boolean[])>",
            "<android.os.BaseBundle: void putIntArray(java.lang.String,int[])>",
            "<android.os.BaseBundle: void putLongArray(java.lang.String,long[])>",
            "<android.os.BaseBundle: void putDoubleArray(java.lang.String,double[])>",
            "<android.os.BaseBundle: void putStringArray(java.lang.String,java.lang.String[])>",
            "<android.os.Bundle: void putBoolean(java.lang.String,boolean)>",
            "<android.os.Bundle: void putByte(java.lang.String,byte)>",
            "<android.os.Bundle: void putChar(java.lang.String,char)>",
            "<android.os.Bundle: void putShort(java.lang.String,short)>",
            "<android.os.Bundle: void putInt(java.lang.String,int)>",
            "<android.os.Bundle: void putLong(java.lang.String,long)>",
            "<android.os.Bundle: void putFloat(java.lang.String,float)>",
            "<android.os.Bundle: void putDouble(java.lang.String,double)>",
            "<android.os.Bundle: void putString(java.lang.String,java.lang.String)>",
            "<android.os.Bundle: void putCharSequence(java.lang.String,java.lang.CharSequence)>",
            "<android.os.Bundle: void putParcelable(java.lang.String,android.os.Parcelable)>",
            "<android.os.Bundle: void putParcelableArray(java.lang.String,android.os.Parcelable[])>",
            "<android.os.Bundle: void putParcelableArrayList(java.lang.String,java.util.ArrayList)>",
            "<android.os.Bundle: void putIntegerArrayList(java.lang.String,java.util.ArrayList)>",
            "<android.os.Bundle: void putStringArrayList(java.lang.String,java.util.ArrayList)>",
            "<android.os.Bundle: void putCharSequenceArrayList(java.lang.String,java.util.ArrayList)>",
            "<android.os.Bundle: void putSerializable(java.lang.String,java.io.Serializable)>",
            "<android.os.Bundle: void putBooleanArray(java.lang.String,boolean[])>",
            "<android.os.Bundle: void putByteArray(java.lang.String,byte[])>",
            "<android.os.Bundle: void putShortArray(java.lang.String,short[])>",
            "<android.os.Bundle: void putCharArray(java.lang.String,char[])>",
            "<android.os.Bundle: void putIntArray(java.lang.String,int[])>",
            "<android.os.Bundle: void putLongArray(java.lang.String,long[])>",
            "<android.os.Bundle: void putFloatArray(java.lang.String,float[])>",
            "<android.os.Bundle: void putDoubleArray(java.lang.String,double[])>",
            "<android.os.Bundle: void putStringArray(java.lang.String,java.lang.String[])>",
            "<android.os.Bundle: void putCharSequenceArray(java.lang.String,java.lang.CharSequence[])>",
            "<android.os.Bundle: void putBundle(java.lang.String,android.os.Bundle)>",
            "<android.os.Bundle: void putBinder(java.lang.String,android.os.IBinder)>",
            "<android.os.Bundle: void putSize(java.lang.String,android.util.Size)>",
            "<android.os.Bundle: void putSizeF(java.lang.String,android.util.SizeF)>",
            "<android.os.Bundle: void putSparseParcelableArray(java.lang.String,android.util.SparseArray)>",
            "<android.content.SharedPreferences$Editor: android.content.SharedPreferences$Editor putString(java.lang.String,java.lang.String)>",
            "<android.content.SharedPreferences$Editor: android.content.SharedPreferences$Editor putString(java.lang.String,boolean)>",
            "<android.content.SharedPreferences$Editor: android.content.SharedPreferences$Editor putString(java.lang.String,float)>",
            "<android.content.SharedPreferences$Editor: android.content.SharedPreferences$Editor putString(java.lang.String,int)>",
            "<android.content.SharedPreferences$Editor: android.content.SharedPreferences$Editor putString(java.lang.String,long)>",
            "<android.content.SharedPreferences$Editor: android.content.SharedPreferences$Editor putString(java.lang.String,java.util.Set)>"},
            argIndexes = {BASE})
    public void mapHolderPut(Context context, Invoke invoke, PointsToSet mapObjs) {
        CSVar key = csManager.getCSVar(context, InvokeUtils.getVar(invoke, 0));
        CSVar value = csManager.getCSVar(context, InvokeUtils.getVar(invoke, 1));
        mapObjs.forEach(mapObj -> {
            // process registerOnSharedPreferenceChangeListener
            handlerContext.sharedPreferences2Callback().get(mapObj).forEach(callback -> {
                Context callbackCtx = callback.getContext();
                Var indexParam = callback.getMethod().getIR().getParam(1);
                Var indexArg = invoke.getInvokeExp().getArg(0);
                solver.addPFGEdge(new AndroidTransferEdge(
                        csManager.getCSVar(context, indexArg),
                        csManager.getCSVar(callbackCtx, indexParam))
                        , indexParam.getType());
            });

            mapHolder.put(mapObj, new MapHolder(key, value));
        });
    }

    @InvokeHandler(signature = {
            "<android.content.Intent: android.os.Bundle getExtras()>"},
            argIndexes = {BASE})
    public void intentGetExtras(Context context, Invoke invoke, PointsToSet mapObjs) {
        Var result = invoke.getResult();
        if (result != null) {
            mapObjs.forEach(mapObj -> solver.addVarPointsTo(context, result, mapObj));
        }
    }


    @InvokeHandler(signature = {
            "<android.content.Intent: java.lang.Object getExtra(java.lang.String, java.lang.Object)>",
            "<android.content.Intent: boolean getBooleanExtra(java.lang.String,boolean)>",
            "<android.content.Intent: byte getByteExtra(java.lang.String,byte)>",
            "<android.content.Intent: short getShortExtra(java.lang.String,short)>",
            "<android.content.Intent: char getCharExtra(java.lang.String,char)>",
            "<android.content.Intent: int getIntExtra(java.lang.String,int)>",
            "<android.content.Intent: long getLongExtra(java.lang.String,long)>",
            "<android.content.Intent: float getFloatExtra(java.lang.String,float)>",
            "<android.content.Intent: double getDoubleExtra(java.lang.String,double)>",
            "<android.content.Intent: java.lang.String getStringExtra(java.lang.String)>",
            "<android.content.Intent: java.lang.CharSequence getCharSequenceExtra(java.lang.String)>",
            "<android.content.Intent: android.os.Parcelable getParcelableExtra(java.lang.String)>",
            "<android.content.Intent: android.os.Parcelable[] getParcelableArrayExtra(java.lang.String)>",
            "<android.content.Intent: java.util.ArrayList getParcelableArrayListExtra(java.lang.String)>",
            "<android.content.Intent: java.io.Serializable getSerializableExtra(java.lang.String)>",
            "<android.content.Intent: java.util.ArrayList getIntegerArrayListExtra(java.lang.String)>",
            "<android.content.Intent: java.util.ArrayList getStringArrayListExtra(java.lang.String)>",
            "<android.content.Intent: java.util.ArrayList getCharSequenceArrayListExtra(java.lang.String)>",
            "<android.content.Intent: boolean[] getBooleanArrayExtra(java.lang.String)>",
            "<android.content.Intent: byte[] getByteArrayExtra(java.lang.String)>",
            "<android.content.Intent: short[] getShortArrayExtra(java.lang.String)>",
            "<android.content.Intent: char[] getCharArrayExtra(java.lang.String)>",
            "<android.content.Intent: int[] getIntArrayExtra(java.lang.String)>",
            "<android.content.Intent: long[] getLongArrayExtra(java.lang.String)>",
            "<android.content.Intent: float[] getFloatArrayExtra(java.lang.String)>",
            "<android.content.Intent: double[] getDoubleArrayExtra(java.lang.String)>",
            "<android.content.Intent: java.lang.String[] getStringArrayExtra(java.lang.String)>",
            "<android.content.Intent: java.lang.CharSequence[] getCharSequenceArrayExtra(java.lang.String)>",
            "<android.content.Intent: android.os.Bundle getBundleExtra(java.lang.String)>",
            "<android.os.BaseBundle: boolean getBoolean(java.lang.String)>",
            "<android.os.BaseBundle: boolean getBoolean(java.lang.String,boolean)>",
            "<android.os.BaseBundle: int getInt(java.lang.String)>",
            "<android.os.BaseBundle: int getInt(java.lang.String,int)>",
            "<android.os.BaseBundle: long getLong(java.lang.String)>",
            "<android.os.BaseBundle: long getLong(java.lang.String,long)>",
            "<android.os.BaseBundle: double getDouble(java.lang.String)>",
            "<android.os.BaseBundle: double getDouble(java.lang.String,double)>",
            "<android.os.BaseBundle: java.lang.String getString(java.lang.String)>",
            "<android.os.BaseBundle: java.lang.String getString(java.lang.String,java.lang.String)>",
            "<android.os.BaseBundle: boolean[] getBooleanArray(java.lang.String)>",
            "<android.os.BaseBundle: int[] getIntArray(java.lang.String)>",
            "<android.os.BaseBundle: long[] getLongArray(java.lang.String)>",
            "<android.os.BaseBundle: double[] getDoubleArray(java.lang.String)>",
            "<android.os.BaseBundle: java.lang.String[] getStringArray(java.lang.String)>",
            "<android.os.Bundle: java.lang.Object get(java.lang.String, java.lang.Object)>",
            "<android.os.Bundle: boolean getBoolean(java.lang.String)>",
            "<android.os.Bundle: boolean getBoolean(java.lang.String,boolean)>",
            "<android.os.Bundle: byte getByte(java.lang.String)>",
            "<android.os.Bundle: byte getByte(java.lang.String,byte)>",
            "<android.os.Bundle: short getShort(java.lang.String)>",
            "<android.os.Bundle: short getShort(java.lang.String,short)>",
            "<android.os.Bundle: char getChar(java.lang.String)>",
            "<android.os.Bundle: char getChar(java.lang.String,char)>",
            "<android.os.Bundle: int getInt(java.lang.String)>",
            "<android.os.Bundle: int getInt(java.lang.String,int)>",
            "<android.os.Bundle: long getLong(java.lang.String)>",
            "<android.os.Bundle: long getLong(java.lang.String,long)>",
            "<android.os.Bundle: float getFloat(java.lang.String)>",
            "<android.os.Bundle: float getFloat(java.lang.String,float)>",
            "<android.os.Bundle: double getDouble(java.lang.String)>",
            "<android.os.Bundle: double getDouble(java.lang.String,double)>",
            "<android.os.Bundle: java.lang.String getString(java.lang.String)>",
            "<android.os.Bundle: java.lang.String getString(java.lang.String,java.lang.String)>",
            "<android.os.Bundle: java.lang.CharSequence getCharSequence(java.lang.String)>",
            "<android.os.Bundle: java.lang.CharSequence getCharSequence(java.lang.String,java.lang.CharSequence)>",
            "<android.os.Bundle: android.os.Parcelable getParcelable(java.lang.String)>",
            "<android.os.Bundle: android.os.Parcelable getParcelable(java.lang.String,java.lang.Class)>",
            "<android.os.Bundle: android.os.Parcelable[] getParcelableArray(java.lang.String)>",
            "<android.os.Bundle: android.os.Parcelable[] getParcelableArray(java.lang.String,java.lang.Class)>",
            "<android.os.Bundle: java.util.ArrayList getParcelableArrayList(java.lang.String)>",
            "<android.os.Bundle: java.util.ArrayList getParcelableArrayList(java.lang.String,java.lang.Class)>",
            "<android.os.Bundle: java.io.Serializable getSerializable(java.lang.String)>",
            "<android.os.Bundle: java.io.Serializable getSerializable(java.lang.String,java.lang.Class)>",
            "<android.os.Bundle: java.util.ArrayList getIntegerArrayList(java.lang.String)>",
            "<android.os.Bundle: java.util.ArrayList getStringArrayList(java.lang.String)>",
            "<android.os.Bundle: java.util.ArrayList getCharSequenceArrayList(java.lang.String)>",
            "<android.os.Bundle: boolean[] getBooleanArray(java.lang.String)>",
            "<android.os.Bundle: byte[] getByteArray(java.lang.String)>",
            "<android.os.Bundle: short[] getShortArray(java.lang.String)>",
            "<android.os.Bundle: char[] getCharArray(java.lang.String)>",
            "<android.os.Bundle: int[] getIntArray(java.lang.String)>",
            "<android.os.Bundle: long[] getLongArray(java.lang.String)>",
            "<android.os.Bundle: float[] getFloatArray(java.lang.String)>",
            "<android.os.Bundle: double[] getDoubleArray(java.lang.String)>",
            "<android.os.Bundle: java.lang.String[] getStringArray(java.lang.String)>",
            "<android.os.Bundle: java.lang.CharSequence[] getCharSequenceArray(java.lang.String)>",
            "<android.os.Bundle: android.os.Bundle getBundle(java.lang.String)>",
            "<android.os.Bundle: android.os.IBinder getBinder(java.lang.String)>",
            "<android.os.Bundle: android.util.Size getSize(java.lang.String)>",
            "<android.os.Bundle: android.util.SizeF getSizeF(java.lang.String)>",
            "<android.os.Bundle: android.util.SparseArray getSizeF(java.lang.String)>",
            "<android.os.Bundle: android.util.SparseArray getSizeF(java.lang.String,java.lang.Class)>",
            "<android.content.SharedPreferences: java.lang.String getString(java.lang.String,java.lang.String)>",
            "<android.content.SharedPreferences: java.lang.String getBoolean(java.lang.String,boolean)>",
            "<android.content.SharedPreferences: java.lang.String getFloat(java.lang.String,float)>",
            "<android.content.SharedPreferences: java.lang.String getInt(java.lang.String,int)>",
            "<android.content.SharedPreferences: java.lang.String getLong(java.lang.String,long)>",
            "<android.content.SharedPreferences: java.lang.String getStringSet(java.lang.String,java.util.Set)>"},
            argIndexes = {BASE})
    public void mapHolderGet(Context context, Invoke invoke, PointsToSet mapObjs) {
        CSCallSite csCallSite = csManager.getCSCallSite(context, invoke);
        mapObjs.forEach(map -> map2GetInvoke.put(map, csCallSite));
    }

    protected void processUnresolvedMapGetInvoke() {
        map2GetInvoke.forEach((map, csCallSite) -> {
            CSVar key = csManager.getCSVar(csCallSite.getContext(), csCallSite.getCallSite().getInvokeExp().getArg(0));
            processResult(solver.getPointsToSetOf(key), map, csCallSite);
        });

        unresolvedMapGetInvoke.forEach((map, csCallSite) -> {
            Context context = csCallSite.getContext();
            Invoke callSite = csCallSite.getCallSite();
            Var result = callSite.getResult();
            if (result != null) {
                mapHolder.get(map).forEach(extra ->
                        solver.addPFGEdge(new AndroidTransferEdge(extra.value(), csManager.getCSVar(context, result)), result.getType()));
                processDefaultValue(context, callSite);
            }
        });
    }

    protected void processResult(PointsToSet keyObjs, CSObj map, CSCallSite csCallSite) {
        Context context = csCallSite.getContext();
        Invoke callSite = csCallSite.getCallSite();
        Var result = callSite.getResult();
        if (result == null) {
            return;
        }

        Set<MapHolder> extras = mapHolder.get(map);
        for (CSObj keyObj : keyObjs) {
            boolean isConstantObj = keyObj.getObject() instanceof ConstantObj;
            List<MapHolder> filterExtras = extras
                    .stream()
                    .filter(e -> !isConstantObj || solver.getPointsToSetOf(e.key()).contains(keyObj))
                    .toList();

            // if filterExtras is empty, then it must be unresolved.
            if (filterExtras.isEmpty()) {
                unresolvedMapGetInvoke.put(map, csCallSite);
            } else {
                filterExtras.forEach(extra -> {solver.addPFGEdge(new AndroidTransferEdge(extra.value(), csManager.getCSVar(context, result)), result.getType());});
                if (isConstantObj) {
                    unresolvedMapGetInvoke.remove(map, csCallSite);
                }
            }
        }
    }

    private void processDefaultValue(Context context, Invoke callSite) {
        if (callSite.getInvokeExp().getArgCount() > 1) {
            Var result = callSite.getResult();
            Var defaultValue = callSite.getInvokeExp().getArg(1);
            solver.addPFGEdge(new AndroidTransferEdge(csManager.getCSVar(context, defaultValue), csManager.getCSVar(context, result)), result.getType());
        }
    }
}
