package pt.ist.fenixWebFramework.servlets.filters.contentRewrite;

import java.io.IOException;
import java.io.Writer;

public class ChecksumRewritingWriter extends Writer {

    private final Writer writer;
    private State state = State.DATA;

    public ChecksumRewritingWriter(Writer writer) {
        this.writer = writer;
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        for (int i = 0; i < len; i++) {
            char c = cbuf[i + off];
            switch (state) {
            case DATA:
                if (c == '<') {
                    state = State.OPEN_BRACKET;
                }
                break;
            case OPEN_BRACKET:
                if (c == 'a') {

                } else if (c == 'f') {

                } else {
                    state = State.DATA;
                }
            default:
                throw new UnsupportedOperationException();
            }
        }
        writer.write(cbuf, off, len);
    }

    @Override
    public void flush() throws IOException {
        writer.flush();
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

    private enum State {
        DATA, OPEN_BRACKET, START_LINK, START_FORM;
    }
}
