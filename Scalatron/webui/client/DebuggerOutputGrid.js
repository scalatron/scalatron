DebuggerOutputGrid = {

    toDataSet:function (params) {

        var dataset = [];

        if(params) {
            Ext.each(params, function(e) {
                if(e.opcode != "Log" && e.opcode != "Nop") {
                    dataset.push({
                        key: e.opcode,
                        value: this.toParamString(e)
                    })
                }
            }, this);
        }

        return dataset;
    },

    toParamString:function (obj) {
        var opcode = obj.opcode;
        var params = obj.params;

        var res = "";
        for (var i in params) {
            if (params.hasOwnProperty(i))
                var val = params[i];
            if (i == "view") {
                val = "<view>"
            }
            res += i + "=" + val + ",";
        }
        res = res.substr(0, res.length - 1);
        return res;
    },


    create:function () {
        var store = Ext.create('Ext.data.Store', {
            fields:[ 'key', 'value'],
            data:{}
        });

        return Ext.create('Ext.grid.Panel', {
            title: "<b>Output</b> from control function",
            flex: 1,
            //margin:3,
            store:store,
            hideHeaders:true,
            columns:[
                //{ header:'--', dataIndex:'severity', width:20 },
                { header:'Key', dataIndex:'key', width: 50 },
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
                    this.store.loadRawData(DebuggerOutputGrid.toDataSet(state.output));
                }
            }
        });
    }
};