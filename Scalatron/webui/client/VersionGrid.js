(function () {

    function createStore() {
        return Ext.create('Ext.data.Store', {
            fields:[ 'date', 'label', 'id', 'url'],
            data:{},
            // Note: See columns: date is handled as int
            sorters:[{ property:'date', direction:'DESC' }]
        });
    }

    VersionGrid = {
        create:function (maxHeight) {
            return Ext.create("Ext.grid.Panel", {
                store:createStore(),
                width: "400",

                // Has no effect!
                // maxHeight: maxHeight,

                sortableColumns: false,

                // Workaround: See afterrender below.
                height: maxHeight,

                columns:[
                    { header:'ID', dataIndex:'id', hideable: false, width:40},
                    { header:'Date', dataIndex:'date', width:120, hideable: false, type:'int',
                        renderer:function (value) {
                            if(value) {
                                return Ext.Date.format(new Date(parseInt(value)), "Y-m-d H:i:s");
                            }
                            return "";
                        }
                    },
                    { header:'Label', dataIndex:'label', hideable: false, flex:1,
                        renderer: function(value) {
                            // The string might contain html special chars.
                            // Prevent that rendering get screwed up.
                            return Ext.util.Format.htmlEncode(value);
                        }
                    }
                ],

                listeners:{
                    itemclick:function (grid, record) {
                        var data = record.data;

                        // It can be null in case no versions are available - fake message.
                        if(data.url) {
                            Ext.Ajax.request({
                                url:data.url,
                                method:"GET",
                                headers:{
                                    "Accept":'application/json',
                                    "Content-Type":'application/json'
                                },
                                success:function (r) {
                                    if (r.responseText) {
                                        var result = Ext.JSON.decode(r.responseText);

                                        // TODO: Why does a version deliver a list of files?
                                        var newCode = result.files[0].code;
                                        Editor.setContent(newCode);
                                    }
                                },
                                failure:function (r) {
                                    ErrorConsole.showError(r.responseText);
                                }
                            });
                        }
                    },

                    /**
                     * Hack: Adapt the height after rendering when we know how big the actual content is.
                     * @param grid this grid
                     */
                    afterrender: function(grid) {
                        // Must be delayed a bit because child components are might be not rendered.
                        setTimeout(function() {
                            var h1 = grid.getEl().down(".x-grid-table").getHeight();
                            h1 = h1 + grid.getEl().down(".x-grid-header-ct").getHeight() + 3 /* Magic Number */;

                            var h2 = grid.getHeight();

                            if(h1 < h2) {
                                grid.setHeight(h1);
                            }
                        }, 10);
                    }
                },

                loadVersions:function (fn) {
                    var store = this.store;

                    API.enumVersions({
                        success:function (e) {
                            if(e.versions.length == 0) {
                                e.versions.push({
                                    label: "No versions available"
                                })
                            }

                            store.loadRawData(e.versions);
                            fn(e.versions);
                        },
                        failure:function (r) {
                            ErrorConsole.showError(r.responseText);
                        }
                    });
                }
            })
        }
    }

})();