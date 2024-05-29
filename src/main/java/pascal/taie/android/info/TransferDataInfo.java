package pascal.taie.android.info;

import java.util.Collections;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record TransferDataInfo(Set<String> schemes,
                               Set<String> hosts,
                               Set<String> ports,
                               Set<String> paths,
                               Set<String> pathPrefixes,
                               Set<String> pathSuffixes,
                               Set<String> pathPatterns,
                               Set<String> pathAdvancedPatterns,
                               Set<String> mimeTypes) {

    public Set<UriData> convertToDataSet() {
        return convertToDataSet(0,
                schemes,
                hosts,
                ports,
                paths,
                pathPrefixes,
                pathSuffixes,
                pathPatterns,
                pathAdvancedPatterns,
                mimeTypes);
    }

    @SafeVarargs
    private static Set<UriData> convertToDataSet(int index, Set<String>... sets) {
        if (index < 0 || index >= sets.length) {
            return Collections.emptySet();
        }

        // Recursively process the next set
        Set<UriData> subResult = convertToDataSet(index + 1, sets);
        if (sets[index].isEmpty()) {
            return subResult;
        }
        if (subResult.isEmpty()) {
            return generateDataSet(
                    value -> Stream.of(createDataWithNulls(index, value, UriData.builder().build())),
                    sets[index]);
        }

        return generateDataSet(
                value -> subResult.stream().map(data -> createDataWithNulls(index, value, data)),
                sets[index]);
    }

    private static Set<UriData> generateDataSet(Function<String, Stream<UriData>> mapper, Set<String> set) {
        return set
                .stream()
                .flatMap(mapper)
                .collect(Collectors.toSet());
    }

    private static UriData createDataWithNulls(int index, String value, UriData uriData) {
        return switch (index) {
            case 0 -> UriData.builder().data(uriData).scheme(value).build();
            case 1 -> UriData.builder().data(uriData).host(value).build();
            case 2 -> UriData.builder().data(uriData).port(value).build();
            case 3 -> UriData.builder().data(uriData).path(value).build();
            case 4 -> UriData.builder().data(uriData).pathPrefix(value).build();
            case 5 -> UriData.builder().data(uriData).pathSuffix(value).build();
            case 6 -> UriData.builder().data(uriData).pathPattern(value).build();
            case 7 -> UriData.builder().data(uriData).pathAdvancedPattern(value).build();
            case 8 -> UriData.builder().data(uriData).mimeType(value).build();
            default -> throw new IllegalArgumentException("Invalid index");
        };
    }

}
