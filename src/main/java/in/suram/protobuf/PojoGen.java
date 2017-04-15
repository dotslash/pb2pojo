package in.suram.protobuf;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.List;

import static java.lang.String.format;

public class PojoGen {
  public static class Config {
    String pkg;
  }

  public static String Generate(Message message, Config config)
      throws IOException, FormatterException {

    // ==pojoBuilder==
    // |  public class Builder {;
    // |    private ${field_type1} ${field_name1};
    // |    private ${field_type2} ${field_name2};
    // |${pojoGettersSetters}
    // |    public ${class_name} build() {
    // |      return new ${class_name}(
    // |                   ${field_name1},
    // |                   ${field_name2}
    // |                 );
    // |    }
    // |  }

    // ==pojo==
    // |public class ${class_name} {;
    // |  private String _string_rep;
    // |  public final ${field_type1} ${field_name1};
    // |  public final ${field_type2} ${field_name2};
    // |  private ${class_name}(
    // |             ${field_type1} ${field_name1},
    // |             ${field_type1} ${field_name1}){
    // |    this.${field_name1} = ${field_name1};
    // |    this.${field_name2} = ${field_name2};
    // |  }
    // |${toString}
    // |${pojoBuilder}
    // |}

    // ==toString==
    // |  private String GenerateStringRep() {
    // |     return MoreObjects.toStringHelper(this).
    // |              .add("field1", field1)
    // |              .add("field2", field2)
    // |              .toString();
    // |  }
    // |  @Override
    // |  public String toString() {
    // |    if (_string_rep != null) { return _string_rep; }
    // |    _string_rep = GenerateStringRep();
    // |  }

    StringBuilder pojoBuilder = new StringBuilder();

    pojoBuilder.append("public static class Builder").append("{\n");
    appendFieldDeclarations(pojoBuilder, message, "private");
    gettersAndSetters(message, pojoBuilder);
    buildFunction(message, pojoBuilder);
    pojoBuilder.append("}").append("\n");

    StringBuilder pojo = new StringBuilder();
    pojo.append("public class ").append(message.name).append("{\n");
    appendFieldDeclarations(pojo, message, "public final");
    appendConstructor(message, pojo);
    appendToStringFunction(message, pojo);
    pojo.append(pojoBuilder);
    pojo.append("}").append("\n");
    // |package ${pkg};
    // |${pojo}
    String packageStr = "package " + config.pkg + ";\n";
    String importStr = "import com.google.common.base.MoreObjects; " + ";\n";
    String sourceString = packageStr + importStr + pojo;
    System.out.println(sourceString);
    return new Formatter().formatSource(sourceString);
  }

  private static StringBuilder gettersAndSetters(Message message, StringBuilder pojoBuilder) {
    // pojoBuilder setters and getters.
    for (Message.Field field : message.fields) {
      String type = field.type.javaType;
      String name = Utils.lowerCamel(field.name);
      String getter =
          format("public %s %s() { return this.%s; }", type, field.javaGetterName, name);
      String setter =
          format(
              "public Builder %s(%s %s) { this.%s = %s; return this; }",
              field.javaSetterName, type, name, name, name);
      pojoBuilder.append(getter).append("\n");
      pojoBuilder.append(setter).append("\n");
    }
    return pojoBuilder;
  }

  private static StringBuilder buildFunction(Message message, StringBuilder pojoBuilder) {
    pojoBuilder.append(format("public %s build() {", message.name)).append("\n");
    pojoBuilder.append(format("return new %s(", message.name)).append("\n");
    for (Message.Field field : message.fields) {
      pojoBuilder.append(field.name).append(",").append("\n");
    }
    Utils.deleteLastX(pojoBuilder, 2).append(");").append("\n");
    pojoBuilder.append("}").append("\n");
    return pojoBuilder;
  }

  private static StringBuilder appendFieldDeclarations(
      StringBuilder toWrite, Message message, String fieldModifiers) {
    for (Message.Field field : message.fields) {
      // {fieldModifier} {fieldType} {fieldType};
      // E.g private static int id;
      toWrite
          .append(fieldModifiers)
          .append(" ")
          .append(field.type.javaType)
          .append(" ")
          .append(field.javaName)
          .append(";\n");
    }
    return toWrite;
  }

  private static StringBuilder appendToStringFunction(Message message, StringBuilder pojo) {
    pojo.append("private String _string_rep;\n");
    pojo.append("private String GenerateStringRep() {").append("\n");
    pojo.append("return MoreObjects.toStringHelper(this).").append("\n");
    for (Message.Field field : message.fields) {
      String addFieldToString = format("add(\"%s\", %s).", field.javaName, field.javaName);
      pojo.append(addFieldToString).append("\n");
    }

    pojo.append("toString();").append("\n");
    pojo.append("}").append("\n");
    pojo.append("@Override").append("\n");
    pojo.append("public String toString() {").append("\n");
    String setStringRepIfNull = "if (_string_rep == null) { _string_rep = GenerateStringRep(); }";
    pojo.append(setStringRepIfNull).append("\n");
    pojo.append("return _string_rep;").append("\n");
    pojo.append("}").append("\n");
    return pojo;
  }

  private static StringBuilder appendConstructor(Message message, StringBuilder pojo) {
    pojo.append("private ").append(message.name).append("(");
    for (Message.Field field : message.fields) {
      pojo.append(field.type.javaType).append(" ").append(field.javaName).append(",");
    }
    Utils.deleteLastX(pojo, 1).append(") {");
    for (Message.Field field : message.fields) {
      String name = field.javaName;
      pojo.append(format("this.%s = %s;", name, name));
    }
    pojo.append("}");
    pojo.append("\n");
    return pojo;
  }

  public static void main(String[] args) throws IOException, FormatterException {

    System.out.println(IOUtils.toString(Utils.ProjectResourceToStream("/example.proto")));
    List<Message> messages =
        ProtoDefinitionCompiler.Compile(Utils.ProjectResourceToStream("/example.proto"));
    Config config = new Config();
    config.pkg = "in.suram.protobuf";
    for (Message message : messages) {
      System.out.printf("name=%s\n", message.name);
      for (Message.Field field : message.fields) {
        System.out.printf(
            "Field = `%s` type = `%s` rule = `%s` id = `%s`\n",
            field.name, field.type, field.fieldRule, field.id);
      }
      System.out.println("\n==Generated Code==\n");
      System.out.println(Generate(message, config));
    }
  }
}
