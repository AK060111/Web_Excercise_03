# Audit results — 2026-09-09

## Root causes

- Reproduced on the existing Tomcat 11.0.25: `ProfileController.forward()` finishes by suspending the underlying response through SiteMesh's wrapper. `DispatchMode.INCLUDE` fixes decorator dispatch, but does not fix that earlier forward. Result before the context fix: HTTP 200, zero characters, with the profile service invoked once.
- Added packaged `META-INF/context.xml` with `suspendWrappedResponseAfterForward="false"`. Tomcat now closes the buffering wrapper after forward, allowing SiteMesh to write the final response. See [Tomcat's context reference](https://tomcat.apache.org/tomcat-11.0-doc/config/context.html).
- `main.jsp` declared an unavailable JSP taglib (`http://www.sitemesh.org/decorator`). The actual SiteMesh 3.3.0-RC1 JAR contains no TLD. Removed the directive; SiteMesh processes its write elements in rendered markup.
- The original `/*` decorator mapping selected the decorator in the isolated test; it was not the cause. Narrowed it to `/profile`. Raw, undecorated output was not reproduced in the clean deployment; stale/overridden WTP deployment remains a possible explanation, not a proven one.

## Files changed in this audit

- `src/main/java/vn/iotstar/filter/ProfileLayoutFilter.java`: exact decorator mapping and dispatch comments.
- `src/main/webapp/WEB-INF/decorators/main.jsp`: remove invalid JSP taglib declaration.
- `src/main/webapp/META-INF/context.xml`: Tomcat 11 buffering compatibility, packaged in WAR.
- `scripts/CheckActivation.java`: replace obsolete non-phone fixture with a valid randomized VN phone.
- `scripts/CheckSiteMeshTomcat.java`: standalone real-container checker, excluded from WAR; actual servlet/JSPs with an in-memory profile, ephemeral loopback connector, no SQL/SMTP/uploads, server stopped in finally.
- This report and a current-results pointer in `SECURITY_VALIDATION_SITEMESH.md`.

Existing staged flattening and assignment edits were preserved. ProfileController and the profile JSP fragment needed no changes. No commit, push, clone, nested project, Tomcat installation, or web.xml.

## Final SiteMesh request flow

`GET /profile` → annotation-registered REQUEST-only ProfileLayoutFilter → ProfileController → `forward(/WEB-INF/views/profile.jsp)` into SiteMesh's buffer → select `/WEB-INF/decorators/main.jsp` → INCLUDE decorator → process write elements → header + profile body + footer.

The packaged Tomcat context setting prevents premature suspension after the controller forward. No FORWARD/INCLUDE filter mapping is needed. The decorator alone owns html/head/body.

## Build and tests

`mvn clean package`: PASS, 45 application classes compiled with release 17. Maven needed an approved run outside the network sandbox to resolve dependencies. Execution used installed JDK 26 and existing Tomcat 11.0.25; no claim of running Tomcat on Java 17.

| Checker | Result | Passed assertions |
|---|---|---:|
| CheckMapping | PASS | successful offline mapping run |
| CheckDatabase | PASS | JDBC and JPA read-only run |
| CheckForgotPassword | PASS | 39 |
| CheckProfile | PASS | 21 |
| CheckProduct | PASS | 30 |
| CheckProduct --database | PASS | 38 |
| CheckSecurity | PASS | 56 |
| CheckActivation | PASS | 22 |
| CheckSiteMeshTomcat | PASS | 32 |

Final result: **9/9 checker runs passed; 238 assertions passed, 0 failed; 1 skipped symlink assertion** because this Windows account cannot create symlinks. The product database run repeats the 30 offline assertions. Real SMTP was deliberately not exercised. All database mutations were rolled back; CheckActivation's schema test also rolled back. Test upload files were cleaned.

Real Tomcat output: HTTP 200, 5,055 characters, one html/head/body/main, header/footer/styles, escaped fixture text, no unresolved SiteMesh write tags, guest redirect preserved. All 22 JSP files compiled through Jasper using `jsp_precompile=true`; compilation does not execute page bodies.

Static review confirmed requested login/register/OTP/reset/category/product/profile validation, admin route authorization, POST-only/CSRF category deletion, upload image decoding and containment, failure cleanup and avatar retention, disabled mail debug, unchanged configuration priority and password storage architecture. No printStackTrace or merge markers found. Working and staged `git diff --check` passed. No duplicate Java filenames/packages or tracked build/cache/upload/local application.properties files; targeted tracked credential-assignment scan found none. This is not a claim of an exhaustive secret-history scan.

WAR checks: all 45 application classes, SiteMesh 3.3.0-RC1, main.jsp and context.xml present; no web.xml or checker instrumentation. Local application.properties and upload data remain ignored and preserved. The locally built WAR includes local runtime configuration through the existing Maven resource behavior; it was not published.

## Remaining risks and exact manual Tomcat steps

The real-container checker uses an in-memory authenticated profile; JDBC/JPA were validated separately. Browser appearance and the user's actual STS deployment still need the following verification. Plaintext password migration remains deferred as requested.

1. Stop the existing STS Tomcat server. Refresh this project, run Maven → Update Project, then Project → Clean. In Servers, use Clean and Publish for the existing application. Do not delete local properties or upload directories.
2. Ensure the published app contains `WEB-INF/classes/vn/iotstar/filter/ProfileLayoutFilter.class`, `WEB-INF/lib/sitemesh-3.3.0-RC1.jar`, the updated decorator, and `META-INF/context.xml`. If STS or an existing external Context descriptor overrides packaged context.xml, set `suspendWrappedResponseAfterForward="false"` on that application's effective `<Context>`; do not add a second Context. No global Tomcat change is necessary.
3. Start Tomcat. Confirm its startup log registers `profileSiteMeshFilter`. Open `http://localhost:8080/ServletCRUDMVC/login` (substitute the existing connector port/context only if different) and sign in with an existing active account.
4. Open `http://localhost:8080/ServletCRUDMVC/profile`. Verify HTTP 200 with a non-empty response, header/navigation, styled profile form, footer, and unchanged URL. View response source: exactly one opening html/head/body, one profile main, and no unresolved sitemesh write elements. Check logs for no JSP taglib error or committed-response exception.
5. On a test account, submit an empty fullname and invalid phone: expect the same decorated form with an escaped error. Test `0912345678` and `+84912345678` using an unused number; both formats must pass format validation (existing duplicate checks still apply). Save with no avatar selected and verify the old avatar remains. Restore any intentionally changed profile fields.
6. On that test account, reject a fake .png and an oversized image; a small real PNG should save and render through `/image`. Verify guest /profile redirects to login after logout.
7. As admin, GET `/admin/category/delete?id=<existing-id>` must return 405 without deletion. POST without CSRF must return 403. As a regular user, Category/Product admin routes must return 403. Use disposable data for any successful mutation checks; do not trigger real OTP email as part of this audit.

To rerun the real-container checker in PowerShell after building, using the existing installation:

```powershell
$tomcatLib = 'C:/ServerWeb/apache-tomcat-11.0.25-windows-x64/apache-tomcat-11.0.25'
java -cp "target/classes;target/ServletCRUDMVC/WEB-INF/lib/*;$tomcatLib/lib/*;$tomcatLib/bin/tomcat-juli.jar" scripts/CheckSiteMeshTomcat.java
```
