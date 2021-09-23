package abuck.rewriteplayground;

import java.util.*;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.tree.Yaml;

/**
 * Make comments more easily queryable by parsing the output of getPrefix for every entry and saving
 * the above line comments and same line comments as a marker.
 */
public class ReadCommentsRecipe extends Recipe {

  @Override
  public String getDisplayName() {
    return "read-comments";
  }

  @Override
  protected TreeVisitor<?, ExecutionContext> getVisitor() {
    return new ReadCommentsVisitor();
  }

  static class Comment implements Marker {
    private final String aboveLine;
    private final String sameLine;

    public Comment(final String sameLine, final String aboveLine) {
      this.sameLine = sameLine;
      this.aboveLine = aboveLine;
    }

    @Override
    public UUID getId() {
      return UUID.randomUUID();
    }

    public String getSameLine() {
      return sameLine;
    }

    public String getAboveLine() {
      return aboveLine;
    }
  }

  /** Parses comments using each yaml entry prefix, and adds the comments as markers */
  private static class ReadCommentsVisitor extends YamlIsoVisitor<ExecutionContext> {

    @Override
    public Yaml.Document visitDocument(
        final Yaml.Document document, final ExecutionContext executionContext) {

      final var flattened = flatten(document);

      for (int i = 0; i < flattened.size() - 1; i++) {
        final var curr = flattened.get(i);
        final var next = flattened.get(i + 1);

        final var currAboveLineComment = getAfterFirstNewLine(curr.getPrefix());
        final var currSameLineComment = getUpToFirstNewLine(next.getPrefix());

        doAfterVisit(new AddCommentMarkerVisitor(curr, currSameLineComment, currAboveLineComment));
      }

      if (!flattened.isEmpty()) {
        final var lastYaml = flattened.get(flattened.size() - 1);
        final var documentEndPrefix = document.getEnd().getPrefix();
        final var lastSameLineComment = getUpToFirstNewLine(documentEndPrefix);
        final var aboveLineComment = getAfterFirstNewLine(lastYaml.getPrefix());

        doAfterVisit(new AddCommentMarkerVisitor(lastYaml, lastSameLineComment, aboveLineComment));
      }

      return super.visitDocument(document, executionContext);
    }

    private String getAfterFirstNewLine(final String s) { // above comment
      final var firstNewLine = s.indexOf("\n");
      if (firstNewLine < 0) {
        return "";
      }

      return s.substring(firstNewLine + 1).trim();
    }

    private String getUpToFirstNewLine(final String s) { // get previous same line
      final var firstNewLine = s.indexOf("\n");

      if (firstNewLine < 0) {
        return "";
      }
      return s.substring(0, firstNewLine).trim();
    }

    private List<Yaml> flatten(final Yaml.Document document) {
      final ArrayDeque<Yaml> flattenedYamls = new ArrayDeque<>();

      final Deque<Yaml> q = new ArrayDeque<>();
      q.push(document.getBlock());

      while (!q.isEmpty()) {
        final var curr = q.pop();
        if (curr instanceof Yaml.Mapping) {
          final var entries = ((Yaml.Mapping) curr).getEntries();
          q.addAll(entries);
        } else if (curr instanceof Yaml.Sequence) {
          final var entriesCopy = new ArrayList<>(((Yaml.Sequence) curr).getEntries());
          Collections.reverse(entriesCopy);
          entriesCopy.forEach(q::push);
        } else if (curr instanceof Yaml.Mapping.Entry) {
          final var entry = (Yaml.Mapping.Entry) curr;
          q.push(entry.getValue());
          flattenedYamls.push(curr);
        } else if (curr instanceof Yaml.Sequence.Entry) {
          final var entry = (Yaml.Mapping.Sequence.Entry) curr;
          q.push(entry.getBlock());
          flattenedYamls.push(curr);
        }
      }

      final var yamls = new ArrayList<>(flattenedYamls);
      Collections.reverse(yamls);
      return yamls;
    }
  }

  /** Adds a comment marker to the target yaml */
  private static class AddCommentMarkerVisitor extends YamlIsoVisitor<ExecutionContext> {

    private final Yaml target;
    private final String sameLineComment;
    private final String aboveLineComment;

    public AddCommentMarkerVisitor(
        final Yaml target, final String sameLineComment, String aboveLineComment) {
      this.target = target;
      this.sameLineComment = sameLineComment;
      this.aboveLineComment = aboveLineComment;
    }

    @Override
    public Yaml.Mapping.Entry visitMappingEntry(
        final Yaml.Mapping.Entry e, final ExecutionContext executionContext) {
      final var entry = super.visitMappingEntry(e, executionContext);

      if (e.isScope(target)) {
        final var markers = addCommentMarker(e.getMarkers());
        return entry.withMarkers(markers);
      }
      return entry;
    }

    private Markers addCommentMarker(final Markers existing) {
      return existing.computeByType(new Comment(sameLineComment, aboveLineComment), (a, b) -> b);
    }

    @Override
    public Yaml.Sequence.Entry visitSequenceEntry(
        final Yaml.Sequence.Entry e, final ExecutionContext executionContext) {
      final var entry = super.visitSequenceEntry(e, executionContext);
      if (entry.isScope(target)) {
        final var markers = addCommentMarker(e.getMarkers());
        return entry.withMarkers(markers);
      }

      return entry;
    }
  }
}
