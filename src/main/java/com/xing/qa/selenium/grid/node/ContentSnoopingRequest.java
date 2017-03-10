package com.xing.qa.selenium.grid.node;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.io.InputStream;

/**
* ContentSnoopingRequest wraps the original request to allow us to peek into the payload of the request.
*
* @author Jens Hausherr (jens.hausherr@xing.com)
*/
class ContentSnoopingRequest extends HttpServletRequestWrapper {

    private String content;
    private String encoding;

    public String getContent() {
        return content;
    }

    /**
     * Constructs a request object wrapping the given request.
     *
     * @param request
     * @throws IllegalArgumentException if the request is null
     */
    public ContentSnoopingRequest(HttpServletRequest request) {
        super(request);

        encoding = request.getCharacterEncoding();
        if (encoding == null)
            encoding = "ISO-8859-1";

        try {
            StringBuilder sb = new StringBuilder();
            InputStream is = request.getInputStream();
            byte[] buffer = new byte[1024];
            int read = 0;

            while ((read = is.read(buffer, 0, 1024)) != -1) {
                sb.append(new String(buffer, 0, read, encoding));
            }

            this.content = sb.toString();

        } catch (IOException e) {
            e.printStackTrace();
            this.content = "";
        }
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return new ServletInputStream() {

            int idx = 0;
            byte[] contents = content.getBytes(encoding);

            @Override
            public boolean isFinished() {
                return idx < contents.length;
            }

            @Override
            public boolean isReady() {
                return !isFinished();
            }

            @Override
            public int read() throws IOException {
                if (idx < contents.length) {
                    return contents[idx++];
                } else return -1;
            }

            @Override
            public void setReadListener(ReadListener listener) {
                throw new java.lang.IllegalStateException();
            }
        };
    }

}
