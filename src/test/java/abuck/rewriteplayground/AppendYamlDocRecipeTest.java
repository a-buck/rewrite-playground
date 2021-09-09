package abuck.rewriteplayground;

import org.junit.Test;
import org.openrewrite.yaml.YamlParser;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class AppendYamlDocRecipeTest {

  @Test
  public void testAppend() throws IOException {

    final var yamlToAppend =
        "apiVersion: \"iam.cnrm.cloud.google.com/v1beta1\"\n" +
        "kind: \"IAMPartialPolicy\"\n" +
        "metadata:\n" +
        "  name: \"my-test-topic-iampartialpolicy\"\n" +
        "  namespace: \"my-namespace\"\n" +
        "spec:\n" +
        "  bindings:\n" +
        "  - members:\n" +
        "    - member: \"serviceAccount:mysa@my-namespace.iam.gserviceaccount.com\"\n" +
        "    role: \"roles/pubsub.publisher\"\n" +
        "  resourceRef:\n" +
        "    apiVersion: \"pubsub.cnrm.cloud.google.com/v1beta1\"\n" +
        "    external: \"projects/my-namespace/topics/my-test-topic\"\n" +
        "    kind: \"PubSubTopic\"";

    final var input = readContents("input.yaml");

    final var inputDocuments = new YamlParser().parse(input);

    final var results = new AppendYamlDocRecipe(yamlToAppend).run(inputDocuments);


    assertEquals(1, results.size());
    final var result = results.get(0);
    System.out.println("Diff:");
    System.out.println(result.diff());

    assertEquals(readContents("expected.yaml"), result.getAfter().print());
  }

  private static String readContents(final String fileName) throws IOException {
    return new String(Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName).readAllBytes());
  }

}
