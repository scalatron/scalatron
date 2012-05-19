Ext.require(['*']);

API.init(function () {
    Ext.onReady(function () {

        Ext.create('Ext.container.Viewport', {
            layout:'border',
            id:'root',

            defaults: {
                collapseMode:"mini",
                collapsible: true,
                split: true,
                hideCollapseTool:false,
                bodyStyle: 'padding:0px',
                layout:'fit'
            },

            items:[
                {
                    title: 'Tutorial',
                    id:"leftSidebar",
                    region:'west',
                    minWidth: 512,
                    collapsed:false,
                    items:[ Tutorial.create() ]
                },
                {
                    title: 'Sandbox - Bot Inspector',
                    id:"rightSidebar",
                    region:'east',
                    minWidth: 224,
                    collapsed:true,
                    items:[ Debugger.create() ]
                },
                {
                    title: 'Build Messages',
                    id:"errorSideBar",
                    height: 200,
                    preventHeader:false,
                    collapsed:true,
                    region:'south',
                    items: [ ErrorConsole.create({ messages: [] }) ]
                },
/*
                {
                    id:"errorSideBar",
                    height: 28,
                    preventHeader:true,
                    collapsed:false,
                    region:'south',
                    collapsible: false,
                    split: false,
                    items: [ ErrorConsole.create({ messages: [] }) ]
                },
*/
                {
                    title: 'Editor',
                    region:'center',
                    contentEl:'editor',
                    hideCollapseTool:true,
                    tbar: EditorToolBar.create(),

                    listeners:{
                        resize:function () {
                            Editor.resize()
                        },

                        afterRender:function () {
                            Ext.get("editor").show();
                            Editor.init("editor");

                            setTimeout(function() {
                                API.loadBotSource({
                                    success: function(e) {
                                        Editor.setContent(e.files[0].code)
                                        EditorModel.contentModified = false
                                    }
                                });
                                // re-validate the layout.
                                Ext.getCmp("root").doLayout();
                            }, 10);

                        }
                    }
                }
            ]
        });
    });
})