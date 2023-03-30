package pascal.taie.analysis.pta;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

record BenchmarkInfo(String id, int jdk,
                     String main, List<String> apps, List<String> libs,
                     String reflectionLog, boolean allowPhantom) {
    @JsonCreator
    public BenchmarkInfo(
            @JsonProperty("id") String id,
            @JsonProperty("jdk") int jdk,
            @JsonProperty("main") String main,
            @JsonProperty("apps") List<String> apps,
            @JsonProperty("libs") List<String> libs,
            @JsonProperty("refl-log") String reflectionLog,
            @JsonProperty("phantom") boolean allowPhantom) {
        this.id = id;
        this.jdk = jdk;
        this.main = main;
        this.apps = Objects.requireNonNullElse(apps, List.of());
        this.libs = Objects.requireNonNullElse(libs, List.of());
        this.reflectionLog = reflectionLog;
        this.allowPhantom = allowPhantom;
    }

    static Map<String, BenchmarkInfo> load(String path) {
        File file = new File(path);
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        TypeReference<List<BenchmarkInfo>> typeRef = new TypeReference<>() {};
        try {
            Map<String, BenchmarkInfo> benchmarkInfos = new LinkedHashMap<>();
            mapper.readValue(file, typeRef).forEach(
                    bmInfo -> benchmarkInfos.put(bmInfo.id(), bmInfo));
            return benchmarkInfos;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
