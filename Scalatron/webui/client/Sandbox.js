
Sandbox = {
    setState: function(state) {
        var entity;
        if(this.entity) {
            entity = this.findEntityById(state.entities, this.entity.id);
        }

        // Master is always a good Fallback
        if(!entity) {
            entity = this.findMaster(state.entities);
        }

        if(!entity) {
            throw new Error("Cannot find any entity");
        }
        this.state = state;

        Events.fireEvent("sandboxStateChanged", state);
        this.setSelectedEntityById(entity.id);
    },

    getState: function(state) {
        return this.state;
    },

    setSelectedEntityById: function(id) {
        var e = this.findEntityById(this.state.entities, id);
        if(e) {
            this.entity = e;
            Events.fireEvent("entitySelectionChanged", e);
            return true;
        }
        return false;
    },

    getSelectedEntityView: function() {
        if(this.entity && this.entity && this.entity.input && this.entity.input.params.view) {
            return this.entity.input.params.view;
        }
        return null;
    },

    getSelectedEntity: function() {
        return this.entity;
    },

    findEntityById:function (entities, id) {
        for (var idx in entities) {
            if (entities[idx].id == id) {
                return entities[idx];
            }
        }
        return null;
    },

    findMaster:function (entities) {
        for (var idx in entities) {
            if (entities[idx].master) {
                return entities[idx];
            }
        }
        return null;
    }
};
