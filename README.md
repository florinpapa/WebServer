Simple Java Web Server


This is a basic implementation of an HTTP server, that exposes a directory
structure and allows directory listing and file downloading. The starting point
for this project is represented by the Hypertext Transfer Protocol RFC available
here:

https://tools.ietf.org/html/rfc7230

The documentation states that it is mandatory for every HTTP server to implement
support for the "GET" and "HEAD" methods, any other method being optional. These
two methods are enough for a basic directory listing and file download 
functionality, therefore the current web server only implements "GET" and "HEAD"
(for the moment).

The application has three main classes: WebServer, WebThread and HTTPParser. The
WebServer class is responsible with starting the server socket that listens for
requests. It is also responsible with starting the threads that actually handle
these requests. The server implements a thread pool mechanism, which means that
the WebServer object will spawn a predefined number of worker threads (specified
in the constructor) that consume work items from a synchronized queue. A
semaphore is used in order to avoid busy waiting on the queue. Each thread waits
for the semaphore to be released in order to acquire an item from the work
queue. The ServerSocket has a timeout of 2 seconds in order to prevent the
WebServer from waiting indefinitely in accept().

The WebThread class extends Thread and handles the actual dialogue with the
client - reads the request line and the attributes and returns the response
header and the actual file (if the method is "GET" and the file exists).

The message received from the client is parsed using the HTTPParser class. This
class implements the HTTP protocol, as defined in the RFC mentioned above. The
method that parses a request is parseRequest(), which first parses the request
line to extract the method, URI and HTTP version. If the request line does not
contain these three elements, a 400 code (BAD REQUEST) is returned. This error
code is also returned if the request is incorrect (bad request method, bad
URI formatting, bad HTTP version, missing Host attribute for HTTP/1.1, white
space between the Field-Name and the colon for an attribute).

A 405 error code (Not IMPLEMENTED) is returned if the request method is an
allowed HTTP method, but it is not implemented by this server. A 404 code (FILE
NOT FOUND) is returned if the URI does not point to a valid file or it goes
outside the server resource root directory. If the file is found, a 200 code 
(OK) is returned.

Parsing the URI requires determining if the URI is in origin form (ex: "GET 
/html/index.php HTTP/1.1") or absolute form (ex: "GET
http://localhost:8080/html/index.php HTTP/1.1"). Regular expressions are used
to determine which form the URI is in.

