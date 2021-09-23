package abuck.rewriteplayground;

import java.io.IOException;
import java.util.stream.Collectors;
import org.junit.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.Markers;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.YamlParser;
import org.openrewrite.yaml.tree.Yaml;

public class ReadCommentsRecipeTest {

  @Test
  public void test1() throws IOException {
    final var input = readContents("input.yaml");

    final var inputDocuments = new YamlParser().parse(input);

    ExecutionContext ctx =
        new InMemoryExecutionContext(
            t -> {
              throw new RuntimeException(t);
            });
    new ReadCommentsRecipe().doNext(new PrintCommentMarkersRecipe()).run(inputDocuments, ctx);
  }

  private static String readContents(final String fileName) throws IOException {
    return new String(
        Thread.currentThread()
            .getContextClassLoader()
            .getResourceAsStream(fileName)
            .readAllBytes());
  }

  private static class PrintCommentMarkersRecipe extends Recipe {

    @Override
    public String getDisplayName() {
      return "print-comment-markers";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
      return new YamlIsoVisitor<>() {

        @Override
        public Yaml.Mapping.Entry visitMappingEntry(
            Yaml.Mapping.Entry entry, ExecutionContext executionContext) {
          printMarkers(entry.getMarkers());
          return super.visitMappingEntry(entry, executionContext);
        }

        @Override
        public Yaml.Sequence.Entry visitSequenceEntry(
            Yaml.Sequence.Entry entry, ExecutionContext executionContext) {
          printMarkers(entry.getMarkers());
          return super.visitSequenceEntry(entry, executionContext);
        }

        private void printMarkers(final Markers markers) {
          final var commentMarkers =
              markers.getMarkers().stream()
                  .filter(m -> m instanceof ReadCommentsRecipe.Comment)
                  .collect(Collectors.toUnmodifiableList());

          final var path = getPath();

          System.out.println(path);
          commentMarkers.forEach(
              c -> {
                final var comment = (ReadCommentsRecipe.Comment) c;
                System.out.println(
                    "above line: "
                        + comment.getAboveLine()
                        + "\nsame line: "
                        + comment.getSameLine());
              });
          System.out.println("------");
        }

        // copied from rewrite-kubernetes
        private String getPath() {
          return "/"
              + getCursor()
                  .getPathAsStream()
                  .filter(p -> p instanceof Yaml.Mapping.Entry)
                  .map(Yaml.Mapping.Entry.class::cast)
                  .map(e -> e.getKey().getValue())
                  .reduce("", (a, b) -> b + (a.isEmpty() ? "" : "/" + a));
        }
      };
    }
  }
}
