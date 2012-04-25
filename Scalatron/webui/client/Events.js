Ext.define('Events', {
    singleton: true,
    extend: 'Ext.util.Observable',
    constructor: function(config){
        this.addEvents({
            /**
             * Debugger: Will be fired when bot view playback has been stated/stopped.
             */
            "playback": true,

            /**
             * Sandbox: Will be fired after entity selection has been changed.
             */
            "entitySelectionChanged": true,
            /**
             * Sandbox: Will be fired when sandbox state has been changed (before selection has been changed).
             */
            "sandboxStateChanged": true,

            /**
             * Editor: will be fired when editor documented has been changed.
             */
            "documentChanged": true,

            /**
             * Will be fired when document has been saved.
             */
            "documentSaved": true,

            "progressStarted": true,
            "progressUpdate": true,
            "progressEnded": true
        });
    }
});

// Debug output:
(function() {
    var scope = {};
    Events.on({
        playback: function(e) {
            scope.running = e.running;
            console.log("Playback:", e)
        },
        entitySelectionChanged: function(e) {
            if(!scope.running) {
                console.log("entitySelectionChanged:", e)
            }
        },
        sandboxStateChanged: function(e) {
            if(!scope.running) {
                console.log("sandboxStateChanged:", e)
            }
        },
        documentChanged: function(e) {
            console.log("documentChanged:", e)
        },
        documentSaved: function(e) {
            console.log("documentSaved:", e)
        }
    })
})();