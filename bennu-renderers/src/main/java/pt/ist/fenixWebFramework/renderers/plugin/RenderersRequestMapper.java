package pt.ist.fenixWebFramework.renderers.plugin;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Supplier;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

public class RenderersRequestMapper {

    private static Supplier<HttpServletRequest> requestMapper = () -> {
        throw new UnsupportedOperationException();
    };

    public static void registerMapper(Supplier<HttpServletRequest> mapper) {
        requestMapper = Objects.requireNonNull(mapper);
    }

    public static HttpServletRequest getCurrentRequest() {
        return requestMapper.get();
    }

    public static Part getUploadedFile(String name) {
        try {
            return requestMapper.get().getPart(name);
        } catch (IOException | ServletException e) {
            throw new RuntimeException(e);
        }
    }

}
