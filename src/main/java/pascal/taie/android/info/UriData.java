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

import javax.annotation.Nullable;
import java.util.List;

/**
 * Contains information about data in intent-filter.
 */
public record UriData(@Nullable
                   String scheme,
                      @Nullable
                   String host,
                      @Nullable
                   String port,
                      @Nullable
                   String path,
                      @Nullable
                   String pathPrefix,
                      @Nullable
                   String pathSuffix,
                      @Nullable
                   String pathPattern,
                      @Nullable
                   String pathAdvancedPattern,
                      @Nullable
                   String mimeType) {

    private static final List<String> PASS_SCHEMES = List.of("http", "https");

    private static final List<String> DEFAULT_SCHEMES = List.of("content", "file");

    public static Builder builder() {
        return new Builder();
    }

    private static UriData of(Builder builder) {
        return new UriData(builder.scheme,
                builder.host,
                builder.port,
                builder.path,
                builder.pathPrefix,
                builder.pathSuffix,
                builder.pathPattern,
                builder.pathAdvancedPattern,
                builder.mimeType);
    }

    /**
     * Builder for building data info.
     */
    public static class Builder {

        private String scheme;

        private String host;

        private String port;

        private String path;

        private String pathPrefix;

        private String pathSuffix;

        private String pathPattern;

        private String pathAdvancedPattern;

        private String mimeType;

        private Builder() {
        }

        public Builder scheme(String scheme) {
            this.scheme = scheme;
            return this;
        }

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(String port) {
            this.port = port;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder pathPrefix(String pathPrefix) {
            this.pathPrefix = pathPrefix;
            return this;
        }

        public Builder pathSuffix(String pathSuffix) {
            this.pathSuffix = pathSuffix;
            return this;
        }

        public Builder pathPattern(String pathPattern) {
            this.pathPattern = pathPattern;
            return this;
        }

        public Builder pathAdvancedPattern(String pathAdvancedPattern) {
            this.pathAdvancedPattern = pathAdvancedPattern;
            return this;
        }

        public Builder mimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        public Builder data(UriData uriData) {
            this.scheme = uriData.scheme;
            this.host = uriData.host;
            this.port = uriData.port;
            this.path = uriData.path;
            this.pathPrefix = uriData.pathPrefix;
            this.pathSuffix = uriData.pathSuffix;
            this.pathPattern = uriData.pathPattern;
            this.pathAdvancedPattern = uriData.pathAdvancedPattern;
            this.mimeType = uriData.mimeType;
            return this;
        }

        public UriData build() {
            return UriData.of(this);
        }

    }

    /**
     * matching rule refers
     * <a href="https://developer.android.com/guide/topics/manifest/data-element?hl=zh-cn">...</a>
     * <a href="https://developer.android.com/guide/components/intents-filters?hl=zh-cn">...</a>
     */
    public boolean match(UriData uriData) {
        boolean uriMatch = hasURI(uriData);
        boolean mimeTypeMatch = hasMimeType(uriData.mimeType);

        if (uriData.scheme != null && uriData.host != null && uriData.mimeType != null) {
            return (uriMatch || DEFAULT_SCHEMES.contains(uriData.scheme)) && mimeTypeMatch;
        } else {
            return uriData.mimeType == null ? uriMatch && this.mimeType == null : emptyUri() && mimeTypeMatch;
        }
    }

    private boolean hasURI(UriData uriData) {
        boolean hasScheme = hasScheme(uriData.scheme);
        boolean hasAuthority = !emptyScheme() && hasHost(uriData.host) && hasPort(uriData.scheme, uriData.port);
        boolean hasPath = !emptyScheme() && !emptyAuthority() && pathMatch(uriData.path);
        return (hasScheme && emptyAuthority() && emptyPath())
                || (hasScheme && hasAuthority && emptyPath())
                || (hasScheme && hasAuthority && hasPath);
    }

    private boolean emptyUri() {
        return this.scheme == null && emptyAuthority() && emptyPath();
    }

    private boolean hasScheme(String scheme) {
        return this.scheme != null && this.scheme.equals(scheme);
    }

    private boolean hasHost(String host) {
        return this.host == null || (host != null
                && (this.host.equals(host)
                || (this.host.startsWith("*")
                && host.matches(this.host.replaceFirst("^\\*", ".*")))));
    }

    private boolean hasPort(String scheme, String port) {
        return (port == null && scheme != null && PASS_SCHEMES.contains(scheme))
                || (this.port != null && this.port.equals(port));
    }

    private boolean pathMatch(String path) {
        return emptyPath() || path == null
                || hasPath(path)
                || hasPathPrefix(path)
                || hasPathSuffix(path)
                || hasPathPattern(path)
                || hasPathAdvancedPattern(path);
    }

    private boolean emptyScheme() {
        return this.scheme == null;
    }

    private boolean emptyAuthority() {
        return this.host == null && this.port == null;
    }

    private boolean emptyPath() {
        return this.path == null
                && this.pathPrefix == null
                && this.pathPattern == null
                && this.pathSuffix == null
                && this.pathAdvancedPattern == null;
    }

    private boolean hasPath(String path) {
        return path.equals(this.path);
    }

    private boolean hasPathPrefix(String path) {
        return this.pathPrefix != null && path.startsWith(this.pathPrefix);
    }

    private boolean hasPathSuffix(String path) {
        return this.pathSuffix != null && path.endsWith(this.pathSuffix);
    }

    private boolean hasPathPattern(String path) {
        return this.pathPattern != null && pathMatch(path, this.pathPattern);
    }

    private boolean hasPathAdvancedPattern(String path) {
        return this.pathAdvancedPattern != null && pathMatch(path, this.pathAdvancedPattern);
    }

    private boolean hasMimeType(String mimeType) {
        return mimeType != null && this.mimeType != null && mimeTypeMatch(mimeType, this.mimeType);
    }

    private boolean pathMatch(String path, String regex) {
        return path.matches(regex);
    }

    private boolean mimeTypeMatch(String mimeType, String fakeRegex) {
        return fakeRegex.equals(mimeType) || mimeType.matches(fakeRegex.replace("*", ".*"));
    }
}
