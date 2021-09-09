package abuck.rewriteplayground;

import org.intellij.lang.annotations.Language;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.YamlParser;
import org.openrewrite.yaml.tree.Yaml;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Append YAML document to a file.
 */
public class AppendYamlDocRecipe extends Recipe {

  @Language("yml")
  private final String yamlToAppend;

  public AppendYamlDocRecipe(final String yamlToAppend) {
    this.yamlToAppend = yamlToAppend;
  }

  @Override
  public String getDisplayName() {
    return "append-yaml-doc";
  }

  @Override
  protected TreeVisitor<?, ExecutionContext> getVisitor() {
    return new YamlIsoVisitor<>() {

      @Override
      public Yaml.Documents visitDocuments(final Yaml.Documents documents, final ExecutionContext executionContext) {

        final var newDoc = new YamlParser().parse(yamlToAppend).get(0).getDocuments().get(0).withExplicit(true);

        // original documents + the new one
        final var combined = Stream.concat(documents.getDocuments().stream(), Stream.of(newDoc)).collect(Collectors.toUnmodifiableList());

        return super.visitDocuments(documents.withDocuments(combined), executionContext);
      }
    };
  }

}
