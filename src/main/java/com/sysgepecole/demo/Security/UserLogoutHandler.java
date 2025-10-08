package com.sysgepecole.demo.Security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

@Component
public class UserLogoutHandler implements LogoutHandler {

	 @Override
	    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
	        if (authentication != null) {
	            System.out.println("Logging out user: " + authentication.getName());

	            SecurityContextHolder.clearContext();
	            request.getSession().invalidate();
	            response.addCookie(createLogoutCookie("JSESSIONID"));
	        }
	    }

	    private Cookie createLogoutCookie(String cookieName) {
	        Cookie cookie = new Cookie(cookieName, null);
	        cookie.setPath("/");
	        cookie.setHttpOnly(true);
	        cookie.setMaxAge(0);
	        return cookie;
	    }

}
