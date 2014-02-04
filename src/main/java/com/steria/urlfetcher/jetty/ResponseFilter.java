package com.steria.urlfetcher.jetty;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;

import com.steria.urlfetcher.db.IResponseLogger;

@WebFilter("/*")
public class ResponseFilter implements Filter {
	IResponseLogger responseLogger;
	Boolean applyFilter;
	
	@Override
	public void init(FilterConfig config) throws ServletException {
		try{
			String responseLoggerClass = System.getProperty("ResponseLoggerClass", "com.steria.urlfetcher.db.JDBCResponseLogger");
			Class<?> rlClass = Class.forName(responseLoggerClass);
			responseLogger = (IResponseLogger)rlClass.newInstance();
		  responseLogger.init();
		}catch(Exception e){
			e.printStackTrace();
			return;
		}
		applyFilter = true;
	}
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws ServletException, IOException {
		//avoid crashing the server if there are db problems for the filter...
		if(!applyFilter){
			chain.doFilter(request, response);
			return;
		}
		try{
			if (response.getCharacterEncoding() == null) {
				response.setCharacterEncoding("UTF-8"); // Or whatever default. UTF-8 is
																								// good for World Domination.
			}
	
			HttpServletResponseCopier responseCopier = new HttpServletResponseCopier(
					(HttpServletResponse) response);
	
			Request req = (Request) request;
			Response res = (Response) response;
			try {
				chain.doFilter(request, responseCopier);
				responseCopier.flushBuffer();
			} finally {
				byte[] copy = responseCopier.getCopy();
				String responseString = new String(copy, response.getCharacterEncoding());
				log(req, res, responseString);
			}
		}catch(Exception e){
			System.err.println("Exception in logging filter, disabled until next restart: "+e);
			e.printStackTrace(System.err);
			applyFilter = false;
		}
	}

	private void log(Request request, Response response, String responseBody) throws Exception {
			String requestHeaders = "";
			Enumeration<String> headerNames = request.getHeaderNames();
			while (headerNames.hasMoreElements()) {
				String headerName = headerNames.nextElement();
				Enumeration<String> headers = request.getHeaders(headerName);
				while(headers.hasMoreElements()){
					String nextElement = headers.nextElement();
					//ensure we persist each cookie as a separate header line, so we're consistent across all api:s...
					if("CookieZZZ".equals(headerName)){
						String[] cookies = nextElement.split("; ");
						for(String cookie: cookies){
							requestHeaders += headerName + ": " + cookie + "\n";
						}
					}else{
						requestHeaders += headerName + ": " + nextElement + "\n";
					}
				}				
			}
			
			HttpSession session = request.getSession(false);
			String sessionId = "";
			if(session != null){
				sessionId = session.getId();
			}
			String requestBody = "";
			String line= null;
			BufferedReader in = new BufferedReader( 
          new InputStreamReader( 
                  request.getInputStream()));
			while((line=in.readLine()) != null){
				if(!"".equals(requestBody)){
					requestBody += "\n";
				}
				requestBody += line;
			}
			
			String responseHeaders = "";
			Collection<String> responseHeaderNames = response.getHeaderNames();
			for (String headerName: responseHeaderNames) {
				Collection<String> headers = response.getHeaders(headerName);
				for(String header: headers){
					responseHeaders += headerName + ": " + header + "\n";	
				}				
			}
			responseHeaders += "Content-Length: "+responseBody.length()+"\n";
			responseHeaders += "Server: "+"SERVER_REPLACE_ME_IN_REGEXP_FILE"+"\n";
			
			String method = request.getMethod();
			String query = request.getQueryString();
			if(query == null){
				query = "";
			}
			String r = request.getRequestURL() + query;
			
			responseLogger.persist(r, method, requestHeaders, requestBody, sessionId, response.getStatus(), responseBody, responseHeaders);
	}
	
	@Override
	public void destroy(){
		if(responseLogger != null){
			responseLogger.destroy();
		}
	}
}
