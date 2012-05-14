
(function () {
    EditorToolBar = {
        create:function () {

            var actions = [];

            function errorHandler(response) {
                disableActions(false);
                ErrorConsole.showError(response.responseText);
            }

            function disableActions(dis) {
                Ext.each(actions, function (e) {
                    e.setDisabled(dis);
                    if(dis) {
                        Events.fireEvent("progressStarted");
                    } else {
                        Events.fireEvent("progressEnded");
                    }
                })
            }

            function createPublishBuildAction(text, label, fn) {
                return Ext.create('Ext.Action', {
                    text:text,
                    handler:function (c) {
                        disableActions(true);

                        var botCode = Editor.getContent();

                        if (botCode) {
                            Events.fireEvent("progressUpdate", { message: "Saving sources" });

                            API.updateSourceFiles({
                                jsonData:{
                                    versionLabel: label,
                                    versionPolicy: "ifDifferent",
                                    files: [{
                                        filename: "Bot.scala",
                                        code: botCode
                                    }]
                                },

                                success:function () {
                                    Events.fireEvent("progressUpdate", { message: "Building sources" });
                                    Events.fireEvent("documentSaved");

                                    API.build({
                                        success:function (result) {
                                            try {
                                                if (result.successful) {
                                                    ErrorConsole.hide(true);
                                                    if (fn) {
                                                        fn();
                                                    }
                                                } else {
                                                    ErrorConsole.show(result);
                                                }
                                            } finally {
                                                disableActions(false);
                                            }
                                        },

                                        failure:errorHandler
                                    });
                                },

                                failure:errorHandler
                            });
                        }

                    }
                });
            }

            var buildAction = createPublishBuildAction("Build", "auto-backup before Build");

            var buildAndPubAction = createPublishBuildAction('Publish into Tournament', "auto-backup before Publish into Tournament", function () {
                Events.fireEvent("progressUpdate", { message: "Publishing" });
                API.publish({});
            });

            var sandbox = createPublishBuildAction('Run in Sandbox', "auto-backup before Run in Sandbox", function () {
                Events.fireEvent("progressUpdate", { message: "Creating new sandbox" });
                API.createSandbox({
                    jsonData:{
                        config: {
                            "-x":"50",
                            "-y":"50",
                            "-perimeter":"open",
                            "-walls":"20",
                            "-snorgs":"20",
                            "-fluppets":"20",
                            "-toxifera":"20",
                            "-zugars":"20"
                        }
                    },
                    success:function () {
                        Debugger.show();
                    }
                });

            });

            var signOut = Ext.create('Ext.Action', {
                text: "Sign Out",
                handler:function (c) {
                    disableActions(true);
                    API.logout();
                    window.location = "/";
                }
            });

            var saveAction = Ext.create('Ext.Action', {
                text: "Save...",

                saveHandler: function(label) {
                    /* var self = this; */

                    disableActions(true);
                    var botCode = Editor.getContent();
                    if (botCode) {
                        /* var backupLabel = "Auto-backup before Save of '" + label + "'"; */

                        API.createVersion({
                            jsonData:{
                                label: label,
                                files: [
                                    { filename: "Bot.scala", code: botCode }
                                ]
                            },
                            success:function () {
                                disableActions(false);
                                Events.fireEvent("documentSaved");
                            },
                            failure: errorHandler
                        });

                        /*
                                                API.updateSourceFiles({
                                                    // Create a version of the previous content - if different.
                                                    jsonData:{
                                                        versionLabel: backupLabel,
                                                        versionPolicy: "ifDifferent",
                                                        files: [ { filename: "Bot.scala", code: botCode} ] },
                                                    success:function () {
                                                        disableActions(false);
                                                        Events.fireEvent("documentSaved");

                                                        self.saveVersion(label, botCode);
                                                    },

                                                    failure:errorHandler
                                                });
                        */
                    }
                },

/*
                saveVersion: function(label, botCode) {
                    API.createVersion({
                        jsonData:{
                            label: label,
                            files: [
                                { filename: "Bot.scala", code: botCode }
                            ]
                        },
                        failure: errorHandler
                    });
                },
*/

                handler:function (c) {
                    Ext.MessageBox.prompt('Save...', 'Please enter label name:', function(btn, label) {
                        if(btn = "ok") {
                            c.saveHandler(label);
                        } // else - user canceled operation.
                    });


                }
            });

            var revertAction = Ext.create('Ext.Action', {
                text:"Revert...",
                handler:function (c) {

                    var el = c.getEl();
                    var xy = el.getXY();
                    var h = el.getHeight();
                    var menuPosY = xy[1] + h;

                    var pageDim = Ext.getBody().getViewSize();
                    var maxHeight = pageDim.height - menuPosY - 20;

                    //maxHeight = maxHeight > 600 ? 600 : maxHeight;

                    var grid = VersionGrid.create(maxHeight);

                    grid.loadVersions(function (versions) {
                        var p = Ext.create("Ext.menu.Menu", {
                            items:[ grid ],
                            listeners: {

                            }
                        });

                        grid.on({
                            itemclick:function () {
                                p.destroy();
                            }
                        });

                        p.showAt(xy[0], menuPosY);
                    });
                }
            });

            var cloneAction = Ext.create('Ext.Action', {
                text: "Clone",
                handler:function (c) {
                    var url = window.location.protocol + "//" + getUserName() + "@" + window.location.host + '/git/' + getUserName();
                    Ext.MessageBox.alert('Clone', "To clone your bot locally, execute this command: <br/>" +
                        "git clone " + url
                    )
                }
            });

            actions.push(saveAction);
            actions.push(cloneAction);
            actions.push(buildAction);
            actions.push(buildAndPubAction);
            actions.push(sandbox);
            actions.push(revertAction);

            var spinner = {
                xtype:"panel",
                width:22,
                height:22,
                border:0,
                bodyCls: "x-toolbar x-toolbar-default",
                items:[
                    Ext.create('Ext.Img', {
                        id: "loadingImage",
                        src:"/ext-4.0.7/resources/themes/images/gray/grid/nowait.gif",

                        stop: function() {
                            this.setSrc("/ext-4.0.7/resources/themes/images/gray/grid/nowait.gif")
                        },

                        start: function() {
                            this.setSrc("/ext-4.0.7/resources/themes/images/gray/grid/loading.gif");
                        },

                        listeners: {
                            afterrender: function() {
                                Events.on("progressStarted", this.start, this);
                                Events.on("progressEnded", this.stop, this);
                            }
                        }
                    })
                ]
            };


            return [ saveAction, revertAction, /* cloneAction, */ "-", buildAction, sandbox, buildAndPubAction, "->", spinner, signOut]
        }
    };

})();