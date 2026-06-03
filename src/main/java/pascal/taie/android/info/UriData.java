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
 * Models the data constraints of Android intent filter.
 *
 * <p>The same record is used on both sides of ICC resolution: manifest filters
 * provide the accepted URI/MIME constraints, while caller-created intents provide
 * the queried URI/MIME values.
 */
public record UriData(@Nullable String scheme,
                      @Nullable String host,
                      @Nullable String port,
                      @Nullable String path,
                      @Nullable String pathPrefix,
                      @Nullable String pathSuffix,
                      @Nullable String pathPattern,
                      @Nullable String pathAdvancedPattern,
                      @Nullable String mimeType) {

    private static final List<String> DEFAULT_MIME_SCHEMES = List.of("content", "file");

    /**
     * Preserves the previous behavior: when an intent has an HTTP(S) URI but no
     * explicit port, a filter-side port constraint is treated as matched.
     */
    private static final List<String> IMPLICIT_PORT_SCHEMES = List.of("http", "https");

    public static Builder builder() {
        return new Builder();
    }

    private static UriData of(Builder builder) {
        return new UriData(
                builder.scheme,
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
     * Builder for {@link UriData}. All fields are optional because Android allows
     * partial data declarations, e.g., MIME-only or scheme-only filters.
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

        /**
         * Copies all fields from an existing {@link UriData}. Kept as {@code data}
         * for source compatibility with existing callers such as data merging.
         */
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
     * Returns whether this filter-side data declaration can match the given
     * intent-side data.
     *
     * <p>The matching rule follows Android's intent-filter data matching order:
     *
     * @see <a href="https://developer.android.com/guide/topics/manifest/data-element">Android data element</a>
     * @see <a href="https://developer.android.com/guide/components/intents-filters">Android intents and filters</a>
     */
    public boolean match(UriData uriData) {
        boolean uriMatches = matchesUri(uriData);
        boolean mimeTypeMatches = matchesMimeType(uriData.mimeType);

        if (uriData.scheme != null && uriData.host != null && uriData.mimeType != null) {
            return (uriMatches || DEFAULT_MIME_SCHEMES.contains(uriData.scheme)) && mimeTypeMatches;
        } else {
            return uriData.mimeType == null ?
                    uriMatches && this.mimeType == null :
                    emptyUri() && mimeTypeMatches;
        }
    }

    private boolean matchesUri(UriData uriData) {
        boolean schemeMatches = matchesScheme(uriData.scheme);
        boolean authorityMatches = hasSchemeConstraint() && matchesHost(uriData.host) && matchesPort(uriData.scheme, uriData.port);
        boolean pathMatches = hasSchemeConstraint() && hasAuthorityConstraint() && matchesPath(uriData.path);
        return (schemeMatches && !hasAuthorityConstraint() && !hasPathConstraint())
                || (schemeMatches && authorityMatches && !hasPathConstraint())
                || (schemeMatches && authorityMatches && pathMatches);
    }

    private boolean emptyUri() {
        return this.scheme == null && !hasAuthorityConstraint() && !hasPathConstraint();
    }

    private boolean hasSchemeConstraint() {
        return this.scheme != null;
    }

    private boolean hasAuthorityConstraint() {
        return this.host != null || this.port != null;
    }

    private boolean hasPathConstraint() {
        return this.path != null
                || this.pathPrefix != null
                || this.pathPattern != null
                || this.pathSuffix != null
                || this.pathAdvancedPattern != null;
    }

    private boolean matchesScheme(String scheme) {
        return this.scheme != null && this.scheme.equals(scheme);
    }

    private boolean matchesHost(String host) {
        return this.host == null || (host != null
                && (this.host.equals(host)
                || (this.host.startsWith("*")
                && host.matches(this.host.replaceFirst("^\\*", ".*")))));
    }

    private boolean matchesPort(String scheme, String port) {
        return (port == null && scheme != null && IMPLICIT_PORT_SCHEMES.contains(scheme))
                || (this.port != null && this.port.equals(port));
    }

    private boolean matchesPath(String path) {
        return !hasPathConstraint() || path == null
                || matchesExactPath(path)
                || matchesPathPrefix(path)
                || matchesPathSuffix(path)
                || matchesPathPattern(path)
                || matchesPathAdvancedPattern(path);
    }

    private boolean matchesExactPath(String path) {
        return path.equals(this.path);
    }

    private boolean matchesPathPrefix(String path) {
        return this.pathPrefix != null && path.startsWith(this.pathPrefix);
    }

    private boolean matchesPathSuffix(String path) {
        return this.pathSuffix != null && path.endsWith(this.pathSuffix);
    }

    private boolean matchesPathPattern(String path) {
        return this.pathPattern != null && matchesPathPattern(path, this.pathPattern);
    }

    private boolean matchesPathAdvancedPattern(String path) {
        return this.pathAdvancedPattern != null && matchesPathPattern(path, this.pathAdvancedPattern);
    }

    private boolean matchesMimeType(String mimeType) {
        return mimeType != null && this.mimeType != null && matchesMimeTypePattern(mimeType, this.mimeType);
    }

    private boolean matchesPathPattern(String path, String regex) {
        return path.matches(regex);
    }

    private boolean matchesMimeTypePattern(String mimeType, String fakeRegex) {
        return fakeRegex.equals(mimeType) || mimeType.matches(fakeRegex.replace("*", ".*"));
    }
}
