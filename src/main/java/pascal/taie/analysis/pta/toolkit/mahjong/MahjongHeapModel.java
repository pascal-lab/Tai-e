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

package pascal.taie.analysis.pta.toolkit.mahjong;

import pascal.taie.analysis.pta.core.heap.AbstractHeapModel;
import pascal.taie.analysis.pta.core.heap.MergedObj;
import pascal.taie.analysis.pta.core.heap.NewObj;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.config.AnalysisOptions;
import pascal.taie.ir.stmt.New;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.CollectionUtils;
import pascal.taie.util.collection.Maps;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

class MahjongHeapModel extends AbstractHeapModel {

    // currently, perform merging for only NewObj
    private final Map<New, MergedObj> mergedObjs;

    MahjongHeapModel(AnalysisOptions options, Collection<Set<Obj>> objGroups) {
        super(options);
        mergedObjs = buildMergeMap(objGroups);
    }

    private Map<New, MergedObj> buildMergeMap(Collection<Set<Obj>> objGroups) {
        Map<New, MergedObj> mergedObjs = Maps.newMap();
        objGroups.forEach(objs -> {
            Type type = CollectionUtils.getOne(objs).getType();
            MergedObj mergedObj = new MergedObj(type,
                    "<Mahjong Merged + " + type + " >");
            objs.forEach(obj -> {
                if (obj instanceof NewObj newObj) {
                    mergedObjs.put(newObj.getAllocation(), mergedObj);
                    mergedObj.addRepresentedObj(obj);
                }
            });
        });
        return mergedObjs;
    }

    @Override
    protected Obj doGetObj(New allocSite) {
        return mergedObjs.get(allocSite);
    }
}
