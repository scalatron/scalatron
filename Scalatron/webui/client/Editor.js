(function() {
    EditorModel = {
        // Default is false because initial content is from server.
        contentModified: false
    };

    Events.on({
        documentChanged: function() {
            EditorModel.contentModified = true;
        },
        documentSaved: function() {
            EditorModel.contentModified = false;
        }
    });


    Editor = {
        init: function(id) {
            this.id = id;
            this.aceEditor = ace.edit(id);
            this.aceEditor.setTheme("ace/theme/textmate");
            this.aceEditor.setHighlightActiveLine(true);
            var LanguageMode = require("ace/mode/scala").Mode;
            this.aceEditor.getSession().setMode(new LanguageMode());

            this.aceEditor.getSession().on('change', function() {
                Events.fireEvent("documentChanged");
            });
        },

        resize: function() {
            if(this.aceEditor) {
                this.aceEditor.resize()
            }
        },

        getContent: function() {
            var editor = this.aceEditor;
            if(editor) {
                return editor.getSession().getValue();
            }
        },

        setContent: function(content) {
            var editor = this.aceEditor;
            if(editor) {
                if(EditorModel.contentModified) {
                    Ext.MessageBox.confirm(
                        'Unsaved changes',
                        'Current Bot source is unsaved. Do you want to overwrite it?',
                        function(btn) {
                            if(btn=="yes") {
                                editor.getSession().setValue(content);
                                // loaded from some resource, we assume it is revertible.
                                EditorModel.contentModified = false;
                            }
                        }
                    );
                } else {
                    editor.getSession().setValue(content);
                    EditorModel.contentModified = false;
                }
            }
        },

        highlightLine: function(line) {
            if(line > -1) {
                var editor = this.aceEditor;
                editor.gotoLine(line);
            }
        }
    };
})();