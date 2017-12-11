package de.ids_mannheim.korap.authentication.spring;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;

public class KustvaktBasicAuthenticationEntryPoint
        extends BasicAuthenticationEntryPoint {

    @Override
    public void commence (HttpServletRequest request,
            HttpServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {
        super.commence(request, response, authException);

        String notification = ((KustvaktAuthenticationException) authException)
                .getNotification();

        PrintWriter writer = response.getWriter();
        writer.write(notification);
        writer.flush();
        writer.close();
    }
}
