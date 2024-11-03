package pascal.taie.project;

import java.nio.file.Path;

public class PathUtils {
    /**
     * @param path relative path from root (jar, dir, ...), e.g.
     *             <code>java/lang/Object.class</code>
     * @return the internal name of the class, which is the path with the file extension removed
     * and slashes replaced by dots. e.g. <code>java.lang.Object</code>
     */
    public static String getInternalName(Path path) {
        String extRemoved = removeExt(path.toString());
        return extRemoved.replace('\\', '/') // in case you're windows
                .replace('/', '.');
    }

    public static String getClassName(Path path) {
        return removeExt(path.getFileName().toString());
    }

    private static String removeExt(String s) {
        int dotIndex = s.lastIndexOf('.');
        return (dotIndex == -1) ? s : s.substring(0, dotIndex);
    }
}
