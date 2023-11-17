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

package pascal.taie.analysis.pta.plugin.android.parser;

import org.xmlpull.v1.XmlPullParserException;
import pascal.taie.analysis.pta.plugin.android.entry.EntryEngine;
import pascal.taie.analysis.pta.plugin.android.fact.ApkInfo;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.collection.Streams;
import soot.Scene;
import soot.SootMethod;
import soot.jimple.infoflow.android.manifest.IAndroidComponent;
import soot.jimple.infoflow.android.manifest.ProcessManifest;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Parser Android Application Code or Resources
 * Temp use soot-infoflow-android to parse until new axml parse develop completed
 */
public class ApkParser implements IParser {

    private ApkInfo apkInfo;
    private final ProcessManifest manifest;

    private EntryEngine entryEngine;

    public ApkParser(String apkPath) {
        apkInfo = new ApkInfo();
        apkInfo.setAbsPath(apkPath);
        try {
            manifest = new ProcessManifest(apkPath);
        } catch (IOException | XmlPullParserException e) {
            throw new RuntimeException(e);
        }
        entryEngine =  new EntryEngine();
    }




    @Override
    public void parse() {
        basicWork();
        compoScan();
        doPrivilege();
    }

    /**
     * access basic info(eg: application name, package,sdk version..)
     */
    private void basicWork() {

         //TODO access app label name;
         //apkInfo.setApkName(xxx);

       apkInfo.setPkgName(manifest.getPackageName());
       apkInfo.setApplicationName(manifest.getApplication().getName());
       apkInfo.setApkSize(new File(apkInfo.getAbsPath()).length());


        //TODO judgement of handling application packer status;
        //apkInfo.setPacker(xxx);


       apkInfo.setVersion(manifest.getVersionName());
       apkInfo.setTargetSdkVersion(manifest.getTargetSdkVersion());
       apkInfo.setMinSdkVersion(manifest.getMinSdkVersion());

       //TODO get compile sdk version;
       //apkInfo.setCompileSdkVersion(xxx);

    }

    /**
     * scan component in manifest resources
     */
    private void compoScan() {
        //access all component by type
        apkInfo.addAllActivities(manifest.getActivities().asList().stream().map(IAndroidComponent::getNameString).collect(Collectors.toSet()));
        apkInfo.addAllServices(manifest.getServices().asList().stream().map(IAndroidComponent::getNameString).collect(Collectors.toSet()));
        apkInfo.addAllReceive(manifest.getBroadcastReceivers().asList().stream().map(IAndroidComponent::getNameString).collect(Collectors.toSet()));
        apkInfo.setAllProvider(manifest.getContentProviders().asList().stream().map(IAndroidComponent::getNameString).collect(Collectors.toSet()));

        //access all exported component by type
        apkInfo.addExportActivities(manifest.getActivities().asList().stream().filter(IAndroidComponent::isExported).map(IAndroidComponent::getNameString).collect(Collectors.toSet()));
        apkInfo.addExportServices(manifest.getServices().asList().stream().filter(IAndroidComponent::isExported).map(IAndroidComponent::getNameString).collect(Collectors.toSet()));
        apkInfo.addExportReceive(manifest.getBroadcastReceivers().asList().stream().filter(IAndroidComponent::isExported).map(IAndroidComponent::getNameString).collect(Collectors.toSet()));
        apkInfo.addExportProvider(manifest.getContentProviders().asList().stream().filter(IAndroidComponent::isExported).map(IAndroidComponent::getNameString).collect(Collectors.toSet()));


        Set<String> allSets = Streams.concat(apkInfo.getAllActivities().stream(),
                                            apkInfo.getAllServices().stream(),
                                            apkInfo.getAllReceive().stream(),
                                            apkInfo.getAllProvider().stream())
                                            .collect(Collectors.toSet());
        apkInfo.addAllComponents(allSets);

        //go on generating direct and implicit entries
        entryGenerate();
    }


    /**
     * generate application direct and implicit entries
     *
     */
    private void entryGenerate() {
        //add application class as the other entry
       entryEngine.generateAllComponentLifecycle(Streams.concat(apkInfo.getAllComponents().stream(),
                       Arrays.stream(new String[]{apkInfo.getApplicationName()}))
                       .filter(Predicate.not(Objects::isNull)).map(i -> Scene.v().getSootClassUnsafe(i))
                       .collect(Collectors.toSet()));
       apkInfo.addCompEntries(entryEngine.getAllEntries());
    }

    /**
     * generate application direct and implicit entries
     */
    private void doPrivilege(){
       //TODO access app permissions info (system and user custom);
       //apkInfo.setPermissions();


    }

    @Override
    public String getName() {
        return apkInfo.getApkName();
    }

    @Override
    public String getVersion() {
        return apkInfo.getVersion();
    }

    @Override
    public long getSize() {
        return apkInfo.getApkSize();
    }

    @Override
    public Set<SootMethod> getEntries() {
        return apkInfo.getCompEntries();
    }


}
