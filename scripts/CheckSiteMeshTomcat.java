import java.nio.file.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import jakarta.servlet.*;
import org.apache.catalina.*;
import org.apache.catalina.connector.*;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.valves.ValveBase;
import vn.iotstar.controller.ProfileController;
import vn.iotstar.model.User;
import vn.iotstar.service.UserProfileService;

/** Uses an existing Tomcat 11 installation on the classpath. No SQL, SMTP or saved uploads.
 * Runs the packaged application/JSPs on an ephemeral loopback port with an in-memory profile.
 * This standalone checker is never packaged in the application WAR.
 */
public class CheckSiteMeshTomcat {
    static int checks;
    static void check(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
        checks++; System.out.println("PASS: " + label);
    }
    static int count(String html, String tag) {
        return (int)java.util.regex.Pattern.compile("<" + tag + "(?:\\s|>)", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(html).results().count();
    }
    public static void main(String[] args) throws Exception {
        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(Path.of("target/sitemesh-tomcat").toAbsolutePath().toString());
        tomcat.setPort(0);
        tomcat.getConnector().setProperty("address", "127.0.0.1");
        Context context = tomcat.addWebapp("/ServletCRUDMVC", Path.of("target/ServletCRUDMVC").toAbsolutePath().toString());
        context.setParentClassLoader(CheckSiteMeshTomcat.class.getClassLoader());
        var loader = new org.apache.catalina.loader.WebappLoader();
        loader.setDelegate(true); context.setLoader(loader);
        User fixture = new User(); fixture.setId(1); fixture.setActive(true); fixture.setRoleid(5);
        fixture.setUserName("layout-fixture"); fixture.setFullName("Layout <probe>");
        fixture.setEmail("layout@example.invalid"); fixture.setPhone("+84912345678");
        int[] reads = {0};
        Path webRoot = Path.of("target/ServletCRUDMVC");
        java.util.List<String> jsps;
        try (var paths = Files.walk(webRoot)) {
            jsps = paths.filter(p -> p.toString().endsWith(".jsp"))
                    .map(p -> "/" + webRoot.relativize(p).toString().replace('\\', '/')).sorted().toList();
        }
        context.getPipeline().addValve(new ValveBase() {
            public void invoke(Request req, Response resp) throws java.io.IOException, ServletException {
                String compile = req.getParameter("compile");
                if (jsps.contains(compile) && "true".equals(req.getParameter("jsp_precompile"))) {
                    req.getRequestDispatcher(compile).forward(req, resp); return;
                }
                if (!"guest".equals(req.getParameter("mode"))) req.getSession(true).setAttribute("account", fixture);
                getNext().invoke(req, resp);
            }
        });
        try {
            tomcat.start();
            check(context.getState().isAvailable(), "packaged application starts on existing Tomcat 11");
            check(!context.getSuspendWrappedResponseAfterForward(), "packaged context.xml disables premature response suspension");
            check(context.getServletContext().getFilterRegistration("profileSiteMeshFilter") != null,
                    "annotation scanning registers ProfileLayoutFilter");
            Wrapper wrapper = (Wrapper)context.findChild(context.findServletMapping("/profile"));
            check(wrapper != null && ProfileController.class.getName().equals(wrapper.getServletClass()),
                    "/profile maps to the real ProfileController");
            wrapper.setServlet(new ProfileController(new UserProfileService() {
                public User findById(int id) { reads[0]++; return fixture; }
                public void validate(String name, String phone) { throw new AssertionError("No writes allowed"); }
                public User updateProfile(int id, String name, String phone, String avatar) { throw new AssertionError("No writes allowed"); }
            }));
            String base = "http://127.0.0.1:" + tomcat.getConnector().getLocalPort() + "/ServletCRUDMVC";
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
            var response = client.send(HttpRequest.newBuilder(URI.create(base + "/profile")).timeout(Duration.ofSeconds(30)).build(), HttpResponse.BodyHandlers.ofString());
            String html = response.body();
            check(response.statusCode() == 200 && !html.isBlank(), "GET /profile returns non-empty HTTP 200 (status=" + response.statusCode() + ", length=" + html.length() + ", profile reads=" + reads[0] + ")");
            check(reads[0] == 1 && html.contains("multipart/form-data") && html.contains("layout-fixture"), "controller forward renders actual profile JSP content");
            check(html.contains("<header") && html.contains("<footer") && html.contains("<style>"), "main.jsp supplies header, footer and styles");
            check(count(html,"html") == 1 && count(html,"head") == 1 && count(html,"body") == 1 && count(html,"main") == 1,
                    "exactly one html/head/body and profile fragment");
            check(!html.contains("<sitemesh:") && html.contains("Layout &lt;probe&gt;"), "SiteMesh consumes write tags; stored profile text is escaped");
            var guest = client.send(HttpRequest.newBuilder(URI.create(base + "/profile?mode=guest")).build(), HttpResponse.BodyHandlers.ofString());
            check(guest.statusCode() == 302 && guest.headers().firstValue("location").orElse("").endsWith("/login"), "guest redirects to login");
            for (String jsp : jsps) {
                var compiled = client.send(HttpRequest.newBuilder(URI.create(base + "/profile?jsp_precompile=true&compile=" + jsp))
                        .timeout(Duration.ofSeconds(30)).build(), HttpResponse.BodyHandlers.discarding());
                check(compiled.statusCode() == 200, "Jasper compiles " + jsp);
            }
            System.out.println("CHECKS PASSED: " + checks);
        } finally { tomcat.stop(); tomcat.destroy(); }
    }
}
