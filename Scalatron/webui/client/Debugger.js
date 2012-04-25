(function () {

    Debugger = {

        show:function () {
            DebuggerHelper.getDebuggerSidebar().expand();
            DebuggerHelper.nextStep(1);
        },

        close: function() {
            this.stopPlayback();

            API.destroySandboxes({
                failure:function (response) {
                    DebuggerHelper.showError(response);
                }
            });

            DebuggerHelper.getDebuggerSidebar().collapse();
        },

        startPlayback:function () {
            if (!this.intervalId) {
                this.intervalId = setInterval(function () {
                    DebuggerHelper.nextStep(2)
                }, 100);
                Events.fireEvent("playback", { running:true });
            }
        },

        stopPlayback:function () {
            if (this.intervalId) {
                clearInterval(this.intervalId);
                delete this.intervalId;
                Events.fireEvent("playback", { running:false });
            }
        },

        togglePlayback:function () {
            if (!this.intervalId) {
                this.startPlayback();
            } else {
                this.stopPlayback();
            }
        },

        create:function () {
            return Ext.create('Ext.Panel', {
                layout: {
                    type: 'vbox',
                    align: 'stretch'
                },
                tbar:[
                    {
                        id:"playStop",
                        text:"Run",
                        handler:function () {
                            Debugger.togglePlayback();
                        },

                        handlePlayback: function(e) {
                            this.setText(e.running ? "Stop" : "Run")
                        },

                        listeners: {
                            afterrender:function () {
                                Events.on("playback", this.handlePlayback, this);
                            },

                            destroy:function () {
                                Events.un("playback", this.handlePlayback, this);
                            }
                        }
                    },
                    DebuggerHelper.createStepAction("Step", 1),
                    DebuggerHelper.createStepAction("Step 10", 10),
                    DebuggerHelper.createRestartAction()
                ],

                items:[
                    {
                        xtype:"panel",
                        bodyStyle:{"background-color":"#FFFFFF"},
                        height: 244,
                        //margin:3,

                        contentEl:'botviewContainer',
                        listeners: {
                            afterRender: function() {
                                Ext.get("botviewContainer").show();
                            }
                        },
                        tbar:[
                            'Entities',
                            EntityBox.create()
                        ]
                    },

                    DebuggerInputGrid.create(),
                    DebuggerOutputGrid.create(),

                    DebuggerHelper.createTextPane({
                        title: "Log",
                        id: "debugger_log",
                        updateHandler: function(state) {
                            this.setValue(state.debugOutput);
                        }
                    })
                ]
            });
        }
    };

})();