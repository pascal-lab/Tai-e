package pascal.taie.android.util;

import java.util.List;

public class AndroidSystemClassUtil {

    private static final List<String> SYSTEM_PACKAGES =
            List.of(
                    "org.xml",
                    "org.w3c.dom",
                    "org.json",
                    "org.apache.",
                    "dalvik.system.",
                    "android.",
                    "androidx."
            );

    public static boolean isAndroidSystemClass(String name) {
        return SYSTEM_PACKAGES.stream().anyMatch(name::startsWith);
    }

}
