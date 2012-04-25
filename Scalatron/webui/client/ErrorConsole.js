(function () {
    function getSideBar() {
        return Ext.getCmp("errorSideBar");
    }

    ErrorConsole = {
        create:function (content) {
            var store = Ext.create('Ext.data.Store', {
                // TODO: Need a nice icon.
                fields:['severity', 'line', 'message'],
                data:{'items':content.messages },
                proxy:{
                    type:'memory',
                    reader:{
                        type:'json',
                        root:'items'
                    }
                }
            });

            var table = Ext.create('Ext.grid.Panel', {
                store:store,
                hideHeaders:false,
                listeners:{
                    itemclick:function (view, record) {
                        var line = record.data.line;
                        Editor.highlightLine(line);
                    }
                },
                columns:[
                    //{ header:'--', dataIndex:'severity', width:20 },
                    { header:'Line', dataIndex:'line', width:50 },
                    { header:'Console',
                        dataIndex:'message',
                        flex:'1',
                        tdCls:'wrap',
                        renderer:function (value) {
                            // Add line breaks for each newline.
                            return value.replace("\n", "<br>");
                        }
                    }
                ]
            });

/*
            var message = "Build failed, " + (content.errorCount || 0) + " errors, " + (content.warningCount || 0);
            if(content.messages.length === 0) {
                var message = "Build successful, 0 errors, 0 warnings";
            }
*/


            return Ext.create('Ext.Panel', {
                layout:'fit',
                /*
                 headerCfg: {
                 tag: 'div',
                 cls: 'x-panel-header',
                 children: [
                 { tag: 'div', cls: 'panel_header_main', 'html': 'Shopping Cart' },
                 { tag: 'div', cls: 'panel_header_icon1', 'html': '<img src="images/icon_plus.png" />' },
                 { tag: 'div', cls: 'panel_header_extra', 'html': 'Order Number: 2837428347' }
                 ]
                 },
                 */
                /*
                tbar:[

                    {
                        html: message
                    },
                    "->",
                    Ext.create('Ext.ProgressBar', {
                        progressUpdate: function(e) {
                            this.updateText(e.message)
                        },

                        progressEnded: function() {
                            this.reset();
                            this.updateText("Done")
                        },

                        progressStarted: function(e) {
                            this.wait({
                                duration: 1000 * 20, //1000 * 20,

                                //increment: 15,
                                //interval: 500, //bar will move fast!

                                text:'Loading...',
                                scope:this,
                                fn:function () {
                                    this.updateText('Done');
                                }
                            });
                        },

                        listeners: {
                            afterrender: function() {
                                Events.on("progressStarted", this.progressStarted, this);
                                Events.on("progressUpdate", this.progressUpdate, this);
                                Events.on("progressEnded", this.progressEnded, this);
                            },
                            beforedestroy: function() {
                                Events.un("progressStarted", this.progressStarted, this);
                                Events.on("progressUpdate", this.progressUpdate, this);
                                Events.on("progressEnded", this.progressEnded, this);
                            }
                        },
                        width: 300
                    })
                ],
*/
                items:[ table ]
            });
        },

        showError: function(text) {
            this.show({
                messages:[
                    { line:0, message: text, severity:0 }
                ],
                errorCount:1,
                warningCount:0
            });
        },

        hide:function (clearMessage) {
            var sideBar = getSideBar();
            sideBar.collapse();

            if(clearMessage) {
                sideBar.removeAll();
                sideBar.add(this.create({ messages: [] }));
            }

            /*
                        sideBar.setHeight(28);
                        sideBar.doLayout();
             */

            // TODO: Hack to resize tutorial correctly. -- problem: doesn't help :-(
            // Ext.getCmp('tutorial_outer').doLayout();
        },

        show:function (content) {
            var sideBar = getSideBar();
            sideBar.expand();
            sideBar.removeAll();
            sideBar.add(this.create(content));
            sideBar.expand();
            sideBar.doLayout();

            // TODO: Hack to resize tutorial correctly. -- problem: doesn't help :-(
            // Ext.getCmp('tutorial_outer').doLayout();
        }
    }

})();