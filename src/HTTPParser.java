import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class HTTPParser {
	private int BAD_REQUEST = 400;
	private int NOT_IMPLEMENTED = 405;
	private String[] supported_methods = {"GET", "HEAD"};
	private String[] not_implemented_methods = {"PUT", "POST", "DELETE",
											    "CONNECT", "OPTIONS", "TRACE"}; 
	
	private class BasicRequest {
		String method;
		String path;
		String httpVersion;
	}
	
	void parseRequest(BufferedReader in, PrintWriter out) throws IOException {
		BasicRequest reqLine = parseRequestLine(in);
		String response;
		
		if (reqLine == null || !isHTTPMethod(reqLine.method)) { // bad request
			response = getResponseHeader(BAD_REQUEST, "");
			out.println(response);
			out.flush();
			return;
		} else {
			if (isSupported(reqLine.method)) {
				// do stuff
			} else {	// method not allowed
				response = getResponseHeader(NOT_IMPLEMENTED, reqLine.method);
				out.println(response);
				out.flush();
				return;
			}
		}
	}
	
	String getServerInfo() {
		String result = new String("");
		final Date currentTime = new Date();
		final SimpleDateFormat sdf =
		        new SimpleDateFormat("EEE, MMM d, yyyy hh:mm:ss a z");

		// Output GMT time
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		result += "Server: TinyJavaServer Java8\n";
		result += "Date: " + sdf.format(currentTime) + " GMT\n";
		result += "Content-Type: text/html\n";
		result += "Connection: close\n\n";
		
		return result;
	}
	
	boolean isSupported(String method) {
		int len = supported_methods.length;
		for (int i = 0; i < len; i++) {
			if (method.equals(supported_methods[i]))
				return true;
		}
		return false;
	}
	
	boolean isHTTPMethod(String method) {
		int len = not_implemented_methods.length;
		
		for (int i = 0; i < len; i++) {
			if (method.equals(not_implemented_methods[i]))
				return true;
		}
		
		return isSupported(method);
	}
	
	String getResponseHeader(int statusCode, String args) throws IOException {
		String result = new String("");
		
		if (statusCode == BAD_REQUEST)
			result += "HTTP/1.1 400 Bad Request";
		if (statusCode == NOT_IMPLEMENTED) {
			result += "HTTP/1.1 501 Unsupported Method ('";
			result += args + "')\n";
			result += getServerInfo();
			result += readFile("html/unsupported.html");
		}
		
		return result;
	}
	
	String readFile(String filename) throws IOException {
		return new String(Files.readAllBytes(Paths.get(filename)));
	}
	
	/*
	 * Function that parses the request line of the HTTP request.
	 * Empty lines at the beginning are ignored. If the request
	 * is invalid, the function returns null.
	 */
	BasicRequest parseRequestLine(BufferedReader in) throws IOException {
		BasicRequest result = null;
		
		String req = in.readLine();
		String[] req_parts;
		
		/*
		 * Ignore all blank lines at the beginning
		 * of the request.
		 */
		while (req.equals("\n")) {
			req = in.readLine();
		}
		req_parts = req.split(" ");
		
		if (req_parts.length != 2 && req_parts.length != 3)  {// bad request
			System.out.println("BAD request");
			return null;
		}
		
		result = new BasicRequest();
		result.method = req_parts[0];
		result.path = req_parts[1];
		if (req_parts.length == 3)
				result.httpVersion = req_parts[2]; 
		
		return result;
	}
}
