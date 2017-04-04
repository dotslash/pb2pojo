import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.apache.commons.io.IOUtils;


import java.io.IOException;
import java.io.InputStream;

public class ProtoPg {
    public static void main(String[] args) throws IOException {
        System.out.println(IOUtils.toString(ProtoPg.class.getResourceAsStream("example.proto")));
        InputStream inputStream = ProtoPg.class.getResourceAsStream("example.proto");
        ProtoLexer lexer = new ProtoLexer(new ANTLRInputStream(inputStream));
        ProtoParser parser = new ProtoParser(new CommonTokenStream(lexer));
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(
                    Recognizer<?, ?> recognizer,
                    Object offendingSymbol,
                    int line,
                    int pos,
                    String msg,
                    RecognitionException e) {
                throw new IllegalStateException("failed to parse at line " + line + " due to " +
                        msg, e);
            }
        });
        parser.addParseListener(new ProtoBaseListener() {
            @Override
            public void exitMessages(ProtoParser.MessagesContext ctx) {
                System.out.printf("name = %s\n", ctx.name().getText());
                for (ProtoParser.Field_declarationContext field : ctx
                        .field_declaration()) {
                    System.out.printf("field=%s; id=%s; type=%s\n",
                            field.name().getText(),
                            field.number().getText(),
                            field.type().getText());
                }

            }
        });
        parser.proto();

    }
}