SCALATRON - Learn Scala With Friends
http://scalatron.wordpress.com - Twitter: @scalatron / #scalatron
This work is licensed under the Creative Commons Attribution 3.0 Unported License.

Developer Documentation - Scalatron APIs
Version 0.9.8 -- updated 2012-04-22



# Overview

## Scalatron Bot "API"

The interaction between bots and the game server, which in some sense could also be seen as
an API, is not covered by this document. For information on that area of Scalatron, see the
*Scalatron Protocol* document.



## Scalatron Scala API

The Scalatron source code contains an API entry point via the `Scalatron` trait. This native
Scala API exposes methods and objects that provides access to most important states and
operations of the server. For example:

* for managing (web) users;
* for updating source code in a local workspace;
* for building source code in a local workspace code into bots;
* for launching sandboxed, private game rounds that load the local bot version;
* for publishing the local bot version into the tournament plug-in directory;
* for creating, deleting and listing sample bots;
* and for tracking the state of the tournament loop.

It is NOT intended for server/bot interaction. For For information on that area of Scalatron, see
the *Scalatron Protocol* document.



## Scalatron RESTful web API

The Scalatron server includes a web server, which does not just serve the browser-based user
interface but also exposes the functionality of the Scalatron Scala API as a RESTful web API.

You can use this API to create your own tools, like a tournament leaderboard tracker, integration
into your IDE or writing your own client IDE. The resources and verbs of this API are described
in the following section.

The API design was inspired by a variety of blog entries, including:

* http://www.peej.co.uk/articles/restfully-delicious.html
* http://jcalcote.wordpress.com/2008/10/16/put-or-post-the-rest-of-the-story/

Important Note:

* the web server and REST API are intended for serving information to local users during a
  workshop in which all participants are in the same room. It is neither designed for nor
  appropriate for serving sessions over the public Internet or to an audience that includes
  untrusted users. User authentication is insecure and intended for organizational purposes
  only - it is not designed for or capable of keeping out users with malicious intent.


## List of Resources & Implementation Status

    status  url                                     verb    input   output  success failures        description
    ------- --------------------------------------- ------- ------- ------- ------- --------------- --------------------------------
    tested  /api                                    GET     -       json    200     -               Entry Point
    tested  /api/users                              GET     -       json    200     -               Get Existing Users

    tested  /api/users/{user}/session               POST    json    json    200     401,404,500     Log-In As User
    tested  /api/users/{user}/session               DELETE  -       -       204     401,404         Log-Off As User

    tested  /api/users                              POST    json    json    200     400,401,403,500 Create User
    tested  /api/users/{user}                       DELETE  -       -       204     400,401,404,500 Delete User
    tested  /api/users/{user}                       PUT     json    -       204     400,401,404     Update User Attributes
    tested  /api/users/{user}                       GET     -       json    200     400,401,404,500 Get User Attributes & Resources

    tested  /api/users/{user}/sources               GET     -       json    200     401,404,500     Get Source Files
    tested  /api/users/{user}/sources               PUT     json    -       204     401,404,500     Update Source Files
    tested  /api/users/{user}/sources/build         PUT     -       json    200     401,404,415     Build Source Files into Unpublished Bot

    n/a     /api/users/{user}/unpublished           PUT     jar     -       204     401,415         Upload Unpublished Bot
    n/a     /api/users/{user}/unpublished           GET     -       jar     200     401,404         Download Unpublished Bot

    tested  /api/users/{user}/unpublished/publish   PUT     -       -       204     401,404         Publish Unpublished Bot
    n/a     /api/users/{user}/published             PUT     jar     -       204     401,415         Publish Bot Via Upload

    tested  /api/users/{user}/sandboxes             POST    json    json    200     401,415         Create Sandbox
    untstd  /api/users/{user}/sandboxes             DELETE  -       -       204     401,404         Delete All Sandboxes
    tested  /api/users/{user}/sandboxes/{id}/{time} GET     -       json    200     401,404        Get Sandbox State

    tested  /api/users/{user}/versions              GET     -       json    200     401,404         Get Existing Versions
    tested  /api/users/{user}/versions              POST    json    url     201     401,415         Create Version
    n/a     /api/users/{user}/versions/{versionId}  GET     -       json    200     401,404         Get Version Files
    n/a     /api/users/{user}/versions/{versionId}  DELETE  -       -       204     401,404         Delete Version

    n/a     /api/samples                            GET     -       json    200     -               Get Existing Samples
    n/a     /api/samples                            POST    json    -       201     401,415         Create Sample
    n/a     /api/samples/{sample}                   GET     -       json    200     401,404         Get Sample Files
    n/a     /api/samples/{sample}                   DELETE  -       -       204     401,404         Delete Sample

    n/a     /api/tournament                         GET     -       json    200     -               Get Tournament Resources
    n/a     /api/tournament/stats                   GET     -       json    200     -               Get Tournament Stats







## Entry Point

### Get Available Resources

Returns urls pointing to the primary resources exposed by the web API, such as `users`,
`samples` and `tournament`.

* URL:              /api
* Method:           GET
* Returns:          200 OK & JSON
* Authentication:   no need to be logged in

Response JSON example:

    {
        "version" : "0.9.7",
        "resources" :
        [
            { "name" : "Users",         "url" : "/api/users"},
            { "name" : "Samples",       "url" : "/api/samples"},
            { "name" : "Tournament",    "url" : "/api/tournament"}
        ]
    }


### Get Existing Users

Returns a list of all user accounts that currently exist on the server. Accounts can be created
by the Administrator, either using the appropriate API call or via the browser-based UI.

* URL:              /api/users
* Method:           GET
* Returns:          200 OK & JSON
* Authentication:   no need to be logged in

Response JSON example:

    {
        "users" :
        [
            { "name" : "Administrator", "session" : "/api/users/Administrator/session", "resource" : "/api/users/Administrator"},
            { "name" : "Frank",         "session" : "/api/users/Frank/session",         "resource" : "/api/users/Frank"},
            { "name" : "Daniel",        "session" : "/api/users/Daniel/session",        "resource" : "/api/users/Daniel"},
            { "name" : "{user}",        "session" : "/api/users/{user}/session"         "resource" : "/api/users/{user}"}
        ]
    }






## Authentication

Before a user can perform operations on its resources, she must be authenticated on the system.
This is done by logging in with a password (which may be empty). A session is terminated by
logging off.

### Log-In As User

Authenticates the caller as a particular user. The password must match the password configured
for this user on the server, which may be empty.

If successful, returns a list of resources available for the user, such as the source code;
labeled versions of the source code; the unpublished, private bot plug-in .jar file; the user's
private sandbox for testing bots; and the bot plug-in .jar file the user published into the
tournament loop.

If the user was previously logged in as another user, this operation automatically logs the
user off first.

* URL:              /api/users/{user}/session
* Method:           POST
* Request Body:     JSON
* Result Body:      JSON
* Returns:
    * 200 OK & JSON (success)
    * 401 Unauthorized (wrong password)
    * 404 Not Found (if user does not exist)
    * 500 Internal Server Error (user configuration file not found)
* Authentication:   no need to be logged in

Request JSON example:

    {
        "password" : ""
    }

Response JSON example:

    {
        "resources" :
        [
            { "name" : "Sources",       "url" : "/api/users/{user}/sources" },
            { "name" : "Build",         "url" : "/api/users/{user}/sources/build" },
            { "name" : "Sandboxes",     "url" : "/api/users/{user}/sandboxes" },
            { "name" : "Publish",       "url" : "/api/users/{user}/unpublished/publish" }
            { "name" : "Versions",      "url" : "/api/users/{user}/versions" },
            { "name" : "Unpublished",   "url" : "/api/users/{user}/unpublished" },
            { "name" : "Published",     "url" : "/api/users/{user}/published" }
        ]
    }

Comments:

* The HTTP verb is POST because conceptually a new session is created for the user.


### Log-Off As User

Deauthenticates the caller from a particular user account. Callers must log-out before they can
log in as another user, for example when switching between the Administrator account and another
user's account.

* URL:              /api/users/{user}/session
* Method:           DELETE
* Returns:
    * 204 No Content
    * 401 Unauthorized (not the currently logged in user)
    * 404 Not Found (if user does not exist)
* Authentication:   must be logged on as that user

Comments:

* The HTTP verb is DELETE because conceptually the current session is deleted for the user.






## User Management

Users for the purposes of this API are players connecting to the Scalatron server in order to
participate in the tournament.

User accounts need to exist for a user to be able to log in. Accounts can be created by the
Administrator, either using the appropriate API call or via the browser-based UI.

The Scalatron server maintains a private workspace for each user on the server computer.
The workspace contains several bits of information, such as the users current source code;
optional labeled versions of the source code; and the most recently built bot plug-in .jar
file. For details about the locations of these items on disk on the server, please refer to
the documentation of the Scalatron Scala API.



### Create User

Creates a new user account for a given user name and a given password. Certain restrictions
apply to the user name, which are described in the documentation of the Scalatron Scala API.
The characters `.` and `/` are not permitted, for example. The password may be empty, in
which case the new user will be able to log in without being prompted for a password.

* URL:              /api/users
* Method:           POST
* Request Body:     JSON
* Result Body:      JSON
* Returns:
    * 200 Created & JSON (success)
    * 400 Bad Request (if the user name was invalid; reason is provided)
    * 401 Unauthorized (if not logged on as Administrator)
    * 403 Forbidden (if a user with the given name already exists)
    * 500 Internal Server Error (if user workspace could not be created on disk)
* Authentication:   must be logged on as Administrator

Request JSON example:

    {
        "name" : "{user}",
        "password" : ""
    }

Result JSON example:

    {
        "name" : "{user}",
        "session" : "/api/users/{user}/session",
        "resource" : "/api/users/{user}"
    }


Comments:

* On success, a 201 Created response is returned along with a location header containing the
  URL of the newly created user resource: "/api/users/{user}"
* The HTTP verb is POST because a new resource is created which did not previously exist.
  The operation is not idempotent.



### Delete User

Removes a user account from the system, deleting all associated content (source code,
versions, private bot plug-in .jar, published bot plug-in .jar).

* URL:              /api/users/{user}
* Method:           DELETE
* Returns:
    * 204 No Content (success)
    * 400 Bad Request (if the user name was invalid; reason is provided)
    * 401 Unauthorized (if not logged on as that user or as Administrator)
    * 404 Not Found (if user does not exist)
    * 500 Not Found (if user does not exist)
* Authentication:   must be logged on as Administrator

Comments:

* The HTTP verb is DELETE because a resource is deleted. The operation is not idempotent.



### Update User Attributes (including password)

Updates one or more attributes for a particular user, which are recorded in the user's configuration
file. Can only be invoked by the user who owns the account and by the Administrator.

* URL:              /api/users/{user}
* Method:           PUT
* Request Body:     JSON
* Returns:
    * 204 No Content (success)
    * 400 Bad Request (if the user name was invalid; reason is provided)
    * 401 Unauthorized (if not logged on as Administrator or as the user in question)
    * 404 Not Found (if user does not exist)
* Authentication:   must be logged on as Administrator or as the user in question

Request JSON example:

    {
        "attributes" :
        [
            { "name" : "keyA",       "value" : "valueA" },
            { "name" : "keyB",       "value" : "valueB" }
        ]
    }

Comments:

* Since we don't want people to create new users using PUT, only update existing users, we'll
  return a 404 Not Found error if the resource doesn't already exist.
* The HTTP verb is PUT because a resource is updated and the operation is idempotent.




### Get User Attributes & Resources

Returns the attributes stored for a particular user in the user's configuration file,
as well as the resources available for this user account.
Can only be invoked by the user who owns the account and by the Administrator.
The user's password is never returned.

* URL:              /api/users/{user}
* Method:           GET
* Request Body:     JSON
* Returns:
    * 200 OK & Result (success)
    * 400 Bad Request (if user name contained invalid characters)
    * 401 Unauthorized (if not logged on as Administrator or as the user in question)
    * 404 Not Found (if user does not exist)
    * 500 Internal Server Error (configuration attribute file could not be read or was invalid)
* Authentication:   must be logged on as Administrator or as the user in question

Request JSON example:

    {
        "attributes" :
        [
            { "name" : "keyA",       "value" : "valueA" },
            { "name" : "keyB",       "value" : "valueB" }
        ],
        "resources" :
        [
            { "name" : "Sources",       "url" : "/api/users/{user}/sources" },
            { "name" : "Build",         "url" : "/api/users/{user}/sources/build" },
            { "name" : "Sandboxes",     "url" : "/api/users/{user}/sandboxes" },
            { "name" : "Publish",       "url" : "/api/users/{user}/unpublished/publish" }
            { "name" : "Versions",      "url" : "/api/users/{user}/versions" },
            { "name" : "Unpublished",   "url" : "/api/users/{user}/unpublished" },
            { "name" : "Published",     "url" : "/api/users/{user}/published" }
        ]
    }

Comments:

* Since we don't want people to create new users using PUT, only update existing users, we'll
  return a 404 Not Found error if the resource doesn't already exist.
* The HTTP verb is PUT because a resource is updated and the operation is idempotent.









## User-Sources Management

Manages the currently active versions of the source code files. The workspace on the server
can contain several source files, which are generally expected and returned as a collection
of (filename, code) string pairs. Currently files are restricted to all reside in the same
directory, which no subdirectories permitted (this may change in the future).

Note also that the browser-based Scalatron IDE currently only provides access to a single
source code file, which is expected to have the name `Bot.scala`. If more than that one
file is present, the behavior of the browser UI is undefined. This may change in the future.


### Get Source Files

Retrieves the currently active versions of the source code files present in the user's
workspace on the server. You might use this to retrieve the user's source code for display
in an editor. The returned files are the files that were most recently written to disk on
the server either when the user account was initialized or when an upload/update was performed
via one of the APIs.

* URL:              /api/users/{user}/sources
* Method:           GET
* Returns:
    * 200 OK & JSON (success)
    * 401 Unauthorized (if not logged on as {user})
    * 404 Not Found (if user does not exist)
    * 500 Internal Server Error (error reading source files from disk)
* Authentication:   must be logged on as Administrator or as {user}

Response JSON example:

    {
        "files" :
        [
            { "filename" : "Bot.scala", "code" : "class ControlFunctionFactory { ... }" },
            { "filename" : "Util.scala", "code" : "class View { ... }" }
        ]
    }



### Update Source Files

Updates the source code files in the user's workspace on the server.

* URL:              /api/users/{user}/sources
* Method:           PUT
* Request Body:     JSON
* Returns:
    * 204 No Content (success)
    * 401 Unauthorized (if not logged on as that user)
    * 404 Not Found (if user does not exist)
    * 500 Internal Server Error (error writing source files to disk)
* Authentication:   must be logged on as that user

Request JSON example:

    {
        "files" :
        [
            { "filename" : "Bot.scala", "code" : "class ControlFunctionFactory { ... }" },
            { "filename" : "Util.scala", "code" : "class View { ... }" }
        ]
    }

Comments:

* Since we don't want people to create new users using PUT, only update existing source
  files, we'll return a 404 Not Found error if the resource doesn't already exist.
* The HTTP verb is PUT because a resource is updated and the operation is idempotent.
  The source files are treated like a property of the user object. Updating the sources files
  updates that property. The operation is idempotent because the same source files can be
  PUT multiple times, always resulting in the same final state.



### Build Source Files into Unpublished Bot

Builds the currently present source files of the user into a private, unpublished bot plug-in
.jar file. The invocation returns the output of the Scala compiler in a way that allows an
editor to jump to the associated source code location.

Note: you may want to update the user's current source code beforehand by uploading the most
recent version using PUT to /api/users/{user}/sources.

* URL:              /api/users/{user}/sources/build
* Method:           PUT
* Returns:
    * 200 OK & Build Results
    * 401 Unauthorized (if not logged on as that user)
    * 404 Not Found (if user does not exist)
    * 500 Internal Server Error (compile service unavailable, disk error, etc.)
* Authentication:   must be logged on as that user

Result JSON example:

    {
        "successful" : true,
        "errors" : 1,
        "warnings" : 1,
        "messages" :
        [
            { "filename" : "Bot.scala", "line" : 1, "column" : 8, "severity": 1, "message" : "some compiler warning message" },
            { "filename" : "Bot.scala", "line" : 2, "column" : 4, "severity": 2, "message" : "some compiler error message" }
        ]
    }

Comments:

* The HTTP verb is PUT because a resource is updated and the operation is idempotent.
  The unpublished bot is treated like a property of the user object. Updating the unpublished
  bot by building from sources updates that property. The operation is idempotent because
  you can build a bot (from the same sources) multiple times, always resulting in the same
  final state.
* The method could also have been PUT on just /api/users/{user}/sources, but that method is
  reserved for uploading bots that were built client-side.





## User-Unpublished-Bot Management

Manages the user's private, unpublished bot plug-in .jar file. This file can either be generated
by building from sources on the server or by uploading a pre-built file via the API. Once the
file is present on the server, it can be published into the tournament loop or used to run a
private test game in a sandbox.


### Upload Unpublished Bot

Uploads an already-build bot version to the server. The uploaded file will be placed into the
location where the server expects to find the private, unpublished bot plug-in .jar file. Use
this to upload a bot that was built client-side.

* URL:              /api/users/{user}/unpublished
* Method:           PUT
* Request Body:     application/java-archive
* Returns:
    * 204 No Content (success)
    * 401 Unauthorized (if not logged on as that user)
    * 415 Unsupported Media Type (malformed request, or other problem)
* Authentication:   must be logged on as that user

Comments:

* The HTTP verb is PUT because a resource is updated and the operation is idempotent.
  The unpublished bot is treated like a property of the user object. Updating the unpublished
  bot by uploading it to the server updates that property. The operation is idempotent because
  you can upload the same bot multiple times, always resulting in the same final state.


### Download Unpublished Bot (Not Implemented)

Downloads the current version of the private, unpublished bot plug-in .jar file.
This allows client-side tools to use the Scalatron server as a Scala build service:
update sources, build sources, download unpublished bot.

* URL:              /api/users/{user}/unpublished
* Method:           GET
* Result Body:      application/java-archive
* Returns:
    * 501 Not Implemented
    Future:
    * 200 OK & Result (success)
    * 401 Unauthorized (if not logged on as that user)
    * 404 Not Found (no unpublished bot present in workspace)
* Authentication:   must be logged on as that user

Comments:

* Currently not supported, maybe in a future version. But this is where it would be.






## User-Publication Management

Manages the bot currently published by the user into the tournament loop.


### Publish Unpublished Bot

Publishes the unpublished bot version residing in the user's workspace into the tournament by
copying the bot plug-in .jar file into the tournament bot directory appropriate for the user.

Note: you may need to update the user's bot beforehand, either by uploading a .jar file built
client-side via PUT to /api/users/{user}/unpublished or by building from sources (potentially
also to be uploaded first) via PUT to /api/users/{user}/sources/build

* URL:              /api/users/{user}/unpublished/publish
* Method:           PUT
* Returns:
    * 204 No Content (success)
    * 401 Unauthorized (if not logged on as that user)
    * 404 Not Found (if there is no unpublished bot plug-in .jar file)
    * 500 Internal Server Error (problems copying the bot on disk)
* Authentication:   must be logged on as that user

Comments:

* The HTTP verb is PUT because a resource is updated and the operation is idempotent.
  The published bot is treated like a property of the user object. Updating the published
  bot by publishing the unpublished bot updates that property. The operation is idempotent
  because you can publish the same unpublished bot multiple times, always resulting in the
  same final state.
* In a future version, we could return a link to "/api/users/{user}/published/stats"
  and use that to provide stats on the current user's bot's performance in the tournament. This
  differs from the "/api/tournament" resource, since the latter provides stats for all users.




### Publish Bot Via Upload (Not Implemented)

Uploads an already-build bot version into the tournament bot directory appropriate for the user
on the server. The uploaded file will be placed into the location where the server expects to
find the published bot plug-in .jar file. Use this to upload a bot that was built client-side.

* URL:              /api/users/{user}/unpublished
* Method:           PUT
* Request Body:     application/java-archive
* Returns:
    * 501 Not Implemented
    Future:
    * 204 No Content (success)
    * 401 Unauthorized (if not logged on as that user)
    * 415 Unsupported Media Type (malformed request, or other problem)
* Authentication:   must be logged on as that user

Comments:

* The HTTP verb is PUT because a resource is updated and the operation is idempotent.
  The published bot is treated like a property of the user object. Updating the published
  bot by uploading a new .jar file updates that property. The operation is idempotent
  because you can upload the same bot .jar file multiple times, always resulting in the
  same final state.







## User-Sandbox Management

Manages the currently active sandbox of the user. The sandbox is a private, single-steppable
game started for a particular user which loads the private, unpublished bot of the user. The
server holds the state of the game, which can be updated via API calls. The server also
provides information about the state of the user's entities (bots and mini-bots) in the
game, such as the most recent:

* control function input (which contains the view the bot has of its environment, as well
  as the bot state parameters);
* control function input (which contains the response of the bot);
* debug output generated by the bot via the `Log()` opcode.


### Create Sandbox

Creates a new, private sandbox run for the given user by launching a simulation run using
the bot plug-in .jar file currently present in the user's workspace, as well as the given
configuration parameters.

Note: you may need to update the user's bot beforehand, either by uploading a .jar file built
client-side via PUT to /api/users/{user}/unpublished or by building from sources (potentially
also to be uploaded first) via PUT to /api/users/{user}/sources/build

* URL:              /api/users/{user}/sandboxes
* Method:           POST
* Request Body:     JSON
* Returns:
    * 200 OK & Result
    * 401 Unauthorized (if not logged on as that user)
    * 404 Not Found (if there is no unpublished bot plug-in .jar file)
    * 500 Internal Server Error (failed to create sandboxed game)
* Authentication:   must be logged on as that user

Request JSON example:

    {
        "config" :
        {
            "-perimeter" : "open",
            "-walls" : "30",
            "-snorgs" : "200",
        }
    }

Result JSON example:

    {
        "id" : {id},    // the ID of the sandbox instance

        "url":         "/api/users/{user}/sandboxes/{id}/{time+0}",      // call to re-fetch this
        "urlPlus1":    "/api/users/{user}/sandboxes/{id}/{time+1}",      // call to single-step
        "urlPlus2":    "/api/users/{user}/sandboxes/{id}/{time+2}",      // call to step 2x
        "urlPlus10":   "/api/users/{user}/sandboxes/{id}/{time+10}",     // call to step 10x

        "time" : "0",
        "entities" :
        [
            "entity" :
            {
                "id": 33,
                "name": "Daniel",
                "master": true,
                "debugOutput": "line 1\nline 2\nline 3",
                "input" :
                {
                    "opcode" : "React",
                    "params" :
                    [
                        { "name" : "energy", "value": 1200 },
                        { "name" : "time", "value": 0 },
                        ... other opcode parameters
                    ]
                },
                "output" :
                [
                    {
                        "opcode" : "Move",
                        "params" :
                        [
                            { "name" : "direction", "value" : "2:3" }
                            ... other opcode parameters
                        ]
                    },
                    ... other commands
                ]
            }
        ]
    }

Comments:

* The HTTP verb is POST because each instance of a sandbox simulation is treated like a distinct
  resource (identified by a user-unique id, which is incorporated into the url in the result).
* Each simulation step has an associated time index and in theory (if users' bot control
  functions do not hold local state in the plug-in, such as a randomizer) iterating a simulation
  step is idempotent. Even if this does not always hold and "rewinding" a simulation may therefore
  not work, for the purposes of the API simulation steps are treated as distinct resources.
  However, only one step is guaranteed to be valid and retrievable at any one time, via its
  distinct URL. Once that was retrieved, only the URLs returned within that state are valid
  next URLs (see below).
* in the result JSON, the user's bots are returned in a collection "userbots" within an outer
  collection, "entities". This gives us the option, in the future, to return additional entity
  lists like "otherbots", "beasts", "plants", etc. to provide a fuller simulation state.



### Get Sandbox State

Retrieves the state of the sandbox with the given ID at the given step/time.

Note: for the purposes of the API, simulation steps (times) are treated as distinct resources.
However, only certain times and one sandbox id are guaranteed to be valid and retrievable at
any one time, namely those returned as follow-on URLs by a sandbox resource. Once one of
those resources was retrieved, only the next follow-on URLs returned there are valid
options.

* URL:              /api/users/{user}/sandboxes/{id}/{time}
* Method:           GET
* Returns:
    * 200 OK & Result
    * 401 Unauthorized (if not logged on as that user)
    * 404 Not Found (if the sandbox id or time are not valid)
* Authentication:   must be logged on as that user

Result JSON example:

    see Result JSON of "Create Sandbox" / POST to "/api/users/{user}/sandboxes"



### Delete All Sandboxes

Deletes all private sandboxes currently held by the server for the given user.

* URL:              /api/users/{user}/sandboxes
* Method:           DELETE
* Returns:
    * 204 No Cortent (success)
    * 401 Unauthorized (if not logged on as that user)
    * 404 Not Found (if the user does not exist)
* Authentication:   must be logged on as that user





## User-Version Management

Since the user may be working in a browser with no immediate local saving capabilities, the
server provides a simple version control system. Versions are simply packages of source files
maintained under an associated ID and with an optional label.

Note that the browser-based Scalatron IDE does not yet currently support creation and retrieval
of versions; this may change in the future.



### Get Existing Versions

Lists all versions currently available on the server for this particular user. Each version
is exposed as a separate resource.

* URL:              /api/users/{user}/versions
* Method:           GET
* Returns:
    * 200 OK & JSON (success)
    * 401 Unauthorized (if not logged on as that user)
    * 404 Not Found (if user does not exist)
    * 500 Internal Server Error (if version information could not be read from disk)
* Authentication:   must be logged on as that user

Response JSON example:

    {
        "versions" :
        [
            { "id" : "1", "label" : "Label 1", "date" : "1234556", "url" : "/api/users/{user}/versions/1" },
            { "id" : "2", "label" : "Label 2", "date" : "1234556", "url" : "/api/users/{user}/versions/2" },
            { "id" : "3", "label" : "Label 3", "date" : "1234556", "url" : "/api/users/{user}/versions/3" }
        ]
    }

Note that the returned "date" values are the number of milliseconds since the standard base time known as
"the epoch", namely January 1, 1970, 00:00:00 GMT. Put this into a Long value and use the Java new Date(Long)
constructor to work with the value.


### Create Version

Creates a new version on the server from the given source files.

* URL:              /api/users/{user}/versions
* Method:           POST
* Request Body:     JSON
* Returns:
    * 201 Created & Location (/api/users/{user}/versions/{id})
    * 401 Unauthorized (if not logged on as that user)
    * 415 Unsupported Media Type (malformed request, or version already exists, or other problem)
* Authentication:   must be logged on as that user

Request JSON example:

    {
        "label" : "Label 1",
        "files" :
        [
            { "filename" : "Bot.scala", "code" : "class ControlFunctionFactory { ... }" },
            { "filename" : "Util.scala", "code" : "class View { ... }" }
        ]
    }

Response JSON example:

    { "id" : "1", "label" : "Label 1", "date" : "1234556", "url" : "/api/users/{user}/versions/1" },

Note that the returned "date" values are the number of milliseconds since the standard base time known as
"the epoch", namely January 1, 1970, 00:00:00 GMT. Put this into a Long value and use the Java new Date(Long)
constructor to work with the value.

Comments:

* On success, a 201 Created response is returned along with a location header containing the
  URL of the newly created version resource: "/api/users/{user}/versions/{versionId}"
* The HTTP verb is POST because a new resource (the version) is created.




### Get Version Files

Retrieves the source files associated with a particular version ID.

* URL:              /api/users/{user}/versions/{versionId}
* Method:           GET
* Returns:
    * 200 OK & JSON (success)
    * 401 Unauthorized (if not logged on as that user)
    * 404 Not Found (if user or versionId does not exist)
* Authentication:   must be logged on as that user

Response JSON example:

    {
        "files" :
        [
            { "filename" : "Bot.scala", "code" : "class ControlFunctionFactory { ... }" },
            { "filename" : "Util.scala", "code" : "class View { ... }" }
        ]
    }



### Delete Version

Delets a particular version on the server.

* URL:              /api/users/{user}/versions/{versionId}
* Method:           DELETE
* Returns:
    * 204 No Content (success)
    * 401 Unauthorized (if not logged on as that user)
    * 404 Not Found (if user or version does not exist)
* Authentication:   must be logged on as that user








## Sample Management

The server provides a repository of code samples, which are source code packages accessible for
all users. The primary intent is to serve up the source code for the bots developed throughout
the tutorial. However, it looks like this would be a useful mechanism for participants for
sharing bot code through the server.


### Get Existing Samples

Lists all samples currently available on the server.

* URL:              /api/samples
* Method:           GET
* Returns:          200 OK & JSON
* Authentication:   no need to be logged in

Response JSON example:

    {
        "samples" :
        [
            { "name" : "Tutorial Bot 01", "url" : "/api/samples/Tutorial%20Bot%2001"},
            { "name" : "Tutorial Bot 02", "url" : "/api/samples/Tutorial%20Bot%2002"},
            { "name" : "Tutorial Bot 03", "url" : "/api/samples/Tutorial%20Bot%2003"},
        ]
    }



### Create Sample

Creates a new sample from the given source code files.

* URL:              /api/samples
* Method:           POST
* Request Body:     JSON
* Returns:
    * 201 Created & Location (/api/samples/{sample})
    * 401 Unauthorized (if not logged on as any user)
    * 415 Unsupported Media Type (malformed request, or sample already exists, or other problem)
* Authentication:   must be logged on as some user (any user is OK)

Request JSON example:

    {
        "name" : "Tutorial Bot 03",
        "files" :
        [
            { "filename" : "Bot.scala", "code" : "class ControlFunctionFactory { ... }" },
            { "filename" : "Util.scala", "code" : "class View { ... }" }
        ]
    }

Comments:

* On success, a 201 Created response is returned along with a location header containing the
  URL of the newly created sample resource: "/api/samples/{sample}"



### Get Sample Files

Returns the source code files for a particular sample.

* URL:              /api/samples/{sample}
* Method:           GET
* Returns:
    * 200 OK & JSON (success)
    * 401 Unauthorized (if not logged on as any user)
    * 404 Not Found (if sample does not exist)
* Authentication:   must be logged on as some user (any user is OK)

Response JSON example:

    {
        "files" :
        [
            { "filename" : "Bot.scala", "code" : "class ControlFunctionFactory { ... }" },
            { "filename" : "Util.scala", "code" : "class View { ... }" }
        ]
    }



### Delete Sample

Deletes a sample from the server, including all associated source code files.

* URL:              /api/samples/{sample}
* Method:           DELETE
* Returns:
    * 204 No Content (success)
    * 401 Unauthorized (if not logged on as Administrator)
    * 404 Not Found (if sample does not exist)
* Authentication:   must be logged on as Administrator







## Tournament Management

This API section provides access to the state of the tournament loop. The loop generally runs
in the foreground, with its output displayed on a projected screen (although a headless mode is
also available). The stat includes information such as the leader board. At this point, the API
does not yet expose the state of the game currently in progress, such as the scores of the
bots in the current game round.


### Get Tournament Resources

Returns links to the resources available for tournament management.

* URL:              /api/tournament
* Method:           GET
* Returns:          200 OK & JSON
* Authentication:   no need to be logged in

Response JSON example:

    {
        "resources" :
        [
            { "name" : "Stats",         "url" : "/api/tournament/stats"}
        ]
    }


### Get Tournament Stats

Returns the number of rounds played so far as well as the current leader board.

* URL:              /api/tournament/stats
* Method:           GET
* Returns:          200 OK & JSON
* Authentication:   no need to be logged in

Response JSON example:

    {
        "roundsPlayed" : 15,
        "leaderBoard" :
        {
            "last1" :
            [
                { "name" : "Daniel", "score" : 2000 },
                { "name" : "Frank",  "score" : 1000 },
                { "name" : "Linus",  "score" : 500 },
            ],
            "last5" :
            [
                { "name" : "Daniel", "score" : 2000 },
                { "name" : "Frank",  "score" : 1000 },
                { "name" : "Linus",  "score" : 500 },
            ],
            "last20" :
            [
                { "name" : "Daniel", "score" : 2000 },
                { "name" : "Frank",  "score" : 1000 },
                { "name" : "Linus",  "score" : 500 },
            ],
            "alltime" :
            [
                { "name" : "Daniel", "score" : 2000 },
                { "name" : "Frank",  "score" : 1000 },
                { "name" : "Linus",  "score" : 500 },
            ],
        }
    }

