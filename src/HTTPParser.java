import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HTTPParser {
	private int BAD_REQUEST = 400;
	private int NOT_IMPLEMENTED = 405;
	private String[] supported_methods = {"GET", "HEAD"};
	private String[] not_implemented_methods = {"PUT", "POST", "DELETE",
											    "CONNECT", "OPTIONS", "TRACE"};
	private String serverRoot = "res/";
	private String serverName = "localhost";
	
	private class BasicRequest {
		String method;
		String path;
		String httpVersion;
	}
	
	void parseRequest(BufferedReader in, PrintWriter out) throws IOException {
		BasicRequest reqLine = parseRequestLine(in);
		String response;
		Hashtable<String, String> attributes;
		
		if (reqLine == null || !isHTTPMethod(reqLine.method)) { // bad request
			printResponse(BAD_REQUEST, "", out);
			return;
		} else {
			attributes = parseAttributes(in);
			if (isSupported(reqLine.method)) {
				File target;
				
				/*
				 * Parse request header to determine if the target
				 * resource is correct
				 */
				target = parseTargetResource(reqLine.path);
				if (target == null) {
					printResponse(BAD_REQUEST, "", out);
					return;
				}
			} else {	// method not allowed
				printResponse(NOT_IMPLEMENTED, reqLine.method, out);
				return;
			}
		}
	}
	
	/*
	 * Parse request target to determine whether it is correct
	 * and it points to a valid file, inside the HTTP server
	 * root. 
	 * 
	 * Determine if the target resource is in origin form or
	 * absolute form, as described in section 5.3 of
	 * the HTTP RFC here:
	 * https://tools.ietf.org/html/rfc7230#section-5.3
	 * 
	 * Return File if the target resource is valid
	 * and null otherwise.
	 */
	File parseTargetResource(String path) {
		Pattern originPattern = Pattern.compile("/.*");
		Pattern absolutePattern = Pattern.compile("http://localhost/.*");
		Matcher originMatcher = originPattern.matcher(path);
		Matcher absoluteMatcher = absolutePattern.matcher(path);
		File result = null;
		int pathStartIndex = 17;
		
		if (originMatcher.matches()) { // origin form
			System.out.println("origin form " + serverRoot + " " + path);
			result = new File(serverRoot, path);
			if (!result.exists())
				return null;
		} else if (absoluteMatcher.matches()){ // absolute form
			String relativePath = path.substring(pathStartIndex);
			System.out.println("absolute form " + serverRoot + " " + relativePath);
			result = new File(serverRoot, relativePath);
			if (!result.exists())
				return null;
		}
		
		return result;
	}
	
	String getServerInfo() {
		String result = new String("");
		final Date currentTime = new Date();
		final SimpleDateFormat sdf =
		        new SimpleDateFormat("EEE, MMM d, yyyy hh:mm:ss a z");

		// Output GMT time
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		result += "Server: TinyJavaServer Java8\n";
		result += "Date: " + sdf.format(currentTime) + "\n";
		result += "Connection: close\n";
		
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
	
	void printResponse(int statusCode, String args, PrintWriter out) throws IOException {
		String result = new String("");
		
		if (statusCode == BAD_REQUEST) {
			result += "HTTP/1.1 400 Bad Request";
			result += formatResponse("html/badrequest.html");
			out.println(result);
			out.flush();
		} if (statusCode == NOT_IMPLEMENTED) {
			result += "HTTP/1.1 501 Unsupported Method ('";
			result += args + "')\n";
			result += formatResponse("html/unsupported.html");
			result = result.replace("method",
                    	"method '(" + args + ")'");
			out.println(result);
			out.flush();
		}
	}
	
	String formatResponse(String filename) throws IOException {
		String result = "";
		String fileContent;
		
		fileContent = readFile(filename);
		
		result += getServerInfo();
		result += "Content-Type: text/html; charset=UTF-8\n";
		result += "Content-Length: " + fileContent.length() + "\n\n";
		result += fileContent;
		
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
		while (req.equals("")) {
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
	
	/*
	 * Function that parses the attributes received
	 * in the header of an HTTP request. Returns a
	 * Hashtable<String, String> where the key is
	 * the attribute name.
	 */
	Hashtable<String, String> parseAttributes(BufferedReader in) throws IOException {
		Hashtable<String, String> result = new Hashtable<>();
		String line = in.readLine();
		
		while (!line.equals("")) {
			/* only split by the first ':' */
			String[] tokens = line.split(":", 2);
			if (tokens.length == 2)
				result.put(tokens[0], tokens[1]);
			else
				result.put(tokens[0], "");
			line = in.readLine();
		}

		return result;
	}
	
}
