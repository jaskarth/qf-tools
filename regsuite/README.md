# Regression Suite

This tool serves to automatically run Quiltflower patches against a huge amount of code, and compare it to older changesets.
The rationale is that we need a large corpus of non-unit tests, to ensure that well-meaning patches don't accidentally break things elsewhere.

A word of caution when using this: Decompiling the whole thing takes roughly 12 minutes on my Ryzen 5-4500U, and uses ~200MB of space for the input jars and ~600MB for the output java files.
You can choose what gets decompiled with the `dlmanifest.txt` file, to remove the more intensive decompilation targets.

Suggested workflow:
1. Make your changes to QF.
2. Run all the unit tests. This is the easiest way to see if a change breaks anything unintended, and takes less than a minute to complete on a modern system.
3. If the unit tests don't pass, return to step 1.
4. Publish to local maven (`gradlew publishToMavenLocal`).
5. Run the regression suite.
6. Check the output (`results/changes.diff`).
7. If the output doesn't have issues, return to step 1, but try to isolate a small unit test to show the issue for the future.
8. If there's no issues, feel free to make a PR!

Note that the first time you run it, no diff will be made. This is because it'll initialize the git repo with the first run as the fresh state.