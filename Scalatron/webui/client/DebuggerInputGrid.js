DebuggerInputGrid = {

    toDataSet:function (params) {

        var dataset = [];

        if(params) {
            for (var i in params) {
                if (params.hasOwnProperty(i)) {
                    var val = params[i];
                    if (i != "view" && i != "name") {
                        dataset.push({
                            key:i,
                            value:val
                        });
                    }
                }
            }
        }

        return dataset;
    },

    create:function () {
        var store = Ext.create('Ext.data.Store', {
            fields:[ 'key', 'value'],
            data:{}
        });

        return Ext.create('Ext.grid.Panel', {
            title: "<b>Input</b> to control function",
            flex: 1,
            //margin:3,
            store:store,
            hideHeaders:true,
            columns:[
                //{ header:'--', dataIndex:'severity', width:20 },
                { header:'Key', dataIndex:'key', width: 75},
                { header:'Value', dataIndex:'value', flex: 1 }
            ],

            listeners:{
                afterrender:function () {
                    Events.on("entitySelectionChanged", this.updateContent, this);
                },

                destroy:function () {
                    Events.un("entitySelectionChanged", this.updateContent, this);
                }
            },

            updateContent: function(state) {
                if(state.input.params) {
                    this.store.loadRawData(DebuggerInputGrid.toDataSet(state.input.params));
                }
            }
        });
    }
};