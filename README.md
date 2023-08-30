# lms-lti-crosslist
The Cross-listing Assistant in Canvas at Indiana University is a utility for combining enrollments from multiple course sections into a single primary course site, which reduces the administrative overhead of managing a separate version of the course for each section.

## Running standalone
Add env vars or system properties as desired.

| ENV Property                           | System Property                        | Default Value             | Description                                                                                                    |
|----------------------------------------|----------------------------------------|---------------------------|----------------------------------------------------------------------------------------------------------------|
| `APP_FULLFILEPATH`                     | `app.fullFilePath`                     | `/usr/src/app/config`     | Directory for configuration files                                                                              |
| `APP_OVERRIDESFILENAME`                | `app.overridesFileName`                | `overrides.properties`    | Customizable filename for additional configurations.  Would be located in the above directory.                 |
| `SPRING_PROFILES_ACTIVE`               | `spring.profiles.active`               |                           | Supply spring profiles to activate.  See configuration details below for potential values.                     |
| `APP_ENV`                              | `app.env`                              | `dev`                     | Environment designator.  Free-form and can be used for your own purposes.  Shows up in the application footer. |
| `LTI_CLIENTREGISTRATION_DEFAULTCLIENT` | `lti.clientregistration.defaultClient` | canvas                    | Specify the launching configuration to expect (canvas/saltire)                                                 |

## Setup Database
After compiling, see `target/generated-resources/sql/ddl/auto/postgresql9.sql` for appropriate ddl.
Insert a record into the `LTI_13_AUTHZ` table with your tool's registration_id (`lms_lti_crosslisting`), along with the client_id
and secret from Canvas's Developer Key.  An `env` designator is also required here, and allows a database to support
multiple environments simultaneously (dev and reg, for example).

## Test a local launch
Startup the application with the `LTI_CLIENTREGISTRATION_DEFAULTCLIENT` value set to `saltire`.
Use an LTI tool consumer launcher, like https://saltire.lti.app/platform.
Default values are fine, with the below exceptions...

In the `Message` section, set the following:
<table>
<tr><th>Property</th><th>Value</th></tr>
<tr><td>Custom parameters</td><td>

```
canvas_course_id=123456
canvas_user_login_id=johnsmith
instructure_membership_roles=http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor
```

</td></tr>
</table>

Use an appropriate `canvas_course_id` and `canvas_user_login_id`.

From the `Security Model` section, set the following:
<table>
<tr><th>Property</th><th>Value</th></tr>
<tr><td>LTI version</td><td>1.3.0</td></tr>
<tr><td>Message URL</td><td>http://localhost:8080/app/launch</td></tr>
<tr><td>Client ID</td><td>dev (or whatever is appropriate based on the record inserted in the database table from above)</td></tr>
<tr><td>Initiate login URL</td><td>http://localhost:8080/lti/login_initiation/lms_lti_crosslisting</td></tr>
<tr><td>Redirection URI(s)</td><td>http://localhost:8080/lti/login</td></tr>
</table>

## Canvas JSON
Example json for the tool can be found in the [examples](examples) directory.

## Configuration
If choosing to use properties files for the configuration values, the default location is `/usr/src/app/config`, but that can be overridden by setting the `APP_FULLFILEPATH` value via system property or environment variable.
You may use `security.properties`, `overrides.properties`, or set the `APP_OVERRIDESFILENAME` value with your desired file name.

### Canvas Configuration
The following properties need to be set to configure the communication with Canvas and Canvas Catalog.
They can be set in a properties file, or overridden as environment variables.

| Property             | Default Value               | Description                                               |
|----------------------|-----------------------------|-----------------------------------------------------------|
| `canvas.host`        |                             | Hostname of the Canvas instance                           |
| `canvas.sso.host`    |                             | Hostname of the Canvas OIDC auth domain                   |
| `canvas.baseUrl`     | https://`${canvas.host}`    | Base URL of the Canvas instance                           |
| `canvas.baseApiUrl`  | `${canvas.baseUrl}`/api/v1  | Base URL for the Canvas API                               |
| `canvas.token`       |                             | Token for access to Canvas instance                       |
| `canvas.accountId`   |                             | Your institution's root accountId in your Canvas instance |
| `catalog.baseUrl`    |                             | Base URL of the Canvas Catalog instance                   |
| `catalog.baseApiUrl` | `${catalog.baseUrl}`/api/v1 | Base URL for the Canvas Catalog API                       |
| `catalog.token`      |                             | Token for access to the Canvas Catalog instance           |

### Database Configuration
The following properties need to be set to configure the communication with a database.
They can be set in a properties file, or overridden as environment variables.

| Property             | Description                                                                                                            |
|----------------------|------------------------------------------------------------------------------------------------------------------------|
| `lms.db.user`        | Username used to access the database                                                                                   |
| `lms.db.url`         | JDBC URL of the database.  Will have the form `jdbc:<dbtype>://<host>:<port>/<database>`                               |
| `lms.db.password`    | Password for the user accessing the database                                                                           |
| `lms.db.poolType`    | Fully qualified name of the connection pool implementation to use. By default, it is auto-detected from the classpath. |

### Configure support contact information
The following properties need to be set to configure the contact information on the global error page.
They can be set in a security.properties file, or overridden as environment variables.

| Property                | Description                                                                                               |
|-------------------------|-----------------------------------------------------------------------------------------------------------|
| `lti.errorcontact.name` | Display name for your support organization                                                                |
| `lti.errorcontact.link` | Contact mechanism - URL or mailto:email (e.g. `http://support.school.edu` or `mailto:support@school.edu`) |

### Redis Configuration (optional)
If you would like to use Redis for session storage, you will need to enable it by including the value `redis-session` into the `SPRING_PROFILES_ACTIVE` environment variable. Be aware that if the tool requires multiple values, that there could be more than one profile value in there.

Additionally, the following properties need to be set to configure the communication with Redis.
Then can be set in a properties file, or overridden as environment variables.

| Property                | Description                                    |
|-------------------------|------------------------------------------------|
| `spring.redis.host`     | Redis server host.                             |
| `spring.redis.port`     | Redis server port.                             |
| `spring.redis.database` | Database index used by the connection factory. |
| `spring.redis.password` | Login password of the redis server.            |


### Vault Configuration (optional)
If you would like to use HasiCorp's Vault for secure property storage, you will need to enable it by including the value `vault` into the `SPRING_PROFILES_ACTIVE` environment variable. Be aware that if the tool requires multiple values, that there could be more than one profile value in there.
Include any `spring.cloud.vault.*` properties that your environment requires in a properties file, or override as environment variables.

### Exposing the LTI authz REST endpoints
If you would like to expose the LTI authz endpoints in this tool (for CRUD operations on the LTI authorizations), you will
need to enable it by including the value `ltirest` into the `SPRING_PROFILES_ACTIVE` environment variable. Be aware that
if the tool requires multiple values, that there could be more than one profile value in there.

#### Enabling swagger-ui for the LTI authz REST endpoints
:warning: Experimental :warning:

If you would like to enable the swagger-ui for interacting with the endpoints, include the value `swagger` into the `SPRING_PROFILES_ACTIVE` environment variable.
Once enabled, the ui will be available at `/api/lti/swagger-ui.html`.  There are some additional OAuth2 considerations
that need to be accounted for while using this setup.

This is marked as experimental due to the fact that we aren't running with this option at IU.  We are running into CORS
issues when trying to talk to our OAuth2 service via swagger, so we can't verify if it really works or not!
