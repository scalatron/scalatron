
EntityBox = {
    createStore:function () {
        return Ext.create('Ext.data.Store', {
            // store configs
            autoDestroy: true,
            //storeId: 'myStore',
         //   idIndex: 0,
            fields:['id', 'name'],
            data:[
                {"id":"-none-", "name":"-none-"}
            ]
        });
    },

    create: function() {
        var states = this.createStore();

        return Ext.create('Ext.form.ComboBox', {
            store:states,

            displayField:'name',
            valueField:'id',
            id:'entityBox',

            /** Bugfix: The data model get lost on the first time user opens the box. */
            queryMode: "local",

            editable:false,
            autoSelect: false,
            forceSelection: false,

            handlePlayback:function (e) {
                this.setDisabled(e.running);
            },
            handleBotUpdate: function() {
                if(!this.botUpdateDisabled) {
                    var state = Sandbox.getState();
                    this.store.loadRawData(state.entities);
                    var entity = Sandbox.getSelectedEntity();
                    this.select(this.getStore().getById(entity.id));
                    // console.log("State changed: ", state.entities);
                }
            },

            executeNonUpdate: function(fn) {
                this.botUpdateDisabled = true;
                try {
                    fn()
                } finally {
                    this.botUpdateDisabled = false;
                }
            },

            listeners:{

                afterrender:function () {
                    Events.on("playback", this.handlePlayback, this);
                    Events.on("entitySelectionChanged", this.handleBotUpdate, this);
                },

                destroy:function () {
                    Events.un("playback", this.handlePlayback, this);
                    Events.un("entitySelectionChanged", this.handleBotUpdate, this);
                },
                select: function(box, records, opts) {
                    this.executeNonUpdate(function() {
                        // console.log("Entity Selection changed: " + records[0].raw.id);
                        Sandbox.setSelectedEntityById(records[0].raw.id);
                    });
                }
            }
        });
    }
};