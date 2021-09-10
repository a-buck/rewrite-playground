package abuck.rewriteplayground;

import org.intellij.lang.annotations.Language;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.kubernetes.Kubernetes;
import org.openrewrite.kubernetes.KubernetesVisitor;
import org.openrewrite.yaml.tree.Yaml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Append YAML document to a file.
 */
public class AppendYamlDocRecipe extends Recipe {

  private static final Logger LOG = LoggerFactory.getLogger(AppendYamlDocRecipe.class);

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
    return new KubernetesVisitor<>() {

      @Override
      public Kubernetes.ResourceDocument visitKubernetes(Kubernetes.ResourceDocument resource, ExecutionContext executionContext) {
        System.out.println("AppendYamlDocRecipe.visitKubernetes");
        return super.visitKubernetes(resource, executionContext);
      }

      @Override
      public Yaml visitDocument(Yaml.Document document, ExecutionContext executionContext) {
        System.out.println("AppendYamlDocRecipe.visitDocument");
        return super.visitDocument(document, executionContext);
      }

      @Override
      public Yaml visitDocuments(final Yaml.Documents documents, final ExecutionContext executionContext) {
        System.out.println("AppendYamlDocRecipe.visitDocuments");
        Yaml.Documents yaml = (Yaml.Documents) super.visitDocuments(documents, executionContext);

        List<Kubernetes.ResourceDocument> policyMembers = new ArrayList<>();
        @SuppressWarnings("unchecked") List<Yaml.Document> resources = ListUtils.map(
            (List<Yaml.Document>) yaml.getDocuments(),
            doc -> {
              // TODO: How to convert doc to ResourceDocument?
              final Kubernetes.ResourceDocument resource;

              try {
                resource = (Kubernetes.ResourceDocument) doc;
              } catch (ClassCastException e) {
                // logging exception as it otherwise gets swallowed
                LOG.error("cannot cast to ResourceDocument", e);
                throw e;
              }

              // using the constructor also fails due to there being no KubernetesModel marker
//              final var resource = new Kubernetes.ResourceDocument(doc);



              if (resource.getModel().getKind().equals("IAMPolicyMember")) {
                policyMembers.add(resource);
                return null;
              }
              return doc;
            });

        final List<Yaml.Document> iamPartialPolicies = new ArrayList<>();
        // todo: build iamPartialPolicies

        return yaml.withDocuments(ListUtils.concatAll(resources, iamPartialPolicies));
      }
    };

  }

}
