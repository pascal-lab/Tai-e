package pascal.taie.analysis.pta.plugin.cutshortcut.container;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import pascal.taie.analysis.pta.plugin.cutshortcut.container.enums.ContExitCategory;
import pascal.taie.analysis.pta.plugin.cutshortcut.container.enums.ContainerType;
import pascal.taie.analysis.pta.plugin.cutshortcut.container.enums.ExtendType;
import pascal.taie.analysis.pta.plugin.cutshortcut.container.enums.IterExitCategory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class MakeDefaultContainerConfig {
    public static void make() {
        ContainerConfig config = ContainerConfig.config;
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

            // [Map/Col] classes
            Map<String, List<String>> rawHostClassesData = mapper.readValue(
                    Thread.currentThread().getContextClassLoader().getResourceAsStream("container-config/host-classes.yml"),
                    new TypeReference<>() {});
            rawHostClassesData.forEach((containerName, containerClasses) -> {
                ContainerType containerType = ContainerType.getTypeName(containerName);
                containerClasses.forEach(containerClassName ->
                        config.addHostClass(containerType, containerClassName));
            });

            // [Other Class]
            config.resolveUnmodeledClasses();

            // [Entrance-Append]
            Map<String, List<Map<String, Object>>> rawEntranceAppendDatas = mapper.readValue(
                    Thread.currentThread().getContextClassLoader().getResourceAsStream("container-config/entrance-append.yml"),
                    new TypeReference<>() {});
            rawEntranceAppendDatas.forEach((categoryName, APIInfos) -> {
                ContExitCategory category = ContExitCategory.getCategory(categoryName);
                APIInfos.stream().filter(Objects::nonNull).forEach(APIInfo -> {
                    String methodSig = (String) APIInfo.get("signature");
                    Integer index = (Integer) APIInfo.get("index");
                    config.addEntranceAppendIndex(methodSig, category, index);
                });
            });

            // [Entrance-Extend]
            Map<String, List<Map<String, Object>>> rawEntranceExtendDatas = mapper.readValue(
                    Thread.currentThread().getContextClassLoader().getResourceAsStream("container-config/entrance-extend.yml"),
                    new TypeReference<>() {});
            rawEntranceExtendDatas.forEach((extendTypeValue, APIInfos) -> {
                ExtendType extendType = ExtendType.getExtendType(extendTypeValue);
                APIInfos.stream().filter(Objects::nonNull).forEach(APIInfo -> {
                    String methodSig = (String) APIInfo.get("signature");
                    Integer index = (Integer) APIInfo.get("index");
                    config.addEntranceExtendIndex(methodSig, extendType, index);
                });

            });

            // [Entrance-Array Initializer]
            Map<String, List<Map<String, Object>>> rawArrayInitializerDatas = mapper.readValue(
                    Thread.currentThread().getContextClassLoader().getResourceAsStream("container-config/array-initializer.yml"),
                    new TypeReference<>() {});
            rawArrayInitializerDatas.get("ArrayInit").forEach(APIInfo -> {
                String methodSig = (String) APIInfo.get("signature");
                Integer srcIndex = (Integer) APIInfo.get("src");
                Integer dstIndex = (Integer) APIInfo.get("dst");
                config.addArrayInitializer(methodSig, srcIndex, dstIndex);
            });

            // [Exit]
            Map<String, List<String>> rawExitDatas = mapper.readValue(
                    Thread.currentThread().getContextClassLoader().getResourceAsStream("container-config/exit.yml"),
                    new TypeReference<>() {});
            rawExitDatas.forEach((exitType, methodsigs) -> {
                ContExitCategory category = ContExitCategory.getCategory(exitType);
                IterExitCategory iterExitCategory = IterExitCategory.getCategory(exitType);
                // Container-Exit
                if (category != null)
                    methodsigs.stream().filter(Objects::nonNull).forEach(methodsig -> config.addContainerExitCategory(methodsig, category));
                // Iter-Exit
                else if (iterExitCategory != null)
                    methodsigs.stream().filter(Objects::nonNull).forEach(methodsig -> config.addIterExitCategory(methodsig, iterExitCategory));
            });

        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
