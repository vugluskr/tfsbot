package utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

import static utils.TextUtils.isEmpty;
import static utils.TextUtils.notNull;

public class Test {

    public static void main(final String[] argz) throws Exception {
        final Path namesFile = Paths.get("/home/lucky/tmp/names.txt");
        final Path pathsFile = Paths.get("/home/lucky/tmp/paths.txt");
        final Path p = Paths.get("/home/lucky/tmp/genres_fb2.glst");
        final Path p2 = Paths.get("/home/lucky/tmp/genres_nonfb2.glst");

        final SortedMap<String, String> names = new TreeMap<>();
        final SortedSet<Path> paths = new TreeSet<>();

        doFile(p, names, paths);
        doFile(p2, names, paths);

        for (final Map.Entry<String, String> e : names.entrySet())
            Files.write(namesFile, (e.getKey() + " : " + e.getValue() + "\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);

        for (final Path l : paths)
            Files.write(pathsFile, (l + "\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
    }

    private static void doFile(final Path p, final SortedMap<String, String> names, final SortedSet<Path> paths) throws IOException {
        final List<String> all = Files.readAllLines(p);
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).startsWith("#") || isEmpty(all.get(i))) {
                all.remove(i--);
                continue;
            }
            final String l = all.get(i);
            final int space = l.indexOf(' ');
            final int semi = l.indexOf(';');

            if (space == -1 || semi == -1 || semi <= space) {
                all.remove(i--);
                continue;
            }

            final String id = l.substring(0, l.indexOf(' '));
            final String tag = notNull(l.substring(space, semi));
            final String title = l.substring(semi + 1);

            names.put(tag, title);
            if (id.length() < 4) {
                paths.add(Paths.get(tag));

                getid(id, Paths.get(tag), i, all, paths);
            }
        }

    }

    private static void getid(final String id, final Path tag, final int i, final List<String> all, final SortedSet<Path> paths) {
        boolean started = false;

        for (int j = i + 1; j < all.size(); j++)
            if (!all.get(j).startsWith(id)) {
                if (started)
                    break;
            } else {
                if (!started)
                    started = true;

                final String m = all.get(j);
                final int space = m.indexOf(' ');
                final int semi = m.indexOf(';');

                final String myid = m.substring(0, space);
                final String mytag = notNull(m.substring(space, semi));

//                if (paths.contains(mytag))
//                    continue;

//                paths.stream().filter(mytag::startsWith).collect(Collectors.toList()).forEach(paths::remove);
                final Path myPath = tag.resolve(mytag);
                paths.add(tag.resolve(mytag));

                getid(myid, myPath, j, all, paths);
            }
    }
}
