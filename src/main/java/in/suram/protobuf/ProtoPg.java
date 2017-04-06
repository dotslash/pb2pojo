package in.suram.protobuf;

import com.google.common.collect.Lists;

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
import java.util.List;

import in.suram.antlr.ProtoBaseListener;
import in.suram.antlr.ProtoLexer;
import in.suram.antlr.ProtoParser;
import in.suram.protobuf.Message.Field;
import in.suram.protobuf.Message.FieldRule;
import in.suram.protobuf.Message.Type;

public class ProtoPg {
    public static void main(String[] args) throws IOException {
        System.out.println(IOUtils.toString(ProtoPg.class.getResourceAsStream("/example.proto")));
        List<Message> messages = CompileProtoDefinition();
        for (Message message : messages) {
            System.out.printf("name=%s\n", message.name);
            for (Field field : message.fields) {
                System.out.printf(" Field = `%s` type = `%s` rule = `%s` id = `%s`\n",
                        field.name,
                        field.type,
                        field.fieldRule,
                        field.id);
            }

        }

    }

    private static List<Message> CompileProtoDefinition() throws IOException {
        ArrayList<Message> nonThreadSafeMessages = Lists.newArrayList();
        List<Message> messages = Collections.synchronizedList(nonThreadSafeMessages);
        InputStream inputStream = ProtoPg.class.getResourceAsStream("/example.proto");
        ProtoLexer lexer = new ProtoLexer(new ANTLRInputStream(inputStream));
        ProtoParser parser = new ProtoParser(new CommonTokenStream(lexer));

        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int
                    line, int pos, String msg, RecognitionException e) {
                throw new IllegalStateException(
                        "failed to parse at line " + line + " due to " + msg, e);
            }
        });

        parser.addParseListener(new ProtoBaseListener() {
            @Override
            public void exitMessage(ProtoParser.MessageContext ctx) {
                String name = ctx.IDENTIFIER().getText();
                List<Field> fields = Lists.newArrayList();
                for (ProtoParser.Field_declarationContext field : ctx
                        .field_declaration()) {
                    Field msgfield = new Field(
                            FieldRule.fromName(field.FIELD_RULE().getText()),
                            Type.fromName(field.TYPE().getText()),
                            field.IDENTIFIER().getText(),
                            Integer.parseInt(field.NUMBER().getText()));
                    fields.add(msgfield);
                }
                messages.add(new Message(name, fields));
            }
        });
        parser.proto();
        return nonThreadSafeMessages;
    }
}