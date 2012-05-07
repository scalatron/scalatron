(function () {

    /* See webclient.html
    function getUserName() {
        return Ext.util.Cookies.get("scalatron-user");
    }
    */

    Ext.define('API', {
        singleton:true,

        init: function(callback) {
            Ext.Ajax.request({
                scope: this,
                url: "/api/users/" + getUserName() ,
                method:'GET',
                headers:{
                    "Accept":'application/json',
                    "Content-Type":'application/json'
                },
                success: function(r) {
                    var result = Ext.JSON.decode(r.responseText);
                    var api = {};

                    Ext.each(result.resources, function(e) {
                        api[e.name] = e.url;
                    });
                    this.api = api;

                    callback(this.api);
                }
            });
        },

        applyDefault: function(method, url, param) {
            param = param || {};

            var newParam = Ext.applyIf({}, {
                url: url,
                method: method,
                headers:{
                    "Accept":'application/json',
                    "Content-Type":'application/json'
                },
                success: function(r) {
                    if(param.success) {
                        var result = {};

                        if(r.responseText != "") {
                            try {
                                result = Ext.JSON.decode(r.responseText)
                            } catch(e) {
                                console.warn("URL: " + url + " Method: " + method);
                                console.warn("Cannot parse result, Test to parse is: ", r.responseText);

                            }
                        } else {
                            console.log("URL: " + url + " Method: " + method);
                            console.log("Empty result: ", r.responseText);
                        }

                        param.success(result);
                    }
                }
            });

            return Ext.applyIf(newParam, param);
        },

        /*
         { "name" : "Session",       "url" : "/api/users/{user}/session" },
         { "name" : "Sources",       "url" : "/api/users/{user}/sources" },
         { "name" : "Build",         "url" : "/api/users/{user}/sources/build" },
         { "name" : "Sandboxes",     "url" : "/api/users/{user}/sandboxes" },
         { "name" : "Publish",       "url" : "/api/users/{user}/unpublished/publish" }
         { "name" : "Versions",      "url" : "/api/users/{user}/versions" },
         { "name" : "Unpublished",   "url" : "/api/users/{user}/unpublished" },
         { "name" : "Published",     "url" : "/api/users/{user}/published" }
         */
        build:function (param) {
            var p = this.applyDefault("PUT", this.api.Build, param);
            Ext.Ajax.request(p);
        },

        // expects:
        // param.jsonData = { files: [ { filename: "Bot.scala", code: "// the source code" } ] }
        updateSourceFiles: function(param) {
            var p = this.applyDefault("PUT", this.api.Sources, param);
            Ext.Ajax.request(p);
        },

        loadBotSource: function(param) {
            var p = this.applyDefault("GET", this.api.Sources, param);
            Ext.Ajax.request(p);
        },

        createSandbox: function(param) {
            var p = this.applyDefault("POST", this.api.Sandboxes, param);
            Ext.Ajax.request(p);
        },

        destroySandboxes: function(param) {
            var p = this.applyDefault("DELETE", this.api.Sandboxes, param);
            Ext.Ajax.request(p);
        },

        nextStep: function(param) {
            var p = this.applyDefault("PUT", this.api.Sandboxes, param);
            p.jsonData = { steps: param.steps };
            Ext.Ajax.request(p);
        },

        publish: function(param) {
            var p = this.applyDefault("PUT", this.api.Publish, param);
            Ext.Ajax.request(p);
        },

        logout: function() {
            var p = this.applyDefault("DELETE", this.api.Session, {});
            Ext.Ajax.request(p);
        },

        enumVersions: function(param) {
            var p = this.applyDefault("GET", this.api.Versions, param);
            Ext.Ajax.request(p);
        },

        createVersion: function(param) {
            var p = this.applyDefault("POST", this.api.Versions, param);
            Ext.Ajax.request(p);
        }
    });

})();


