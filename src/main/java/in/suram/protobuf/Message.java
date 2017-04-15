package in.suram.protobuf;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@SuppressWarnings("WeakerAccess")
public class Message {

  public enum Type {
    INT64("int64", "long"),
    INT32("int32", "int"),
    DOUBLE("double"),
    FLOAT("float"),
    BOOL("bool", "boolean"),
    BYTES("bytes", "byte[]"),
    STRING("string", "String");

    public final String name;
    public final String javaType;

    private static final ImmutableMap<String, Type> nameToType;

    static {
      ImmutableMap.Builder<String, Type> builder = ImmutableMap.builder();
      for (Type type : Type.values()) {
        builder.put(type.name, type);
      }
      nameToType = builder.build();
    }

    Type(String name) {
      this(name, name);
    }

    Type(String name, String javaType) {
      this.name = name;
      this.javaType = javaType;
    }

    public static Type fromName(String name) {
      return nameToType.get(name);
    }

    @Override
    public String toString() {
      return name;
    }
  }

  public enum FieldRule {
    optional,
    required,
    repeated;

    public static FieldRule fromName(String name) {
      switch (name) {
        case "optional":
          return optional;
        case "required":
          return required;
        case "repeated":
          return repeated;
        default:
          return null;
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
      this.javaName = Utils.lowerCamel(name);
      javaGetterName = Utils.upperCamel("get_" + name);
      javaSetterName = Utils.upperCamel("set_" + name);
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

  public String fieldName(int id) {
    Field field = field(id);
    return field == null ? null : field.name;
  }

  public String fieldName(String id) {
    Field field = field(id);
    return field == null ? null : field.name;
  }

  public Field field(int id) {
    return idToFields.get(id);
  }

  public Field field(String name) {
    return nameToFields.get(name);
  }
}
