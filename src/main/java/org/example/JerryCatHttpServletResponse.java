package org.example;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class JerryCatHttpServletResponse extends HttpServletResponseWrapper {
    private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    private final PrintWriter printWriter = new PrintWriter(byteArrayOutputStream);

    public JerryCatHttpServletResponse(HttpServletResponse response) {
        super(response);
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return printWriter;
    }

    public byte[] getResponseByte() {
        return byteArrayOutputStream.toByteArray();
    }
}
