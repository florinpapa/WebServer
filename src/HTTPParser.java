import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

public class HTTPParser {
	private int BAD_REQUEST = 400;
	
	private class BasicRequest {
		String method;
		String path;
		String httpVersion;
	}
	
	void parseRequest(BufferedReader in, PrintWriter out) throws IOException {
		BasicRequest reqLine = parseRequestLine(in);
		String response;
		
		if (reqLine == null) { // bad request
			response = getResponseHeader(BAD_REQUEST);
			out.println(response);
			out.flush();
			return;
		}
	}
	
	String getResponseHeader(int statusCode) {
		String result = new String("");
		
		if (statusCode == BAD_REQUEST)
			result += "HTTP/1.1 400 Bad Request";
		
		return result;
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
