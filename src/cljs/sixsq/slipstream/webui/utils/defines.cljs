(ns sixsq.slipstream.webui.utils.defines
  "Parameter definitions for controlling the behavior of the application.
   These primarily capture differences between development and production
   builds.")

(goog-define
  ^{:documentation
    "debugging log level

    The default level is 'info', which logs most important actions, but
    does not log HTTP requests to the server. Switch to 'debug' for more
    detailed logging.

    Set the value like this:

    {:compiler-options
      {:closure-defines
        {'sixsq.slipstream.webui.webui.defines/LOGGING_LEVEL \"info\"}}
    "}
  LOGGING_LEVEL "info")

(goog-define
  ^{:documentation
    "determine the host url

     Set a fixed SlipStream endpoint (useful for development) with:

     {:compiler-options
       {:closure-defines
         {'sixsq.slipstream.webui.webui.defines/HOST_URL \"https://nuv.la\"}}

     NOTE: When using an endpoint other than the one serving the javascript code
           you MUST turn off the XSS protections of the browser."}
  HOST_URL "")

;;
;;
(goog-define
  ^{:documentation
    "determine the web application prefix

     The default is to concatenate '/webui' to the end of the SLIPSTREAM_URL.
     If the application is mounted elsewhere, you can change the default with:

     {:compiler-options
       {:closure-defines
         {'sixsq.slipstream.webui.webui.defines/CONTEXT \"\"}}
     "}
  CONTEXT "/webui")

