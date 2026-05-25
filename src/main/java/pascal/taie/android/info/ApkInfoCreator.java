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

package pascal.taie.android.info;

import pascal.taie.config.Options;
import pascal.taie.language.classes.ClassHierarchy;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.android.resources.LayoutFileParser;

import java.io.File;

public final class ApkInfoCreator {

    private ApkInfoCreator() {
    }

    public static ApkInfo create(Options options, ClassHierarchy hierarchy) {
        RawApkInfo rawApkInfo = getRawApkInfo(getApkPath(options));
        ApkInfo.ApkInfoConverter converter =
                new ApkInfo.ApkInfoConverter(rawApkInfo, hierarchy);

        return new ApkInfo(
                converter.convertApplication(),
                converter.convertExportedActivities(),
                converter.convertExportedServices(),
                converter.convertExportedBroadcastReceivers(),
                converter.convertExportedContentProviders(),
                converter.convertEnabledActivities(),
                converter.convertEnabledServices(),
                converter.convertEnabledBroadcastReceivers(),
                converter.convertEnabledContentProviders(),
                converter.convertLayoutCallbacks(),
                converter.convertLayoutFragments(),
                converter.convertLayoutViews(),
                converter.convertAndroidCallbacks(),
                converter.convertComponentFilterAttribute(),
                rawApkInfo
        );
    }

    private static RawApkInfo getRawApkInfo(String apkPath) {
        try {
            ARSCFileParser resources = new ARSCFileParser();
            resources.parse(new File(apkPath));
            ProcessManifest manifest = new ProcessManifest(new File(apkPath), resources);
            LayoutFileParser layoutFile = new LayoutFileParser(manifest.getPackageName(), apkPath, resources);
            return new RawApkInfo(manifest, layoutFile, resources);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String getApkPath(Options options) {
        if (options.getClassPath().isEmpty()) {
            throw new RuntimeException("APK path is not specified");
        }
        return options.getClassPath().get(0);
    }
}
