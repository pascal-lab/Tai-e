package pascal.taie.android.util;

import java.util.List;

public class AndroidSystemClassUtil {

    private static final List<String> SYSTEM_PACKAGES =
            List.of("java.",
                    "javax.",
                    "sun.",
                    "com.sun.",
                    "org.omg.",
                    "org.xml",
                    "org.w3c.dom",
                    "org.json",
                    "org.apache.",
                    "dalvik.system.",
                    "com.android.",
                    "com.google.",
                    "android.",
                    "androidx.",
                    "kotlin.",
                    "kotlinx.");

    public static boolean isApplicationClass(String name) {
        return SYSTEM_PACKAGES.stream().noneMatch(name::startsWith);
    }

    public static boolean haveStartWithName(String name) {
        return name != null && !name.isEmpty() && SYSTEM_PACKAGES.stream().anyMatch(name::startsWith);
    }

}
