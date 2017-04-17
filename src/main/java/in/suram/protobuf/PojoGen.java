package in.suram.protobuf;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.List;

import in.suram.protobuf.CompiledProtos.Field;
import in.suram.protobuf.CompiledProtos.FieldRule;
import in.suram.protobuf.CompiledProtos.Message;

import static java.lang.String.format;

public class PojoGen {
  public static class Config {
    String pkg;
  }

  public static String Generate(Message message, Config config)
      throws IOException, FormatterException {

    PojoGenSBWrapper pojoBuilder =
        PojoGenSBWrapper.create()
            .startClass("public static", "Builder")
            .appendFieldDeclarations(message, "private", true)
            .appendGettersAndSetters(message)
            .appendBuildFunction(message)
            .closeClass();

    PojoGenSBWrapper pojo =
        PojoGenSBWrapper.create()
            .setPackage(config.pkg)
            .addImport("com.google.common.base.MoreObjects")
            .addImport("com.google.common.collect.ImmutableList")
            .startClass("public", message.name)
            .appendFieldDeclarations(message, "public final", false)
            .appendConstructor(message)
            .appendToStringFunction(message)
            .append(pojoBuilder)
            .closeClass();

    System.out.println(pojo.toString());
    return new Formatter().formatSource(pojo.toString());
  }

  public static void main(String[] args) throws IOException, FormatterException {

    System.out.println(IOUtils.toString(Utils.ProjectResourceToStream("/example.proto")));
    List<Message> messages =
        ProtoDefinitionCompiler.Compile(Utils.ProjectResourceToStream("/example.proto"));
    Config config = new Config();
    config.pkg = "in.suram.protobuf";
    for (Message message : messages) {
      System.out.printf("name=%s\n", message.name);
      for (Field field : message.fields) {
        System.out.printf(
            "Field = `%s` type = `%s` rule = `%s` id = `%s`\n",
            field.name, field.type, field.fieldRule, field.id);
      }
      System.out.println("\n==Generated Code==\n");
      System.out.println(Generate(message, config));
    }
  }

  private static class PojoGenSBWrapper {
    private final StringBuilder sb = new StringBuilder();

    private static PojoGenSBWrapper create() {
      return new PojoGenSBWrapper();
    }

    @Override
    public String toString() {
      return sb.toString();
    }

    private PojoGenSBWrapper append(String s) {
      sb.append(s);
      return this;
    }

    private PojoGenSBWrapper append(PojoGenSBWrapper s) {
      sb.append(s.sb);
      return this;
    }

    private PojoGenSBWrapper appendToStringFunction(Message message) {
      // |  private String _string_rep;
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
      sb.append("private String _string_rep;\n");
      sb.append("private String GenerateStringRep() {").append("\n");
      sb.append("return MoreObjects.toStringHelper(this).").append("\n");
      for (Field field : message.fields) {
        String addFieldToString = format("add(\"%s\", %s).", field.javaName, field.javaName);
        sb.append(addFieldToString).append("\n");
      }

      sb.append("toString();").append("\n");
      sb.append("}").append("\n");
      sb.append("@Override").append("\n");
      sb.append("public String toString() {").append("\n");
      String setStringRepIfNull = "if (_string_rep == null) { _string_rep = GenerateStringRep(); }";
      sb.append(setStringRepIfNull).append("\n");
      sb.append("return _string_rep;").append("\n");
      sb.append("}").append("\n");
      return this;
    }

    private PojoGenSBWrapper appendConstructor(Message message) {
      // |private ${class_name}(
      // |           ${field_type1} ${field_name1},
      // |           ${field_type1} ${field_name1}){
      // |  this.${field_name1} = ${field_name1};
      // |  this.${field_name2} = ${field_name2};
      // |}
      sb.append("private ").append(message.name).append("(");
      for (Field field : message.fields) {
        sb.append(field.javaType()).append(" ").append(field.javaName).append(",");
      }
      Utils.deleteLastX(sb, 1).append(") {");
      for (Field field : message.fields) {
        String name = field.javaName;
        sb.append(format("this.%s = %s;", name, name));
      }
      sb.append("}");
      sb.append("\n");
      return this;
    }

    private PojoGenSBWrapper appendFieldDeclarations(
        Message message, String fieldModifiers, boolean isBuilder) {
      for (Field field : message.fields) {
        String type = isBuilder ? field.javaBuilderType() : field.javaType();
        String name = field.javaName;
        // {fieldModifier} {fieldType} {fieldType};
        // E.g private static int id;
        sb.append(fieldModifiers).append(" ").append(type).append(" ").append(name).append(";\n");
      }
      return this;
    }

    private PojoGenSBWrapper appendGettersAndSetters(Message message) {
      // pojoBuilder setters and getters.
      // Getter
      // public {type} {getterName}() { return this.{name}; }
      // Setter
      // public Builder {setterName}({type} {name}) { this.{name} = {name}; {return this; }
      for (Field field : message.fields) {
        String getter =
            format(
                "public %s %s() { return this.%s; }",
                field.javaBuilderType(), field.javaGetterName, field.javaName);
        String setter =
            format(
                "public Builder %s(%s %s) { this.%s = %s; return this; }",
                field.javaSetterName,
                field.javaBuilderType(),
                field.javaName,
                field.javaName,
                field.javaName);
        sb.append(getter).append("\n");
        sb.append(setter).append("\n");
      }
      return this;
    }

    private PojoGenSBWrapper appendBuildFunction(Message message) {
      // |public ${class_name} build() {
      // |  return new ${class_name}(
      // |               ${field_name1},
      // |               ${field_name2}
      // |             );
      // |}
      sb.append(format("public %s build() {", message.name)).append("\n");
      sb.append(format("return new %s(", message.name)).append("\n");
      for (Field field : message.fields) {
        String constructorParam = field.javaName;
        if (field.fieldRule == FieldRule.repeated) {
          constructorParam += ".build()";
        }
        sb.append(constructorParam).append(",").append("\n");
      }
      Utils.deleteLastX(sb, 2).append(");").append("\n");
      sb.append("}").append("\n");
      return this;
    }

    private PojoGenSBWrapper startClass(String classModifier, String name) {
      // {classModifier} class {name} {
      sb.append(classModifier).append(" class ").append(name).append(" {\n");
      return this;
    }

    private PojoGenSBWrapper closeClass() {
      // }
      sb.append("}\n");
      return this;
    }

    private PojoGenSBWrapper setPackage(String pkg) {
      // package {pkg};
      sb.append("package ").append(pkg).append(";\n");
      return this;
    }

    private PojoGenSBWrapper addImport(String pkg) {
      // import {pkg};
      sb.append("import ").append(pkg).append(";\n");
      return this;
    }
  }
}
