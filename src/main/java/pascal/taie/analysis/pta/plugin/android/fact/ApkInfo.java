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



package pascal.taie.analysis.pta.plugin.android.fact;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Sets;
import soot.SootMethod;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class ApkInfo {

    private String apkName;

    private String applicationName;

    private String pkgName;

    private String absPath;

    /**
     * ensure protected by packer company
     * false: 未加固
     * true: 已加固
     */
    private boolean packer;

    /**
     * packer type
     */
    private String protectType;

    private long apkSize;

    private String version;

    /**
     * sdk version with app compiler time
     */
    private int compileSdkVersion;


    /**
     * min sdk version with app support
     */
    private int minSdkVersion;

    /**
     * target sdk version with app support
     */
    private int targetSdkVersion;

    private Set<String> allComponents;
    private Set<String> exportActivities;
    private Set<String> allActivities;

    private Set<String> exportServices;
    private Set<String> allServices;

    private Set<String> exportReceive;
    private Set<String> allReceive;

    private Set<String> exportProvider;
    private Set<String> allProvider;

    private Map<String, String> permissions;

    private Set<UserPermission> userPermissions;


    private Set<SootMethod> compEntries;

    /**
     *  permission class with user custom definition
     */
    static class UserPermission {
        String name;
        String description;
        String permissionGroup;
        String protectionLevel;
    }

    public ApkInfo() {
        exportActivities = Sets.newConcurrentSet();
        allActivities = Sets.newConcurrentSet();
        exportServices = Sets.newConcurrentSet();
        allServices = Sets.newConcurrentSet();
        exportReceive =  Sets.newConcurrentSet();
        allReceive = Sets.newConcurrentSet();
        exportProvider = Sets.newConcurrentSet();
        allProvider = Sets.newConcurrentSet();
        allComponents =  Sets.newConcurrentSet();
        permissions = Maps.newConcurrentMap();
        userPermissions = Sets.newConcurrentSet();
        compEntries = Sets.newConcurrentSet();
    }


    public String getApkName() {
        return apkName;
    }

    public void setApkName(String apkName) {
        this.apkName = apkName;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getPkgName() {
        return pkgName;
    }

    public void setPkgName(String pkgName) {
        this.pkgName = pkgName;
    }

    public String getAbsPath() {
        return absPath;
    }

    public void setAbsPath(String absPath) {
        this.absPath = absPath;
    }

    public boolean isPacker() {
        return packer;
    }

    public void setPacker(boolean packer) {
        this.packer = packer;
    }

    public String getProtectType() {
        return protectType;
    }

    public void setProtectType(String protectType) {
        this.protectType = protectType;
    }

    public long getApkSize() {
        return apkSize;
    }

    public void setApkSize(long apkSize) {
        this.apkSize = apkSize;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getCompileSdkVersion() {
        return compileSdkVersion;
    }

    public void setCompileSdkVersion(int compileSdkVersion) {
        this.compileSdkVersion = compileSdkVersion;
    }

    public int getMinSdkVersion() {
        return minSdkVersion;
    }

    public void setMinSdkVersion(int minSdkVersion) {
        this.minSdkVersion = minSdkVersion;
    }

    public int getTargetSdkVersion() {
        return targetSdkVersion;
    }

    public void setTargetSdkVersion(int targetSdkVersion) {
        this.targetSdkVersion = targetSdkVersion;
    }

    public Set<String> getAllComponents() {
        return Collections.unmodifiableSet(allComponents);
    }

    public void addAllComponents(Set<String> allComponents) {
        this.allComponents.addAll(allComponents);
    }

    public Set<String> getExportActivities() {
        return Collections.unmodifiableSet(exportActivities);
    }

    public void addExportActivities(Set<String> exportActivities) {
        this.exportActivities.addAll(exportActivities);
    }

    public Set<String> getAllActivities() {
        return Collections.unmodifiableSet(allActivities);
    }

    public void addAllActivities(Set<String> allActivities) {
        this.allActivities.addAll(allActivities);
    }

    public Set<String> getExportServices() {
        return Collections.unmodifiableSet(exportServices);
    }

    public void addExportServices(Set<String> exportServices) {
        this.exportServices.addAll(exportServices);
    }

    public Set<String> getAllServices() {
        return Collections.unmodifiableSet(allServices);
    }

    public void addAllServices(Set<String> allServices) {
        this.allServices.addAll(allServices);
    }

    public Set<String> getExportReceive() {
        return Collections.unmodifiableSet(exportReceive);
    }

    public void addExportReceive(Set<String> exportReceive) {
        this.exportReceive.addAll(exportReceive);
    }

    public Set<String> getAllReceive() {
        return Collections.unmodifiableSet(allReceive);
    }

    public void addAllReceive(Set<String> allReceive) {
        this.allReceive.addAll(allReceive);
    }

    public Set<String> getExportProvider() {
        return Collections.unmodifiableSet(exportProvider);
    }

    public void addExportProvider(Set<String> exportProvider) {
        this.exportProvider.addAll(exportProvider);
    }

    public Set<String> getAllProvider() {
        return Collections.unmodifiableSet(allProvider);
    }

    public void setAllProvider(Set<String> allProvider) {
        this.allProvider.addAll(allProvider);
    }

    public Map<String, String> getPermissions() {
        return Collections.unmodifiableMap(permissions);
    }

    public void addPermissions(Map<String, String> permissions) {
        this.permissions.putAll(permissions);
    }

    public Set<UserPermission> getUserPermissions() {
        return Collections.unmodifiableSet(userPermissions);
    }

    public void addUserPermissions(Set<UserPermission> userPermissions) {
        this.userPermissions.addAll(userPermissions);
    }

    public Set<SootMethod> getCompEntries() {
        return Collections.unmodifiableSet(compEntries);
    }

    public void addCompEntries(Set<SootMethod> compEntries) {
        this.compEntries.addAll(compEntries);
    }

}
