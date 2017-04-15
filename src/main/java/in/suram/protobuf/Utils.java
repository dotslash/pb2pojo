package in.suram.protobuf;

import com.google.common.base.CaseFormat;

import java.io.InputStream;

public class Utils {

  public static InputStream ProjectResourceToStream(String resource) {
    return ProtoDefinitionCompiler.class.getResourceAsStream(resource);
  }

  public static String upperCamel(String name) {
    return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name);
  }

  public static String lowerCamel(String name) {
    return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name);
  }

  public static StringBuilder deleteLastX(StringBuilder pojoBuilder, int x) {
    return pojoBuilder.delete(pojoBuilder.length() - x, pojoBuilder.length());
  }
}
