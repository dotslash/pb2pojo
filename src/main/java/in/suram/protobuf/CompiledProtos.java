package in.suram.protobuf;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@SuppressWarnings("WeakerAccess")
public class CompiledProtos {
  private enum Primitive {
    INT64("int64", "long"),
    INT32("int32", "int"),
    DOUBLE("double"),
    FLOAT("float"),
    BOOL("bool", "boolean"),
    BYTES("bytes", "byte[]"),
    STRING("string", "String"),
    NONE("none");

    private static final ImmutableMap<String, Primitive> nameToType;

    static {
      ImmutableMap.Builder<String, Primitive> builder = ImmutableMap.builder();
      for (Primitive type : Primitive.values()) {
        builder.put(type.name, type);
      }
      nameToType = builder.build();
    }

    public final String name;
    private final String javaType;

    Primitive(String name) {
      this(name, name);
    }

    Primitive(String name, String javaType) {
      this.name = name;
      this.javaType = javaType;
    }

    public static Primitive fromName(String name) {
      return MoreObjects.firstNonNull(nameToType.get(name), NONE);
    }

    @Override
    public String toString() {
      return name;
    }
  }

  public static class Type {
    public final String javaType;
    public final Primitive primitive;

    private Type(String javaType, Primitive primitive) {
      this.javaType = javaType;
      this.primitive = primitive;
    }

    public static Type make(String type) {
      Primitive primitive = Primitive.fromName(type);
      String javaType = type;
      if (primitive != Primitive.NONE) {
        javaType = primitive.javaType;
      }
      return new Type(javaType, primitive);
    }
  }

  public enum FieldRule {
    optional,
    required,
    repeated,
    unknown;

    public static FieldRule fromName(String name) {
      switch (name) {
        case "optional":
          return optional;
        case "required":
          return required;
        case "repeated":
          return repeated;
        default:
          return unknown;
      }
    }
  }

  public static class Field {

    public final FieldRule fieldRule;
    public final Type type;
    public final String name;
    public final String javaName;
    public final String javaGetterName;
    public final String javaSetterName;
    public final int id;

    public Field(FieldRule fieldRule, Type type, String name, int id) {
      this.fieldRule = fieldRule;
      this.type = type;
      this.name = name;
      this.id = id;

      String fieldNamePrefix = "";
      if (fieldRule == FieldRule.repeated) {
        fieldNamePrefix = "_list";
      }

      this.javaName = Utils.lowerCamel(name + fieldNamePrefix);
      this.javaGetterName = Utils.lowerCamel("get_" + name + fieldNamePrefix);
      this.javaSetterName = Utils.lowerCamel("set_" + name + fieldNamePrefix);
    }

    public String javaType() {
      return fieldRule == FieldRule.repeated
          ? String.format("ImmutableList<%s>", type.javaType)
          : type.javaType;
    }

    public String javaBuilderType() {
      return fieldRule == FieldRule.repeated
          ? String.format("ImmutableList.Builder<%s>", type.javaType)
          : type.javaType;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("fieldRule", fieldRule)
          .add("type", type)
          .add("name", name)
          .add("id", id)
          .toString();
    }
  }

  public static class Message {
    public final String name;
    public final boolean isValid;
    public final Collection<Field> fields;

    private final Map<String, Field> nameToFields;
    private final Map<Integer, Field> idToFields;

    public Message(String name, Collection<Field> fields) {
      Map<Integer, Field> idToFields = Maps.newHashMap();
      Map<String, Field> nameToFields = Maps.newHashMap();
      boolean isValid = true;

      for (Field field : fields) {
        boolean isNewId = idToFields.put(field.id, field) == null;
        boolean isNewName = nameToFields.put(field.name, field) == null;
        isValid = isValid && !isNewId && !isNewName;
      }
      this.idToFields = idToFields;
      this.nameToFields = nameToFields;
      this.name = name;
      this.isValid = isValid;
      this.fields = Collections.unmodifiableCollection(nameToFields.values());
    }

    public Field field(int id) {
      return idToFields.get(id);
    }

    public Field field(String name) {
      return nameToFields.get(name);
    }
  }
}
