import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FlatMap {

    private static final int EXPECTED_NUM_WORDS = 114_761;

    static final class Contestant implements Comparable<Contestant> {
        private final String name;
        private final Function<Map<?, List<String>>, List<String>> func;
        private long iterations;
        private Duration cumulativeElapsed;
        public Contestant(String name, Function<Map<?, List<String>>, List<String>> func) {
            this.name = Objects.requireNonNull(name);
            this.func = Objects.requireNonNull(func);
            this.iterations = 0;
            this.cumulativeElapsed = Duration.ZERO;
        }
        private void recordRun(Duration elapsed) {
            iterations++;
            this.cumulativeElapsed = this.cumulativeElapsed.plus(elapsed);
        }

        @Override
        public String toString() {
            return "Contestant{" +
                    "name='" + name + '\'' +
                    ", iterations=" + iterations +
                    ", cumulativeElapsed=" + cumulativeElapsed +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Contestant that = (Contestant) o;
            return Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        @Override
        public int compareTo(FlatMap.Contestant o) {
            return cumulativeElapsed.compareTo(o.cumulativeElapsed);
        }
    } 
    
    public static void main(String[] args) throws URISyntaxException, IOException {
        List<String> muchLoremIpsum = loadData(); //114,761
        Set<String> distinctWords = new HashSet<>(muchLoremIpsum); //516
        Map<String, List<String>> map = getStringListMap(muchLoremIpsum, 1000);

        long iterations = 10_000;
        long iterationsPerContestant = 1_000;

        List<Contestant> contestants = Arrays.asList(
                new Contestant("flattenImperative1", FlatMap::flattenImperative1),
                new Contestant("flattenImperative2", FlatMap::flattenImperative2),
                new Contestant("flattenImperative3", FlatMap::flattenImperative3),
                new Contestant("flattenImperative4", FlatMap::flattenImperative4),
                new Contestant("flattenStreams1", FlatMap::flattenStreams1),
                new Contestant("flattenStreams2", FlatMap::flattenStreams2),
                new Contestant("flattenStreams3", FlatMap::flattenStreams3),
                new Contestant("flattenStreams4", FlatMap::flattenStreams4),
                new Contestant("flattenStreams5", FlatMap::flattenStreams5)
        );

        new Random().ints(iterations, 0, contestants.size()).forEach(idx -> {
            Contestant contestant = contestants.get(idx);
            if (contestant.iterations == iterationsPerContestant) {
                return;
            }
            Instant start = Instant.now();
            List<String> result = contestant.func.apply(map);
            Instant end = Instant.now();
            if (! new HashSet<>(result).containsAll(distinctWords)) {
                throw new RuntimeException("contestant failed: " + contestant);
            }
            Duration duration = Duration.between(start, end);
            contestant.recordRun(duration);
        });
        
        contestants.stream().sorted().forEach(System.out::println);
    }

    private static List<String> loadData() throws URISyntaxException, IOException {
        URL resource = FlatMap.class.getResource("lorem-ipsum.txt");
        URI uri = resource.toURI();
        Path path = Paths.get(uri);
        List<String> toReturn = new LinkedList<>();
        try (Stream<String> stream = Files.lines(path)) {
            stream.flatMap(s -> Arrays.stream(s.split("\\s")))
                    .forEach(toReturn::add);
        }
        return toReturn;
    }

    private static Map<String, List<String>> getStringListMap(List<String> in, int numWordsPerMapEntry) {
        Map<String, List<String>> out = new HashMap<>();
        int totalSize = in.size();
        int numEntries = (int) Math.floor((double) totalSize / numWordsPerMapEntry);
        for (int i = 0; i < numEntries; i++) {
            out.put(String.valueOf(i), in.stream().skip((long) i * numWordsPerMapEntry).limit(numWordsPerMapEntry).collect(Collectors.toList()));
        }
        return out;
    }

    public static <E> List<E> flattenImperative1(Map<?, List<E>> map) {
        List<E> toReturn = new ArrayList<>();
        for (Object o : map.keySet()) {
            toReturn.addAll(map.get(o));
        }
        return toReturn;
    }

    public static <E> List<E> flattenImperative2(Map<?, List<E>> map) {
        List<E> toReturn = new ArrayList<>();
        Set<? extends Map.Entry<?, List<E>>> entries = map.entrySet();
        for (Map.Entry<?, List<E>> entry : entries) {
            toReturn.addAll(entry.getValue());
        }
        return toReturn;
    }

    public static <E> List<E> flattenImperative3(Map<?, List<E>> map) {
        List<E> toReturn = new ArrayList<>();
        Collection<List<E>> values = map.values();
        for (List<E> value : values) {
            toReturn.addAll(value);
        }
        return toReturn;
    }

    public static <E> List<E> flattenImperative4(Map<?, List<E>> map) {
        List<E> toReturn = new ArrayList<>(EXPECTED_NUM_WORDS);
        Set<? extends Map.Entry<?, List<E>>> entries = map.entrySet();
        for (Map.Entry<?, List<E>> entry : entries) {
            toReturn.addAll(entry.getValue());
        }
        return toReturn;
    }
    
    public static <E> List<E> flattenStreams1(Map<?, List<E>> map) {
        return map.entrySet().stream().flatMap(entry -> entry.getValue().stream()).collect(Collectors.toList());
    }

    public static <E> List<E> flattenStreams2(Map<?, List<E>> map) {
        return map.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    public static <E> List<E> flattenStreams3(Map<?, List<E>> map) {
        return map.values().stream().collect(ArrayList::new, ArrayList::addAll, ArrayList::addAll);
    }

    public static <E> List<E> flattenStreams4(Map<?, List<E>> map) {
        return map.values().stream().collect(() -> new ArrayList<>(EXPECTED_NUM_WORDS), List::addAll, List::addAll);
    }

    public static <E> List<E> flattenStreams5(Map<?, List<E>> map) {
        return map.values().stream().collect(LinkedList::new, List::addAll, List::addAll);
    }
}
