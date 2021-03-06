import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
	private StatusCodes statusCodes = new StatusCodes();
	private String[] supported_methods = {"GET", "HEAD"};
	private String[] not_implemented_methods = {"PUT", "POST", "DELETE",
											    "CONNECT", "OPTIONS", "TRACE"};
	private String serverRoot = "res/";
	private String host;
	private int port;
	
	private class BasicRequest {
		String method;
		String path;
		String httpVersion;
	}
	
	private class RequestInfo {
		int statusCode;
		File toSend;
		OutputStream out;
		String method;
	}
	
	public HTTPParser(String host, int port) {
		this.host = host;
		this.port = port;
	}
	
	/*
	 * Function that parses an entire request. First, the request line is read
	 * to determine the method, URI and HTTP version. The following lines that
	 * contain attributes are read and stored in a Hashtable.
	 * 
	 * The response is also formed here, according with the request
	 * correctness.
	 */
	void parseRequest(BufferedReader in, OutputStream out) throws IOException {
		BasicRequest reqLine = parseRequestLine(in);
		Hashtable<String, String> attributes;
		RequestInfo req = new RequestInfo();
		
		req.out = out;
		if (reqLine == null || !isHTTPMethod(reqLine.method)) { // bad request
			req.statusCode = statusCodes.BAD_REQUEST;
			printResponse(req);
			return;
		} else {
			attributes = parseAttributes(in);
			if (attributes == null) {
				req.statusCode = statusCodes.BAD_REQUEST;
				printResponse(req);
				return;
			}
			
			if (isSupported(reqLine.method)) {
				File target;
				String method = reqLine.method;
				
				/*
				 * Parse request header to determine if the target
				 * resource is correct
				 */
				target = parseTargetResource(reqLine.path);
				
				/* badly formatted URI */
				if (target == null) {
					req.statusCode = statusCodes.BAD_REQUEST;
					printResponse(req);
					return;
				} else {
					/* HTTP/1.1 requires that the Host attribute is set */
					if (!supportedHttpVersion(reqLine.httpVersion, attributes)) {
						req.statusCode = statusCodes.BAD_REQUEST;
						printResponse(req);
						return;
					}
					
					if (method.equals("GET") || method.equals("HEAD")) {
						/* 404 file not found */
						if (!isValid(target)) {
							req.statusCode = statusCodes.NOT_FOUND;
							printResponse(req);
							return;
						} else {
							req.method = reqLine.method;
							req.statusCode = statusCodes.OK;
							req.toSend = target;
							printResponse(req);
						}
					}
				}
			} else {	// method not allowed
				req.statusCode = statusCodes.NOT_IMPLEMENTED;
				req.method = reqLine.method;
				printResponse(req);
				return;
			}
		}
	}
	
	boolean supportedHttpVersion(String version, Hashtable<String, String> attr) {
		if (version.equals("HTTP/1.0"))
			return true;
		else if (version.equals("HTTP/1.1")) {
			if (attr.containsKey("Host")) {
				String host = attr.get("Host");
				String hostComponents[] = host.split(":");
				
				if (hostComponents.length == 1)
					return hostComponents[0].equals(this.host);
				else if (hostComponents.length == 2) {
					int recvPort = Integer.parseInt(hostComponents[1]);
					String recvHost = hostComponents[0];
					return (recvHost.equals(this.host) && recvPort == this.port); 
				}
			}
		}
		
		return false;
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
		String absPattern = "(http://)?" + this.host + "(:" + 
							this.port + ")?/.*";
		Pattern originPattern = Pattern.compile("/.*");
		Pattern absolutePattern = Pattern.compile(absPattern);
		Matcher originMatcher = originPattern.matcher(path);
		Matcher absoluteMatcher = absolutePattern.matcher(path);
		File result = null;
		
		if (originMatcher.matches()) { // origin form
			result = new File(serverRoot, path);
		} else if (absoluteMatcher.matches()){ // absolute form
			String relativePath;
			
			if (path.startsWith("http://")) {
				String prefix = "http://" + this.host;
				relativePath = path.substring(prefix.length());
			} else {
				relativePath = path.substring(this.host.length());
			}
			
			if (relativePath.startsWith(":")) {
				String pathComponents[] = relativePath.split("/", 2);
				if (pathComponents.length == 1)
					relativePath = "/";
				else
					relativePath = "/" + pathComponents[1];
			}
			result = new File(serverRoot, relativePath);
		} else {
			return null;
		}
		
		return result;
	}
	
	/*
	 * Check whether a file that was requested is valid.
	 * Determine if the file exists and if it is inside the
	 * server resource folder.
	 */
	boolean isValid(File f) throws IOException {
		if (f.exists()) {
			String filePath = f.getCanonicalPath();
			File res = new File("res");
			String resPath = res.getCanonicalPath();
			
			if (filePath.startsWith(resPath))
				return true;
		}
		
		return false;
	}
	
	String getTime(Object timestamp) {
		String result = "";
		final SimpleDateFormat sdf =
		        new SimpleDateFormat("EEE, MMM d, yyyy HH:mm:ss z");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT")); // GMT time
		
		if (timestamp == null) {
			final Date currentTime = new Date();
			result = sdf.format(currentTime);
		} else {
			result = sdf.format(timestamp);
		}
		
		return result;
	}
	
	String getServerInfo() {
		String result = new String("");
		
		result += "Server: TinyJavaServer Java8\n";
		result += "Date: " + getTime(null) + "\n";
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
	
	void printResponse(RequestInfo req) throws IOException {
		String result = new String("");
		PrintWriter out = new PrintWriter(req.out);
		
		if (req.statusCode == statusCodes.BAD_REQUEST) {
			result += "HTTP/1.1 400 Bad Request\n";
			result += formatResponse("html/badrequest.html");
			out.println(result);
			out.flush();
		} else if (req.statusCode == statusCodes.NOT_IMPLEMENTED) {
			result += "HTTP/1.1 501 Unsupported Method ('";
			result += req.method + "')\n";
			result += formatResponse("html/unsupported.html");
			result = result.replace("method",
                    	"method '(" + req.method + ")'");
			out.println(result);
			out.flush();
		} else if (req.statusCode == statusCodes.NOT_FOUND) {
			result += "HTTP/1.1 404 File not found\n";
			result += formatResponse("html/filenotfound.html");
			out.println(result);
			out.flush();
		} else if (req.statusCode == statusCodes.OK) {		
			result += "HTTP/1.1 200 OK\n";
			result += getServerInfo();
			out.print(result);
			out.flush();
			sendFile(req);
		}
	}
	
	void sendFile(RequestInfo req) throws IOException {
		String result = "";
		File f = req.toSend;
		PrintWriter out = new PrintWriter(req.out);
		
		if (f.isDirectory()) {
			String htmlBody = getDirListing(f);
			result += "Content-type: text/html; charset=utf-8\n";
			result += "Content-Length: " + htmlBody.length() + "\n\n";
			
			if (req.method.equals("GET"))
				out.println(result + htmlBody);
			else
				out.print(result);
			out.flush();
		} else {
			long size = f.length();
			
			if (isHtml(f))
				result += "Content-type: text/html; charset=utf-8\n";
			else
				result += "Content-type: application/octet-stream\n";
			
			result += "Content-Length: " + size + "\n";
			result += "Last-Modified: " + getTime(f.lastModified()) + "\n\n";
			out.print(result);
			out.flush();
			
			if (req.method.equals("GET")) {
				InputStream in = new FileInputStream(f);
				byte[] bytes = new byte[16 * 1024];
				int count;
				
				while ((count = in.read(bytes)) > 0) {
		            req.out.write(bytes, 0, count);
		        }
				
				in.close();
				out.println();
				out.flush();
			}
			
		}
	}
	
	boolean isHtml(File f) {
		String name = f.getName();
		return name.endsWith(".html");
	}
	
	String getRelativePath(File f) throws IOException {
		File rootDir = new File(this.serverRoot);
		String path = f.getCanonicalPath();
		String rootPath = rootDir.getCanonicalPath();
		String result = path.replace(rootPath, "");
		
		return result;
	}
	
	String getDirListing(File dir) throws IOException {
		String result = "";
		File[] dirFiles = dir.listFiles();
		
		result += "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 3.2 Final//EN\"><html>\n";
		result += "<title>Directory listing for " + getRelativePath(dir) + "/</title>\n";
		result += "<body>\n";
		result += "<h2>Directory listing for " + getRelativePath(dir) + "/</h2>\n";
		result += "<hr>";
		result += "<ul>";
		for (File f : dirFiles) {
			String path = getRelativePath(f);
			result += "<li><a href=\"" + path + "\">" + f.getName() + "</a>\n";
		}
		result += "</ul>\n</hr>\n</body>\n</html>";

		return result;
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
		
		if (req_parts.length != 2 && req_parts.length != 3) // bad request
			return null;
		
		result = new BasicRequest();
		result.method = req_parts[0];
		result.path = req_parts[1];
		if (req_parts.length == 3)
				result.httpVersion = req_parts[2]; 

		return result;
	}
	
	/*
	 * Function that parses the attributes received in the header of an HTTP
	 * request. Returns a Hashtable<String, String> where the key is the
	 * attribute name.
	 * 
	 * Attributes should have the syntax "Field-Name: Field-Value". No
	 * whitespace is allowed between Field-Name and the colon, due to
	 * security vulnerabilities.
	 * (see https://tools.ietf.org/html/rfc7230#section-3.2.4)
	 * A 400 - Bad Request will be returned for a whitespace detected there.
	 * Preceding and trailing whitespaces in Field-Value will be discarded.  
	 */
	Hashtable<String, String> parseAttributes(BufferedReader in) throws IOException {
		Hashtable<String, String> result = new Hashtable<>();
		String line = in.readLine();
		boolean invalid = false;
		
		while (!line.equals("")) {
			/* only split by the first ':' */
			String[] tokens = line.split(":", 2);
			if (tokens.length >= 1) {
				/* no whitespace allowed between Field-Name and colon */
				if (tokens[0].endsWith(" "))
					invalid = true;
				
				if (tokens.length == 2) // Field-Name: Field-Value
					result.put(tokens[0], tokens[1].trim());
				else if (tokens.length == 1) // Field-Name: nothing
					result.put(tokens[0], "");
			} else { // not a valid attribute
				invalid = true;
			}
			line = in.readLine();
		}

		if (invalid)
			return null;
		else
			return result;
	}
	
}