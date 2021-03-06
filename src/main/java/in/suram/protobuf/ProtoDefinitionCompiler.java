package in.suram.protobuf;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import in.suram.antlr.ProtoBaseListener;
import in.suram.antlr.ProtoLexer;
import in.suram.antlr.ProtoParser;
import in.suram.protobuf.CompiledProtos.Field;
import in.suram.protobuf.CompiledProtos.FieldRule;
import in.suram.protobuf.CompiledProtos.Message;
import in.suram.protobuf.CompiledProtos.Type;

public class ProtoDefinitionCompiler {

  public static List<Message> Compile(InputStream inputStream) throws IOException {
    ArrayList<Message> nonThreadSafeMessages = Lists.newArrayList();
    List<Message> messages = Collections.synchronizedList(nonThreadSafeMessages);
    Set<String> messageNames = Sets.newConcurrentHashSet();
    Set<String> usedMessageNames = Sets.newConcurrentHashSet();
    ProtoLexer lexer = new ProtoLexer(new ANTLRInputStream(inputStream));
    ProtoParser parser = new ProtoParser(new CommonTokenStream(lexer));

    parser.addErrorListener(
        new BaseErrorListener() {
          @Override
          public void syntaxError(
              Recognizer<?, ?> recognizer,
              Object offendingSymbol,
              int line,
              int pos,
              String msg,
              RecognitionException e) {
            throw new IllegalStateException(
                "failed to parse at line " + line + " due to " + msg, e);
          }
        });

    parser.addParseListener(
        new ProtoBaseListener() {
          @Override
          public void exitMessage(ProtoParser.MessageContext ctx) {
            String name = ctx.IDENTIFIER().getText();
            messageNames.add(name);
            List<Field> fields = Lists.newArrayList();
            for (ProtoParser.Field_declarationContext field : ctx.field_declaration()) {
              Type fieldType = Type.make(field.IDENTIFIER().get(0).getText());
              if (fieldType.primitive == CompiledProtos.Primitive.NONE) {
                usedMessageNames.add(fieldType.javaType);
              }
              Field msgfield =
                  new Field(
                      FieldRule.fromName(field.FIELD_RULE().getText()),
                      fieldType,
                      field.IDENTIFIER().get(1).getText(),
                      Integer.parseInt(field.NUMBER().getText()));
              fields.add(msgfield);
            }
            Message message = new Message(name, fields);
            if (!message.isValid) {
              throw new RuntimeException("invalid message definition :" + name);
            }
            messages.add(message);
          }
        });
    parser.proto();
    Sets.SetView<String> difference = Sets.difference(usedMessageNames, messageNames);
    if (!difference.isEmpty()) {
      String unknownMessages = "Unknown Messages : " + Joiner.on(",").join(difference);
      throw new RuntimeException(unknownMessages);
    }
    return nonThreadSafeMessages;
  }

  public static void main(String[] args) throws IOException {

    System.out.println(IOUtils.toString(Utils.ProjectResourceToStream("/example.proto")));
    List<Message> messages = Compile(Utils.ProjectResourceToStream("/example.proto"));
    for (Message message : messages) {
      System.out.printf("name=%s\n", message.name);
      for (Field field : message.fields) {
        System.out.printf(
            " Field = `%s` type = `%s` rule = `%s` id = `%s`\n",
            field.name, field.type, field.fieldRule, field.id);
      }
    }
  }
}
