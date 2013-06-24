package info.pishen.radio;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;

@WebFilter(urlPatterns={"/*"})
public class FrontFilter implements Filter {

	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp,
			FilterChain chain) throws IOException, ServletException {
		HttpServletRequest httpReq = (HttpServletRequest)req;
		String path = httpReq.getRequestURI().substring(httpReq.getContextPath().length());
		
		if(path.startsWith("/resources/")){
			chain.doFilter(req, resp);
		}else{
			req.getRequestDispatcher("/servlets" + path).forward(req, resp);			
		}
	}

	@Override
	public void init(FilterConfig fConfig) {
	}

}
