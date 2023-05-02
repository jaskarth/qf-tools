package jaskarth.regsuite;

import org.jetbrains.java.decompiler.main.Fernflower;
import org.jetbrains.java.decompiler.main.decompiler.DirectoryResultSaver;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestRegressions {
    public static void main(String[] args) throws Throwable {
        Path manifest = Path.of("dlmanifest.txt");
        if (!manifest.toFile().exists()) {
            System.out.println("No download manifest found- running out of wrong directory?");
            return;
        }

        Path input = Path.of("input");
        Path results = Path.of("results");
        input.toFile().mkdir();
        results.toFile().mkdir();

        // Check what jars we already have
        List<String> existing = new ArrayList<>();
        Files.walk(input).forEach(p -> existing.add(p.getFileName().toString()));

        // Download jars from manifest
        List<String> manifestLines = Files.readAllLines(manifest);
        for (String line : manifestLines) {
            String fileName = line.substring(line.lastIndexOf('/') + 1);

            if (existing.stream().anyMatch(s -> s.contains(fileName))) {
                continue;
            }

            System.out.println("Downloading " + fileName);

            URL website = new URL(line);
            ReadableByteChannel rbc = Channels.newChannel(website.openStream());
            FileOutputStream fos = new FileOutputStream("input/" + fileName);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        }

        // Initialize output directory
        boolean first = false;
        Path output = Path.of("output");
        if (!output.toFile().exists()) {
            output.toFile().mkdir();
            first = true;
            System.out.println(output.toFile().exists());

            new ProcessBuilder("git", "init")
                    .directory(output.toFile())
                    .inheritIO().start().waitFor();
        }

        // Run decompiler on jars

        Map<String, Object> props = new HashMap<>(IFernflowerPreferences.DEFAULTS);
        props.put(IFernflowerPreferences.DECOMPILE_GENERIC_SIGNATURES, "1");
        props.put(IFernflowerPreferences.REMOVE_SYNTHETIC, "1");
        props.put(IFernflowerPreferences.INCLUDE_ENTIRE_CLASSPATH, "1");
        props.put(IFernflowerPreferences.PATTERN_MATCHING, "1");
        props.put(IFernflowerPreferences.TERNARY_CONDITIONS, "1");
        props.put(IFernflowerPreferences.THREADS, String.valueOf(Runtime.getRuntime().availableProcessors() / 2));

        int i = 0;
        for (String line : manifestLines) {
            String fileName = line.substring(line.lastIndexOf('/') + 1);
            String name = fileName.replace(".jar", "");
            output.resolve(name).toFile().mkdir();
            for (File file : output.resolve(name).toFile().listFiles()) {
                file.delete();
            }

            System.out.println("Decompiling " + fileName + " (" + ((++i) / (double) manifestLines.size()) + "%)");

            Fernflower quiltflower = new Fernflower(
                    new DirectoryResultSaver(output.resolve(name).toFile()),
                    props,
                    new IFernflowerLogger() {
                        @Override
                        public void writeMessage(String s, Severity severity) {
                            if (severity.ordinal() >= Severity.WARN.ordinal()) {
                                System.out.println(s);
                            }
                        }

                        @Override
                        public void writeMessage(String s, Severity severity, Throwable throwable) {
                            System.err.println(s);
                            throwable.printStackTrace(System.err);
                        }
                    }
            );

            quiltflower.addSource(input.resolve(fileName).toFile());
            quiltflower.decompileContext();
        }

        if (first) {
            new ProcessBuilder("git", "config", "core.longpaths", "true")
                    .directory(output.toFile())
                    .inheritIO().start().waitFor();
            new ProcessBuilder("git", "add", ".")
                    .directory(output.toFile())
                    .inheritIO().start().waitFor();
            new ProcessBuilder("git", "commit", "-m", "Initial")
                    .directory(output.toFile())
                    .inheritIO().start().waitFor();
        } else {
            new ProcessBuilder("git", "add", ".")
                    .directory(output.toFile())
                    .inheritIO().start().waitFor();

            new ProcessBuilder("git", "diff", "HEAD")
                    .directory(output.toFile())
                    .inheritIO().redirectOutput(new File("results/changes.diff"))
                    .start().waitFor();
        }
    }
}
