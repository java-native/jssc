package jssc;

import jssc.junit.rules.DisplayMethodNameRule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static jssc.NativeIdent.isBlank;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * Checks the <code>JSSC-IDENT</code> ({@link NativeIdent}) of each native library under <code>natives/</code> on the
 * classpath:
 * <ol>
 *     <li>Every target in <code>jssc.expected.native.targets</code> (required, comma separated) has exactly one
 *     library with exactly one ident; there are no other directories.</li>
 *     <li>version is <code>jssc.expected.native.version</code>.</li>
 *     <li>target is the directory the library sits in.</li>
 *     <li>All share one rev (<code>jssc.expected.build.rev</code>, if set) and one origin
 *     (<code>jssc.expected.build.origin</code>, if set); no "-dirty" or
 *     "unknown" rev and no "origin=local".</li>
 *     <li>rev is an ancestor of HEAD and the native sources are unchanged since. Needs the git history back to
 *     rev.</li>
 * </ol>
 * Committed natives are usually stale between releases, so expect failures outside of a release.
 */
public class PrecompiledNativesTest extends DisplayMethodNameRule {

    private static final Logger LOG = LogManager.getLogger(PrecompiledNativesTest.class);

    // src/main/cpp holds the JNI header, so a JNI change counts too
    private static final String[] NATIVE_SOURCES = {"src/main/cpp", "toolchain", "CMakeLists.txt"};

    // The targets the caller expects, each a directory under natives/
    private static final Set<String> targets = new TreeSet<String>();
    private static File nativesDir;
    // The idents of the targets whose library could be read; the others are in unreadable
    private static final Map<String, Map<String, String>> idents = new TreeMap<String, Map<String, String>>();
    private static final List<String> unreadable = new ArrayList<String>();

    @BeforeClass
    public static void readIdents() throws Exception {
        final String expectedTargets = System.getProperty("jssc.expected.native.targets");
        assertTrue("jssc.expected.native.targets is not set: name the targets to check, e.g."
                + " -Djssc.native.targets=linux_64,osx_arm64", !isBlank(expectedTargets));
        targets.addAll(Arrays.asList(expectedTargets.trim().split("\\s*,\\s*")));
        LOG.info("Targets: {}", targets);
        final URL natives = PrecompiledNativesTest.class.getClassLoader().getResource("natives");
        assertNotNull("No natives on the classpath", natives);
        assertEquals("Natives on the classpath are not a directory: " + natives, "file", natives.getProtocol());
        nativesDir = new File(natives.toURI());
        for (String target : targets) {
            final File[] files = new File(nativesDir, target).listFiles();
            if (files == null || files.length == 0) {
                unreadable.add(target + ": no native library in " + new File(nativesDir, target));
            } else if (files.length != 1) {
                unreadable.add(target + ": expected one library, found " + Arrays.toString(files));
            } else if (!NativeIdent.LIBRARY_NAMES.contains(files[0].getName())) {
                unreadable.add(target + ": " + files[0] + " is not one of " + NativeIdent.LIBRARY_NAMES);
            } else {
                try {
                    idents.put(target, NativeIdent.parse(files[0]));
                } catch (AssertionError e) {
                    unreadable.add(target + ": " + e.getMessage());
                }
            }
        }
        for (Map.Entry<String, Map<String, String>> entry : idents.entrySet()) {
            LOG.info("{}: {}", entry.getKey(), entry.getValue());
        }
    }

    @Test
    public void everyTargetHasOneLibraryWithOneIdent() {
        assertNoProblems(unreadable);
    }

    @Test
    public void onlyTheTargetsAreShipped() {
        final List<String> problems = new ArrayList<String>();
        for (File dir : nativesDir.listFiles()) {
            if (!targets.contains(dir.getName())) {
                problems.add(dir + " is not one of the targets " + targets);
            }
        }
        assertNoProblems(problems);
    }

    @Test
    public void versionIsTheNativeVersion() {
        final String expected = System.getProperty("jssc.expected.native.version");
        assumeTrue("jssc.expected.native.version is not set; skipping", !isBlank(expected));
        final List<String> problems = new ArrayList<String>();
        for (Map.Entry<String, Map<String, String>> entry : idents.entrySet()) {
            final String version = entry.getValue().get("version");
            if (!expected.equals(version)) {
                problems.add(entry.getKey() + ": version is " + version + ", expected " + expected);
            }
        }
        assertNoProblems(problems);
    }

    @Test
    public void targetIsTheDirectory() {
        final List<String> problems = new ArrayList<String>();
        for (Map.Entry<String, Map<String, String>> entry : idents.entrySet()) {
            final String target = entry.getValue().get("target");
            if (!entry.getKey().equals(target)) {
                problems.add(entry.getKey() + ": ident says target=" + target);
            }
        }
        assertNoProblems(problems);
    }

    @Test
    public void allComeFromOneCiRun() {
        final String expectedRev = System.getProperty("jssc.expected.build.rev");
        final String expectedOrigin = System.getProperty("jssc.expected.build.origin");
        final List<String> problems = new ArrayList<String>();
        for (Map.Entry<String, Map<String, String>> entry : idents.entrySet()) {
            final String target = entry.getKey();
            final String rev = entry.getValue().get("rev");
            final String origin = entry.getValue().get("origin");
            if (!isTraceable(rev)) {
                problems.add(target + ": rev=" + rev + " is not a commit");
            } else if (!isBlank(expectedRev) && !expectedRev.equals(rev)) {
                problems.add(target + ": rev is " + rev + ", expected " + expectedRev);
            }
            if (origin == null || origin.equals("local")) {
                problems.add(target + ": origin=" + origin);
            } else if (!isBlank(expectedOrigin) && !expectedOrigin.equals(origin)) {
                problems.add(target + ": origin is " + origin + ", expected " + expectedOrigin);
            }
        }
        final Set<String> revs = distinct("rev");
        if (revs.size() > 1) {
            problems.add("natives come from different revisions: " + revs);
        }
        final Set<String> origins = distinct("origin");
        if (origins.size() > 1) {
            problems.add("natives come from different origins: " + origins);
        }
        assertNoProblems(problems);
    }

    @Test
    public void nativeSourcesAreUnchangedSinceRev() throws Exception {
        assertNotNull("Cannot read the git history: not a git checkout, or git is missing", git("rev-parse", "HEAD"));
        final List<String> problems = new ArrayList<String>();
        for (String rev : distinct("rev")) {
            if (!isTraceable(rev)) {
                continue; // see allComeFromOneCiRun
            }
            if (git("rev-parse", "-q", "--verify", rev + "^{commit}") == null) {
                problems.add("rev " + rev + " is not in the git history (shallow clone?)");
            } else if (git("merge-base", "--is-ancestor", rev, "HEAD") == null) {
                problems.add("natives were built from " + rev + ", which is not part of this branch");
            } else {
                final List<String> diff = new ArrayList<String>(Arrays.asList("diff", "--name-only", rev, "HEAD", "--"));
                diff.addAll(Arrays.asList(NATIVE_SOURCES));
                final String changed = git(diff.toArray(new String[0]));
                assertNotNull("git diff failed", changed);
                if (!changed.isEmpty()) {
                    problems.add("native sources changed since " + rev + ": "
                            + changed.replace('\n', ' '));
                }
            }
        }
        assertNoProblems(problems);
    }

    private static boolean isTraceable(String rev) {
        return rev != null && rev.matches("[0-9a-f]{4,40}");
    }

    private static Set<String> distinct(String key) {
        final Set<String> values = new TreeSet<String>();
        for (Map<String, String> ident : idents.values()) {
            values.add(String.valueOf(ident.get(key)));
        }
        return values;
    }

    /**
     * Runs git in the working directory
     *
     * @return its trimmed output, or null if it failed
     */
    private static String git(String... args) throws IOException, InterruptedException {
        final List<String> command = new ArrayList<String>();
        command.add("git");
        command.addAll(Arrays.asList(args));
        final Process process = new ProcessBuilder(command).start();
        process.getOutputStream().close();
        final InputStream in = process.getInputStream();
        final String output;
        try {
            output = new String(NativeIdent.readFully(in), "UTF-8").trim();
        } finally {
            in.close();
        }
        return process.waitFor() == 0 ? output : null;
    }

    private static void assertNoProblems(Collection<String> problems) {
        final StringBuilder message = new StringBuilder(problems.size() + " problem(s) with the precompiled natives:");
        for (String problem : problems) {
            message.append("\n  ").append(problem);
        }
        assertTrue(message.toString(), problems.isEmpty());
    }
}
