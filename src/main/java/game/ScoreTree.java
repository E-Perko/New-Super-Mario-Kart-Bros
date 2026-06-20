package game;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ScoreTree {

    private ScoreNode root;

    public void insert(int score, int level) {
        root = insert(root, score, level);
    }

    private ScoreNode insert(ScoreNode node, int score, int level) {
        // TODO (Phase 4): Insert a new score into the BST recursively.
        //
        // Follow the insert() pattern from the Phase 4 guide, adapting it to
        // pass the level parameter when constructing a new ScoreNode.
        return node; // placeholder — replace this
    }

    // Reverse in-order (right → node → left) yields descending order
    public List<ScoreNode> getTopScores(int n) {
        List<ScoreNode> result = new ArrayList<>();
        collectDescending(root, result, n);
        return result;
    }

    private void collectDescending(ScoreNode node, List<ScoreNode> result, int n) {
        // TODO (Phase 4): Collect the top n scores in descending order.
        //
        // The Phase 4 guide shows printInOrder() which visits left → node → right.
        // Adapt that pattern to visit right → node → left instead (larger scores
        // live in the right subtree), and stop once result has n items.
    }

    public boolean isEmpty() {
        return root == null;
    }

    // -----------------------------------------------------------------------
    // Persistence — scores survive between sessions via a plain text file.
    // Each line: "<score> <level>"
    // -----------------------------------------------------------------------

    public void saveToFile(Path path) {
        // TODO (Phase 3): Write all scores to a file using BufferedWriter.
        //
        // 1. Build a List<String> and populate it by calling collectInOrder(root, lines)
        //    — look at that method to see how each entry is formatted.
        // 2. Open a BufferedWriter with Files.newBufferedWriter(path) inside a
        //    try-with-resources block and write each line followed by newLine().
        //    See the Phase 3 file I/O guide for the BufferedWriter pattern.
    }

    private void collectInOrder(ScoreNode node, List<String> lines) {
        if (node == null) return;
        collectInOrder(node.left, lines);
        lines.add(node.score + " " + node.level);
        collectInOrder(node.right, lines);
    }

    public void loadFromFile(Path path) {
        // TODO (Phase 3): Read scores back from the file written by saveToFile().
        //
        // 1. Return early if the file doesn't exist (Files.exists(path)).
        // 2. Read all lines with Files.readAllLines(path) — wrap in try/catch.
        // 3. Use a HashSet<String> to skip duplicate lines before inserting —
        //    see the Phase 3 file I/O guide for how add() detects duplicates.
        // 4. For each unique line, split on " ", parse the two integers,
        //    and call insert() to add the score to the tree.
    }
}
