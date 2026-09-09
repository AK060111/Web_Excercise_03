# Focused fullname and Product validation — 2026-09-09

## Fullname

Register controller/service and Profile service share `ValidationUtil.fullname`: NFC Unicode normalization, Unicode whitespace collapsed to one ordinary space, trimming, 1–255 UTF-16 units (compatible with existing database capacity), then Unicode letters/spaces only. Vietnamese composed and decomposed names work; digits, punctuation, symbols and blank names are rejected with Vietnamese messages. Invalid input remains available to the existing escaped JSP form fields. Normalized names reach both registration and profile persistence.

## Product findings and fixes

- Add: controller → ProductServiceImpl.validate → ProductDaoImpl.insert. Required trimmed name, max 255; BigDecimal > 0, maximum 9999999999999999.99, no rounding of excess fractional precision; numeric positive category ID must exist. Validation runs before upload. Images remain optional. Numeric form input now trims surrounding spaces.
- Edit: existing ID required; validation uses a separate input object before DAO mutation. No new image preserves the stored image. Failed saves clean new files and retain the old reference. Fixed form redisplay referencing a discarded new image after a failed save.
- Delete: existing admin filter, POST and CSRF remain enforced; invalid IDs return 400, missing rows 404, GET 405. Fixed database failure incorrectly opening an Add form: it now returns 503 without rendering a mutation form.
- Images: real image decoding, JPG/JPEG/PNG/WEBP for Product, 5 MB and 4096×4096 limits, generated contained paths, cleanup of newly saved files after failed operations. Successful replacement/deletion retains old files, consistent with the existing shared upload design; no unverified old/shared file is removed. This can leave unused files and would require separate reference-aware garbage collection.
- List/detail: inspected ProductController, admin controller, service, DAO fetch joins, and actual list/detail/form JSPs. IDs and missing records handled safely; stored name/description/category/image URL output escaped; public pagination remains six items per page with bounds and deterministic ordering. No pagination or list/detail changes needed.

## Files changed this follow-up

- `src/main/java/vn/iotstar/util/ValidationUtil.java`
- `src/main/java/vn/iotstar/service/impl/UserServiceImpl.java`
- `src/main/java/vn/iotstar/service/impl/UserProfileServiceImpl.java`
- `src/main/java/vn/iotstar/controller/RegisterController.java`
- `src/main/java/vn/iotstar/controller/ProductAdminController.java`
- `scripts/CheckProduct.java`
- `scripts/CheckProfile.java`
- `scripts/CheckSecurity.java`
- This report.

ProductAdminController retains its no-argument servlet constructor; a service constructor permits isolated tests without real database mutations. No SiteMesh, context.xml, credentials, mail configuration, authentication architecture, or web.xml changes. No commit/push.

## Validation results

`mvn clean package`: PASS, release 17, 45 compiled application classes. Working and staged `git diff --check`: PASS.

| Checker | PASS | FAIL |
|---|---:|---:|
| CheckProduct | 121 | 0 |
| CheckProduct --database | 134 | 0 |
| CheckProfile | 24 | 0 |
| CheckForgotPassword | 39 | 0 |
| CheckSecurity | 78 | 0 |
| CheckSiteMeshTomcat | 32 | 0 |
| Total | 428 | 0 |

Counts include repeated offline assertions in the database Product run. One existing symlink assertion skipped because Windows cannot create symlinks with this account. SQL tests use rollback; temporary Product/Profile upload files cleaned. No real email. Existing Jasper tooling compiled all 22 JSPs, and the unchanged real Tomcat profile rendering test passed. The final Product-only preview correction was followed by another clean build and both Product modes passing.

## Manual Tomcat checks

Republish the final WAR in the existing STS/Tomcat installation using the already validated Context setting. On Register, submit `Khoa123`, `Khoa@`, and `Nguyễn-Văn-An`; expect Vietnamese errors and retained escaped names without registration/email. On a test profile, try those invalid values, then a valid Vietnamese name with repeated spaces; verify normalized persistence and restore the original value afterward.

As admin, use disposable Product data: valid Add, invalid price/category/image, Edit without image, replacement image, detail/list display, and POST Delete. Verify form values/old image remain visible after errors and GET Delete cannot delete. Browser appearance and native multipart interaction in the user's current STS deployment remain manual; controller, DAO, upload and JSP checks above are automated.
