package com.mcmiddleearth.pvp;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/** File helpers for tests that work on copies of the fixtures. */
public final class TestFiles {

    private TestFiles() {
    }

    /** Copies a classpath resource such as {@code /fixtures/maps/AS-TDM} to {@code target}, creating its folder. */
    public static Path copyResource(String resource, Path target) throws IOException {
        try (InputStream in = TestFiles.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException(resource + " is not on the test classpath");
            }
            Files.createDirectories(target.toAbsolutePath().getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            return target;
        }
    }

    /**
     * A byte-exact picture of a folder: every path below {@code dir} ('/'-separated) mapped to the file's bytes in
     * hex, or to {@code <dir>} for a folder. Two equal snapshots mean nothing was added, removed or changed.
     */
    public static Map<String, String> snapshot(Path dir) throws IOException {
        List<Path> paths;
        try (Stream<Path> walk = Files.walk(dir)) {
            paths = walk.toList();
        }
        Map<String, String> picture = new TreeMap<>();
        for (Path path : paths) {
            String name = dir.relativize(path).toString().replace('\\', '/');
            picture.put(name, Files.isDirectory(path) ? "<dir>" : HexFormat.of().formatHex(Files.readAllBytes(path)));
        }
        return picture;
    }
}
