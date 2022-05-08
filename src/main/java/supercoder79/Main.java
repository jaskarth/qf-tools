package supercoder79;

import java.io.File;
import java.nio.file.Files;

public class Main {
    public static void main(String[] args) throws Throwable {
        File inputDir = new File("input");
        inputDir.mkdir();

        File jarDir = new File("jars");
        jarDir.mkdir();

        File outputDir = new File("output");
        outputDir.mkdir();

        for (File file : jarDir.listFiles()) {
            File outdir = outputDir.toPath().resolve(file.getName().replace(".jar", "")).toFile();
            outdir.mkdir();

            String extra = "";
            // Terrible hardcoding to get around bugs that exist in these versions
            if (file.getName().equals("quiltflower-1.7.0.jar") || file.getName().equals("quiltflower-1.8.0.jar")) {
                extra = "--legacy-saving ";
            }

            Runtime.getRuntime().exec("java -jar " + file.getAbsolutePath() + " " + extra + inputDir.getAbsolutePath() + " " + outdir.getAbsolutePath());
        }
    }
}